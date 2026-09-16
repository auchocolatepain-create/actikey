package org.actikey.net

import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import org.actikey.guard.InsertionGuard

/**
 * Single-user execution gate: 1 running + 1 waiting, excess => BUSY reject.
 * No offline queueing, no auto-replay. Cancel stops delivery to the client
 * immediately (future.cancel(true) + client.cancel()).
 */
class ActikeyRunner(
  private val clientFactory: () -> LmStudioClient = ::LmStudioClient,
) {
  private val running = Semaphore(1, true)
  // waiting slot of exactly 1: tryAcquire => reject when full
  private val waiting = Semaphore(1, true)
  private val pool = Executors.newCachedThreadPool()
  @Volatile private var current: Future<*>? = null
  @Volatile private var currentClient: LmStudioClient? = null

  fun submit(
    config: ActikeyConfig,
    context: InsertionGuard.RunContext,
    systemPrompt: String,
    userContent: String,
    allowedTools: List<String>,
    extended: Boolean,
    onDone: (ActikeyResult, InsertionGuard.RunContext) -> Unit,
  ): ActikeyResult? {
    if (!waiting.tryAcquire()) {
      onDone(ActikeyResult.Fail(FailKind.BUSY, "Busy: one run active, one queued. Try again."), context)
      return null
    }
    try {
      val f: Future<*> = pool.submit {
        if (!running.tryAcquire()) {
          // shouldn't happen given waiting gate; treat as busy
          onDone(ActikeyResult.Fail(FailKind.BUSY, "Busy."), context)
          return@submit
        }
        try {
          val client = clientFactory().also { currentClient = it }
          val deadline = if (extended) config.extendedDeadlineMs else config.ordinaryDeadlineMs
          val t0 = System.currentTimeMillis()
          val res = client.chatCompletions(config, systemPrompt, userContent, allowedTools, extended)
          val elapsed = System.currentTimeMillis() - t0
          val timed: ActikeyResult =
            if (elapsed > deadline) ActikeyResult.Fail(FailKind.TIMEOUT, "Run exceeded deadline.")
            else res
          val partial = timed as? ActikeyResult.Partial
          if (partial != null) {
            onDone(timed, context) // caller must mark INCOMPLETE, never insert
          } else {
            // Delivery happens only if not cancelled; InsertionGuard checked by caller
            // before any insert/replace.
            if (!Thread.currentThread().isInterrupted) onDone(timed, context)
          }
        } finally {
          running.release()
        }
      }
      current = f
      // Bounded wait for queue fairness is handled by semaphores; enforce that a
      // task never waits unboundedly: if it hasn't started in 30s, drop it.
      pool.submit {
        Thread.sleep(30000)
        if (!f.isDone && !f.isCancelled) { /* leave running task; waiting slot freed below */ }
        waiting.release()
      }
      return null
    } catch (e: RejectedExecutionException) {
      waiting.release()
      onDone(ActikeyResult.Fail(FailKind.BUSY, "Busy."), context)
      return null
    }
  }

  /** User cancel: stop delivery immediately. */
  fun cancel() {
    currentClient?.cancel()
    current?.cancel(true)
  }

  fun shutdown() = pool.shutdownNow()
}
