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
import kotlin.random.Random

class AlgorithmTest {
    private val MAX_REP = 10
    private val RECALL_PROB = 0.8f
    private val RI = 0.9

    private val testCalendar = Calendar.getInstance()
    private val testAlgo = HC1.also {
        it.ri = RI
        it.calendar = testCalendar
    }
    private val testCard = Card(0,0,0)

    @Test
    fun algorithmLongRunTest() {
        println("Current date: ${currentDate(testCalendar)}")

        for (i in 1..MAX_REP) {
            val recall = Random.nextFloat() < RECALL_PROB
            val grade = -Random.nextFloat()*0.5f+1

            runBlocking {
                testAlgo.updateParams(testCard,grade,recall)
                testAlgo.updateCard(testCard)
            }

            val interval = testCard.due!! - currentDate(testCalendar)
            testCalendar.timeInMillis = (testCard.due!! + 1) * 24L * 60 * 60 * 1000

            println("recall: $recall grade: $grade next interval: $interval")
        }

        println("Final date: ${currentDate(testCalendar)} years passed: ${(currentDate(testCalendar) - currentDate())/365}")
    }
}
