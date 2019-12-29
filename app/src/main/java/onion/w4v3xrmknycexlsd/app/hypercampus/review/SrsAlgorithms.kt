package onion.w4v3xrmknycexlsd.app.hypercampus.review

import onion.w4v3xrmknycexlsd.app.hypercampus.data.Card
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.ln

sealed class SrsAlgorithm {
    var fi: Double = 0.9

    abstract suspend fun calculateInterval(card: Card, grade: Float): Card

    /*
    fun forgettingCurve(t: Double, S: Double): Double {
        return exp(-t/S)
    }
     */

    fun invertForgetting(R: Double, S: Double): Double {
        return -ln(R) *S
    }

    fun nextDue(interval: Int): Int {
        val currentDate = Calendar.getInstance(Locale.US)
        currentDate.add(Calendar.DATE,interval)
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(currentDate.time).toInt()
    }
}

object SM2: SrsAlgorithm() {
    override suspend fun calculateInterval(card: Card, grade: Float): Card {
        val q = grade * 5

        val newEf = (card.eFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))).let { if (it < 1.3) 1.3 else it }
        val newInt = if (grade < 0.5) 1 else when (card.last_interval) {
            0 -> 1
            1 -> ceil(invertForgetting(fi,40.0)).toInt()
            else -> ceil(card.last_interval*newEf).toInt()
        }

        card.eFactor = newEf.toFloat()
        card.due = nextDue(newInt)

        return card
    }
}

object HC1: SrsAlgorithm() {
    override suspend fun calculateInterval(card: Card, grade: Float): Card {
        return card
    }
}
