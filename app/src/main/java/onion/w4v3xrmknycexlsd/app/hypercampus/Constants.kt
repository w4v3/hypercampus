@file:JvmName("Constants")
package onion.w4v3xrmknycexlsd.app.hypercampus

// algorithms
const val ALG_SM2 = 0

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