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

package onion.w4v3xrmknycexlsd.app.hypercampus.review

import android.opengl.Matrix
import onion.w4v3xrmknycexlsd.app.hypercampus.currentDate
import onion.w4v3xrmknycexlsd.app.hypercampus.data.Card
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln

sealed class SrsAlgorithm {
    var fi: Double = 0.9

    abstract suspend fun calculateInterval(card: Card, grade: Float, recall: Boolean): Card

    fun nextDue(interval: Int): Int {
        return currentDate() + interval
    }
}

object SM2: SrsAlgorithm() {
    override suspend fun calculateInterval(card: Card, grade: Float, recall: Boolean): Card {
        val q = grade * 2.5 + if (recall) 2.5 else .0

        val newEf = (card.eFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))).coerceAtLeast(1.3)
        val newInt = if (!recall) 1 else when (card.last_interval) {
            0 -> 1
            1 -> 4
            else -> ceil(card.last_interval*newEf).toInt()
        }

        card.eFactor = newEf.toFloat()
        card.last_interval = newInt
        card.due = nextDue(newInt)

        return card
    }
}

@Suppress("LocalVariableName")
object HC1: SrsAlgorithm() {
    private const val gamma = 0.1f // likelihood variance
    val h: (Float, Float) -> FloatArray = { rho, sigma -> arrayOf(1f,rho,sigma,rho*sigma).toFloatArray() } // feature functions

    operator fun Float.times(m: FloatArray): FloatArray = m.map { this*it }.toFloatArray()
    private infix fun FloatArray.dot(v: FloatArray): Float = this.zip(v).map { (a,b) -> a * b}.fold(0f, Float::plus)
    private infix fun FloatArray.outer(v: FloatArray): FloatArray = Array(4) { i -> Array(4) { j -> this[i]*v[j] } }.flatten().toFloatArray()
    private infix fun FloatArray.plus(m: FloatArray): FloatArray = this.zip(m).map { (a,b) -> a + b }.toFloatArray()
    // because we are so lucky we can use opengl, which only works for 4x4 matrices
    operator fun FloatArray.times(v: FloatArray): FloatArray {
        val result = FloatArray(4)
        Matrix.multiplyMV(result,0,this,0,v,0)
        return result
    }
    operator fun FloatArray.not(): FloatArray {
        val result = FloatArray(16)
        Matrix.invertM(result,0,this,0)
        return result
    }

    override suspend fun calculateInterval(card: Card, grade: Float, recall: Boolean): Card {
        // past values
        val _rho = card.former_retrievability
        val _sigma = card.former_stability

        // estimate actual stability increase
        val rho: Float = grade.coerceIn(0.05f,0.95f)
        val t = card.last_interval + currentDate() - (card.due ?: (currentDate() - 4))
        val sigma = -ln(-ln(rho)/t)
        val dSigma = sigma - _sigma

        // prior
        val theta = card.params.toFloatArray()
        val Psi = card.sigma_params.toFloatArray()

        // infer
        val invPsi = !Psi
        val Psi_ = !(invPsi plus (1/gamma * (h(_rho,_sigma) outer h(_rho,_sigma))))
        val theta_ = Psi_ * ((invPsi * theta) plus (1/gamma * dSigma * h(_rho,_sigma)))

        // predict new stability
        val sigma_ = sigma + (h(rho,sigma) dot theta_)

        // project to interval
        val newInt = ceil(exp(sigma_)*ln(1/(1-fi).coerceAtLeast(0.05))).toInt().coerceAtLeast(1)
        val nextDue = nextDue(newInt)

        // update card
        card.due = nextDue
        card.last_interval = newInt
        card.former_stability = sigma
        card.former_retrievability = rho
        card.params = theta_.toList()
        card.sigma_params = Psi_.toList()

        return card
    }
}
