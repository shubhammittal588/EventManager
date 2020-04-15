package nerd.tuxmobil.fahrplan.congress.repositories

import info.metadude.android.eventfahrplan.commons.logging.Logging
import kotlinx.coroutines.*
import nerd.tuxmobil.fahrplan.congress.BuildConfig
import nerd.tuxmobil.fahrplan.congress.exceptions.ExceptionHandling

class DatabaseScope private constructor(

        private val executionContext: ExecutionContext,
        parentJob: Job,
        exceptionHandler: CoroutineExceptionHandler,
        logging: Logging

) {

    companion object {

        @JvmStatic
        fun of(executionContext: ExecutionContext,
               exceptionHandling: ExceptionHandling,
               logging: Logging = Logging.get()
        ): DatabaseScope {
            val defaultExceptionHandler = CoroutineExceptionHandler(exceptionHandling::onExceptionHandling)
            return DatabaseScope(executionContext, SupervisorJob(), defaultExceptionHandler, logging)
        }

    }

    private val scope = if (BuildConfig.DEBUG) {
        CoroutineScope(executionContext.database +
                parentJob +
                exceptionHandler +
                LoggingContinuationInterceptor(logging))
    } else {
        CoroutineScope(executionContext.database +
                parentJob +
                exceptionHandler)
    }

    fun launchNamed(name: String, block: suspend CoroutineScope.() -> Unit): Job {
        return scope.launch(context = CoroutineName(name), block = block)
    }

    suspend fun <T> withUiContext(block: suspend CoroutineScope.() -> T) = executionContext.withUiContext(block)

}
