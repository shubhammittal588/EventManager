package nerd.tuxmobil.fahrplan.congress.schedule

import info.metadude.android.eventfahrplan.commons.logging.Logging
import nerd.tuxmobil.fahrplan.congress.exceptions.AppExceptionHandler
import nerd.tuxmobil.fahrplan.congress.models.Lecture
import nerd.tuxmobil.fahrplan.congress.repositories.AppExecutionContext
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepository
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepositoryLiveData
import nerd.tuxmobil.fahrplan.congress.repositories.NetworkScope

/**
 * LiveData of lectures for the given [day index][dayIndex] which have not been canceled.
 *
 * See also [AppRepository.loadUncanceledLecturesForDayIndex]
 */
class UncanceledLecturesLiveData @JvmOverloads constructor(

        private val dayIndex: Int,
        private val appRepository: AppRepository = AppRepository,
        private val logging: Logging = Logging.get(),
        scope: NetworkScope = NetworkScope.of(AppExecutionContext, AppExceptionHandler(logging))

) : AppRepositoryLiveData<List<Lecture>>(appRepository, logging, scope) {

    override fun getJobName() = "loadUncanceledLecturesForDayIndex '$dayIndex'"

    override fun loadFromAppRepository() = appRepository.loadUncanceledLecturesForDayIndex(dayIndex)

}
