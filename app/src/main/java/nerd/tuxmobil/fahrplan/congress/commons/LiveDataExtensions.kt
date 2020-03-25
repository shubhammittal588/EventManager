@file:JvmName("LiveDataExtensions")

package nerd.tuxmobil.fahrplan.congress.commons

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer

/**
 * Adds the given [observer] to the observers list within the lifespan of the given [lifecycleOwner].
 * When the payload of [Observer.onChanged] is `null` then a [NullPointerException] is thrown.
 *
 * See also: [LiveData.observe]
 */
fun <T> LiveData<T>.observeNonNullOrThrow(lifecycleOwner: LifecycleOwner, observer: (T) -> Unit) =
        observe(lifecycleOwner, Observer {
            if (it == null) throw NullPointerException("Observed LiveData emitted null value.")
            observer(it)
        })
