package nerd.tuxmobil.fahrplan.congress.repositories

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import nerd.tuxmobil.fahrplan.congress.exceptions.ExceptionHandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
class NetworkScopeTest {

    private class TestExecutionContext(

            override val ui: CoroutineDispatcher = Dispatchers.Unconfined,
            override val network: CoroutineDispatcher = Dispatchers.Unconfined,
            override val database: CoroutineDispatcher = Dispatchers.Unconfined

    ) : ExecutionContext

    @Test
    fun `name can be retrieved within exception handler`() {
        val networkScope = NetworkScope.of(TestExecutionContext(), object : ExceptionHandling {
            override fun onExceptionHandling(context: CoroutineContext, throwable: Throwable) {
                assertThat("Alpha").isEqualTo(context[CoroutineName.Key]?.name)
            }
        })
        networkScope.launchNamed("Alpha") {
            throw Exception()
        }
    }

    @Test
    fun `exception is handled`() {
        var isExceptionHandled = false

        val networkScope = NetworkScope.of(TestExecutionContext(), object : ExceptionHandling {
            override fun onExceptionHandling(context: CoroutineContext, throwable: Throwable) {
                isExceptionHandled = true
            }
        })
        networkScope.launchNamed("Test") {
            throw Exception()
        }
        assertThat(isExceptionHandled).isTrue()
    }

}
