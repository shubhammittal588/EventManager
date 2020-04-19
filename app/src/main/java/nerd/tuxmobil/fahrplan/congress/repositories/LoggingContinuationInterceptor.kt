package nerd.tuxmobil.fahrplan.congress.repositories

import info.metadude.android.eventfahrplan.commons.logging.Logging
import kotlinx.coroutines.CoroutineName
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * Logs the coroutine name and the current thread via the given [logging].
 */
class LoggingContinuationInterceptor(

        val logging: Logging

) : ContinuationInterceptor {

    override val key = ContinuationInterceptor

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        val name = continuation.context[CoroutineName]?.name ?: continuation.toString()
        logging.d(javaClass.simpleName, "Running Coroutine[$name] @${Thread.currentThread().name}")
        return continuation
    }

}
