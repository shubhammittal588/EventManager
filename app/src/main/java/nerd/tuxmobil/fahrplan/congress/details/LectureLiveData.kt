package nerd.tuxmobil.fahrplan.congress.details

import info.metadude.android.eventfahrplan.commons.logging.Logging
import nerd.tuxmobil.fahrplan.congress.exceptions.AppExceptionHandler
import nerd.tuxmobil.fahrplan.congress.models.Lecture
import nerd.tuxmobil.fahrplan.congress.repositories.AppExecutionContext
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepository
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepositoryLiveData
import nerd.tuxmobil.fahrplan.congress.repositories.NetworkScope

/**
 * LiveData of a lecture identified by its [lectureId].
 *
 * See also [AppRepository.readLectureByLectureId]
 */
class LectureLiveData @JvmOverloads constructor(

        private val lectureId: String,
        private val appRepository: AppRepository = AppRepository,
        private val logging: Logging = Logging.get(),
        scope: NetworkScope = NetworkScope.of(AppExecutionContext, AppExceptionHandler(logging))

) : AppRepositoryLiveData<Lecture>(appRepository, logging, scope) {

    init {
        require(lectureId.isNotEmpty())
    }

    override fun getJobName() = "loadLecturesForAllDays '$lectureId'"

    override fun loadFromAppRepository() = appRepository.readLectureByLectureId(lectureId)

}
