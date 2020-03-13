package nerd.tuxmobil.fahrplan.congress.commons

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Custom property delegates.
 */
object Delegates {

    /**
     * Returns a property delegate for a read/write property with a non-`null` value that is
     * initialized not during object construction time but at a later time. The internal initial
     * value of the property is `null`. Trying to read the property before the initial value has
     * been assigned results in an exception. A specified callback function is invoked when the
     * property is changed.
     * @param onChange the callback which is invoked after the change of the property is made.
     * The value of the property has already been changed when this callback is invoked.
     *
     * Inspired by [kotlin.properties.Delegates].
     */
    inline fun <T> notNullObservable(

            crossinline onChange: (property: KProperty<*>, oldValue: T?, newValue: T) -> Unit

    ): ReadWriteProperty<Any?, T> =

            object : NotNullObservableProperty<T>() {
                override fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T) =
                        onChange(property, oldValue, newValue)
            }

}

/**
 * Implements the core logic of a property delegate for a read/write property with a non-`null`
 * value that is initialized not during object construction time but at a later time. The
 * internal initial value of the property is `null`. Trying to read the property before the
 * initial value has been assigned results in an exception. A specified callback function is
 * invoked when the property is changed.
 *
 * Inspired by [kotlin.properties.ObservableProperty].
 */
abstract class NotNullObservableProperty<T> : ReadWriteProperty<Any?, T> {

    private var value: T? = null

    /**
     *  The callback which is called before a change to the property value is attempted.
     *  The value of the property hasn't been changed yet, when this callback is invoked.
     *  If the callback returns `true` the value of the property is being set to the
     *  new value, and if the callback returns `false` the new value is discarded and the
     *  property remains its old value.
     */
    protected open fun beforeChange(property: KProperty<*>, oldValue: T?, newValue: T): Boolean = true

    /**
     * The callback which is called after the change of the property is made. The value of
     * the property has already been changed when this callback is invoked.
     */
    protected open fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T) = Unit

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: throw IllegalStateException(
                "Property ${property.name} should be initialized before get."
        )
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val oldValue = this.value
        if (!beforeChange(property, oldValue, value)) {
            return
        }
        this.value = value
        afterChange(property, oldValue, value)
    }

}
