@file:JvmName("Constants")
package onion.w4v3xrmknycexlsd.app.hypercampus

import android.app.Activity
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

// status
const val STATUS_ENABLED = 1

fun currentDate(): Int {
    val date = Calendar.getInstance(Locale.US)
    return date.get(Calendar.YEAR) * 10000 + (date.get(Calendar.MONTH) + 1) * 100 + date.get(
        Calendar.DAY_OF_MONTH)
}

fun hideSoftKeyboard(activity: Activity) {
    val inputMethodManager: InputMethodManager? = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager?.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
}
