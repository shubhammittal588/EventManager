package nerd.tuxmobil.fahrplan.congress.repositories

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import nerd.tuxmobil.fahrplan.congress.NoLogging
import nerd.tuxmobil.fahrplan.congress.exceptions.ExceptionHandling
import nerd.tuxmobil.fahrplan.congress.utils.getOrAwaitValue
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
class AppRepositoryLiveDataTest {

    @get:Rule
    val taskExecutorRule = InstantTaskExecutorRule()

    private companion object {
        val ACTUAL_PAYLOAD_1 = Payload(1, "Lorem ipsum")
        val ACTUAL_PAYLOAD_2 = Payload(2, "Lorem ipsum")
        val EXPECTED_PAYLOAD_1 = Payload(1, "Lorem ipsum")
        val EXPECTED_PAYLOAD_2 = Payload(2, "Lorem ipsum")
    }

    @Test
    fun `AppRepositoryLiveData loads and emits data`() {
        val liveData = object : AppRepositoryLiveData<Payload>(testableAppRepository, NoLogging, testNetworkScope) {
            override fun getJobName() = "unitTest"
            override fun loadFromAppRepository() = ACTUAL_PAYLOAD_1
        }
        assertThat(liveData.getOrAwaitValue()).isEqualTo(EXPECTED_PAYLOAD_1)
    }

    @Test
    fun `AppRepositoryLiveData loads and emits new data once onLecturesUpdate is invoked`() {
        var shouldEmitFirstPayload = true
        val liveData = object : AppRepositoryLiveData<Payload>(testableAppRepository, NoLogging, testNetworkScope) {
            override fun getJobName() = "unitTest"
            override fun loadFromAppRepository() = if (shouldEmitFirstPayload) ACTUAL_PAYLOAD_1 else ACTUAL_PAYLOAD_2
        }
        val actualPayload1 = liveData.getOrAwaitValue()
        shouldEmitFirstPayload = false
        liveData.onLecturesUpdate()
        val actualPayload2 = liveData.getOrAwaitValue()

        assertThat(actualPayload1).isNotEqualTo(actualPayload2)
        assertThat(actualPayload1).isEqualTo(EXPECTED_PAYLOAD_1)
        assertThat(actualPayload2).isEqualTo(EXPECTED_PAYLOAD_2)
    }

    private data class Payload(
            val id: Int,
            val name: String
    )

    private val testableAppRepository: AppRepository
        get() = with(AppRepository) {
            initialize(
                    context = mock(),
                    logging = mock(),
                    networkScope = mock(),
                    alarmsDatabaseRepository = mock(),
                    highlightsDatabaseRepository = mock(),
                    lecturesDatabaseRepository = mock(),
                    metaDatabaseRepository = mock(),
                    scheduleNetworkRepository = mock(),
                    engelsystemNetworkRepository = mock(),
                    sharedPreferencesRepository = mock()
            )
            return this
        }

    private val testNetworkScope = NetworkScope.of(TestExecutionContext, object : ExceptionHandling {
        override fun onExceptionHandling(context: CoroutineContext, throwable: Throwable) {
            throw throwable
        }
    }, NoLogging)

}
