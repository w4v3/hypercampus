@file:JvmName("Constants")
package onion.w4v3xrmknycexlsd.app.hypercampus

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.inputmethod.InputMethodManager
import java.util.*

// algorithms
const val ALG_SM2 = 0
const val ALG_HC1 = 1

// new card learning modes
const val MODE_INFO = 0 // show relevant lesson info file before learning
const val MODE_DROPOUT = 1 // self-paced drop out before review
const val MODE_LEARNT = 2 // treat as if learnt already

// showcase ids
const val COURSE_SHOW = "courses_show"
const val LESSON_SHOW = "lessons_show"
const val CARD_SHOW = "card_show"
const val SRS_SHOW = "srs_show"
val SHOWCASE = arrayOf(COURSE_SHOW, LESSON_SHOW, CARD_SHOW, SRS_SHOW)

// card status, for future features
const val STATUS_ENABLED = 1

// MIME file types
const val FILE_IMAGE = "image/*"
const val FILE_AUDIO = "audio/*"

// private file directories
const val DIR_MEDIA = "media"

// utils

const val NEW_DAY = 3 // start of new day in hours after midnight
fun currentDate(): Int {
    val date = Calendar.getInstance()
    return ((date.timeInMillis / (1000 * 60 * 60) - date.timeZone.rawOffset - NEW_DAY) / 24).toInt()
}

fun hideSoftKeyboard(activity: Activity) {
    val inputMethodManager: InputMethodManager? = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
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
