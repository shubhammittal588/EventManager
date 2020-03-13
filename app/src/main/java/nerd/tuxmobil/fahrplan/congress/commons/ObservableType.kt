package nerd.tuxmobil.fahrplan.congress.commons

import info.metadude.android.eventfahrplan.commons.logging.AlmostNoLogging
import info.metadude.android.eventfahrplan.commons.logging.Logging
import nerd.tuxmobil.fahrplan.congress.commons.Delegates.notNullObservable

class ObservableType<T : Any>(logging: Logging = AlmostNoLogging) {

    private val tag = ObservableType::class.java.simpleName

    var value: T by notNullObservable { _, oldValue, newValue ->
        val className = newValue::class.java.simpleName
        if (oldValue != newValue) {
            logging.d(tag, "$className has changed.")
            logging.d(tag, "old = $oldValue")
            logging.d(tag, "new = $newValue")
            logging.d(tag, "Notifying observers.")
            observers.forEach { it() }
        } else {
            logging.d(tag, "$className has not changed. Skip notifying observers.")
        }
    }

    private val observers = mutableSetOf<() -> Unit>()

    fun addObserver(observer: () -> Unit) {
        observers.add(observer)
    }

    fun deleteObservers() {
        observers.clear()
    }

}
