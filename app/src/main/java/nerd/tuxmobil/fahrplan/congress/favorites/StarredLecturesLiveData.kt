package nerd.tuxmobil.fahrplan.congress.favorites

import info.metadude.android.eventfahrplan.commons.logging.Logging
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepositoryLiveData
import nerd.tuxmobil.fahrplan.congress.exceptions.AppExceptionHandler
import nerd.tuxmobil.fahrplan.congress.models.Lecture
import nerd.tuxmobil.fahrplan.congress.repositories.AppExecutionContext
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepository
import nerd.tuxmobil.fahrplan.congress.repositories.NetworkScope

/**
 * LiveData of lectures which have been favored aka. starred but no canceled.
 *
 * See also [AppRepository.loadStarredLectures]
 */
class StarredLecturesLiveData @JvmOverloads constructor(

        private val appRepository: AppRepository = AppRepository,
        private val logging: Logging = Logging.get(),
        scope: NetworkScope = NetworkScope.of(AppExecutionContext, AppExceptionHandler(logging))

) : AppRepositoryLiveData<List<Lecture>>(appRepository, logging, scope) {

    override fun getJobName() = "loadStarredLectures"

    override fun loadFromAppRepository(): List<Lecture> = appRepository.loadStarredLectures()

}
