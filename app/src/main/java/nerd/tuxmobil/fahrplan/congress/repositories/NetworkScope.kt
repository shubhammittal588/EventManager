package nerd.tuxmobil.fahrplan.congress.repositories

import info.metadude.android.eventfahrplan.commons.logging.Logging
import kotlinx.coroutines.*
import nerd.tuxmobil.fahrplan.congress.BuildConfig
import nerd.tuxmobil.fahrplan.congress.exceptions.ExceptionHandling

class NetworkScope private constructor(

        private val executionContext: ExecutionContext,
        parentJob: Job,
        exceptionHandler: CoroutineExceptionHandler,
        loggingInterceptor: LoggingContinuationInterceptor

) {

    companion object {

        @JvmStatic
        fun of(executionContext: ExecutionContext,
               exceptionHandling: ExceptionHandling,
               logging: Logging = Logging.get(),
               loggingInterceptor: LoggingContinuationInterceptor = LoggingContinuationInterceptor(logging)
        ): NetworkScope {
            val defaultExceptionHandler = CoroutineExceptionHandler(exceptionHandling::onExceptionHandling)
            return NetworkScope(executionContext, SupervisorJob(), defaultExceptionHandler, loggingInterceptor)
        }

    }

    private val scope: CoroutineScope

    init {
        var context = executionContext.network + parentJob + exceptionHandler
        if (BuildConfig.DEBUG) {
            context += loggingInterceptor
        }
        scope = CoroutineScope(context)
    }

    fun launchNamed(name: String, block: suspend CoroutineScope.() -> Unit): Job {
        return scope.launch(context = CoroutineName(name), block = block)
    }

    suspend fun <T> withUiContext(block: suspend CoroutineScope.() -> T) = executionContext.withUiContext(block)

}
