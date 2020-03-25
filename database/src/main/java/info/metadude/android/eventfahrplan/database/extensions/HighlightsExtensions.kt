package info.metadude.android.eventfahrplan.database.extensions

import android.content.ContentValues
import info.metadude.android.eventfahrplan.database.models.Highlight

fun List<Highlight>.toContentValues(): List<ContentValues> = map(Highlight::toContentValues)
