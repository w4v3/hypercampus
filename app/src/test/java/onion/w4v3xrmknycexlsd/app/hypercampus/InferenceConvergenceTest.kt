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

package onion.w4v3xrmknycexlsd.app.hypercampus

import kotlinx.coroutines.runBlocking
import onion.w4v3xrmknycexlsd.app.hypercampus.data.Card
import onion.w4v3xrmknycexlsd.app.hypercampus.review.HC1
import org.junit.Test
import java.util.*
import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random

class InferenceConvergenceTest {
    private val MAX_REP = 40
    private val RECALL_PROB = 0.8f
    private val RI = 0.9

    private val TRUE_PARAMS = arrayOf(0.7f, -0.02f, -0.03f)
    private val TEST_SIGMAS = arrayOf(-1f, -0.5f, 0f, 0.5f, 1f, 1.5f, 2f, 3.6f, 5f, 10f)

    private val testCalendar = Calendar.getInstance()
    private val testAlgo = HC1.also {
        it.ri = RI
        it.calendar = testCalendar
    }
    private val testCard = Card(0, 0, 0)

    @Test
    fun hyperCampusConvergenceTest() {
        println("Current date: ${currentDate(testCalendar)}")

        for (tS in TEST_SIGMAS) {
            var trueSigma = tS
            var error = 0.0
            for (i in 1..MAX_REP) {
                val recall = Random.nextFloat() < RECALL_PROB
                val t = testCard.last_interval + currentDate(testCalendar) - (testCard.due
                    ?: (currentDate(testCalendar) - 4))
                val grade = exp(-t / exp(trueSigma))

                runBlocking {
                    testAlgo.updateParams(testCard, grade, recall)
                    testAlgo.updateCard(testCard)
                }

                // simulate true stability derived from true parameters
                val rho = grade
                trueSigma += (TRUE_PARAMS[0] + TRUE_PARAMS[1] * rho + TRUE_PARAMS[2] * trueSigma)
                error = (error * (i - 1) + abs(rho - testAlgo.ri)) / i
                testCalendar.timeInMillis = (testCard.due!! + 1) * 24L * 60 * 60 * 1000
            }
            println("Average error for startup sigma = $tS: $error")
            assert(error < 0.15)
        }
    }
}
