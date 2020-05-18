/*
 *     Copyright (c) 2019, 2020 by w4v3 <support.w4v3+hypercampus@protonmail.com>
 *
 *     This file is part of HyperCampus.
 *
 *     HyperCampus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     HyperCampus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with HyperCampus.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:JvmName("Constants")

package onion.w4v3xrmknycexlsd.app.hypercampus

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.databinding.BindingAdapter
import com.google.android.material.snackbar.Snackbar
import onion.w4v3xrmknycexlsd.app.hypercampus.data.HyperDataConverter
import java.util.*

// algorithms
const val ALG_SM2 = 0
const val ALG_HC1 = 1

// new card learning modes
const val MODE_LEARNT = -1 // treat as if learnt already
const val MODE_DROPOUT = 0 // self-paced drop out before review
const val MODE_INFO = 1 // show relevant lesson info file before learning
const val MODE_INFO_DROPOUT = 2 // both

// showcase ids
const val COURSE_SHOW = "courses_show"
const val LESSON_SHOW = "lessons_show"
const val CARD_SHOW = "card_show"
const val SRS_SHOW = "srs_show"
val SHOWCASE = arrayOf(COURSE_SHOW, LESSON_SHOW, CARD_SHOW, SRS_SHOW)

// card status
const val STATUS_ENABLED = 1
const val STATUS_DISABLED = 0

// MIME file types
const val FILE_IMAGE = "image/*"
const val FILE_AUDIO = "audio/*"

// Supported locales
val SUPPORTED_LANG = listOf("en", "de")

// custom adapter for card view in srs
@BindingAdapter("card_view_content")
fun LinearLayout.convertToViews(toConvert: String?) {
    this.removeAllViews()
    context.activity()?.let { hyperActivity ->
        toConvert?.let { HyperDataConverter(hyperActivity).convertToViews(it, this) }
    }
}

tailrec fun Context?.activity(): HyperActivity? = when (this) { // sweet sweet kotlin
    is HyperActivity -> this
    else -> (this as? ContextWrapper)?.baseContext?.activity()
}

// utils

const val NEW_DAY_HOURS = 3 // start of new day in hours after midnight
fun currentDate(date: Calendar = Calendar.getInstance()): Int {
    val utcmidnight = date.timeInMillis + TimeZone.getDefault().getOffset(date.timeInMillis)
    return ((utcmidnight / (1000 * 60 * 60) - NEW_DAY_HOURS) / 24).toInt()
}

@ColorInt
fun Context.getThemeColor(@AttrRes attribute: Int) =
    TypedValue().let { theme.resolveAttribute(attribute, it, true); it.data }

fun hideSoftKeyboard(activity: Activity) {
    val inputMethodManager: InputMethodManager? =
        activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager?.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
}

class ApplicationLanguageHelper(base: Context) : ContextThemeWrapper(base, R.style.AppTheme) {
    companion object {
        fun wrap(context: Context, language: String): ContextThemeWrapper {
            var mContext = context
            val config = mContext.resources.configuration
            if (language != "") {
                val locale = Locale(language)
                Locale.setDefault(locale)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    config.setLocale(locale)
                } else {
                    @Suppress("DEPRECATION")
                    config.locale = locale
                }
                config.setLayoutDirection(locale)
                mContext = mContext.createConfigurationContext(config)
            }
            return ApplicationLanguageHelper(mContext)
        }

    }
}

// snacks
fun Activity.goodSnack(message: String) {
    val snack =
        Snackbar.make(this.findViewById(R.id.the_coordinator), message, Snackbar.LENGTH_SHORT)
    snack.view.setBackgroundColor(this.getThemeColor(R.attr.colorSecondary))
    snack.setTextColor(this.getThemeColor(R.attr.colorOnSecondary))
    snack.show()
}

fun Activity.badSnack(message: String) {
    val snack =
        Snackbar.make(this.findViewById(R.id.the_coordinator), message, Snackbar.LENGTH_INDEFINITE)
    snack.view.setBackgroundColor(this.getThemeColor(R.attr.colorError))
    snack.setTextColor(this.getThemeColor(R.attr.colorOnError))
    snack.setAction(getString(R.string.dismiss)) { }
    snack.setActionTextColor(this.getThemeColor(R.attr.colorOnError))
    snack.show()
}