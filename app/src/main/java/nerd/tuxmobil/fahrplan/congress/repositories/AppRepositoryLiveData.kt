package nerd.tuxmobil.fahrplan.congress.repositories

import android.arch.lifecycle.LiveData
import info.metadude.android.eventfahrplan.commons.logging.Logging
import kotlinx.coroutines.Job

/**
 * LiveData of [T] objects which are provided by the [AppRepository][appRepository].
 * Data loading and UI updates are performed as configured in the given [NetworkScope][scope].
 * Data loading is triggered once the LiveData becomes [active][onActive] and
 * every time [OnLecturesUpdateListener.onLecturesUpdate] is invoked by the [AppRepository].
 */
abstract class AppRepositoryLiveData<T>(

        private val appRepository: AppRepository,
        private val logging: Logging,
        private val scope: NetworkScope

) : LiveData<T>(), OnLecturesUpdateListener {

    private val namedJobs = mutableMapOf<String, Job>()

    override fun onActive() {
        super.onActive()
        appRepository.addOnLecturesUpdateListener(this)
        loadAsync()
    }

    override fun onInactive() {
        super.onInactive()
        appRepository.removeOnLecturesUpdateListener(this)
        namedJobs.values.forEach(Job::cancel)
        namedJobs.clear()
    }

    override fun onLecturesUpdate() {
        loadAsync()
    }

    private fun loadAsync() {
        val jobName = getJobName()
        namedJobs[jobName] = scope.launchNamed(jobName) {
            logging.d(javaClass.simpleName, "Loading ($jobName) from repository ...")
            val result = loadFromAppRepository()
            // Add delay(700) here to simulate loading.
            scope.withUiContext {
                logging.d(javaClass.simpleName, "Pushing result from ($jobName) into LiveData ...")
                value = result
            }
        }
    }

    protected abstract fun getJobName(): String

    protected abstract fun loadFromAppRepository(): T

}
