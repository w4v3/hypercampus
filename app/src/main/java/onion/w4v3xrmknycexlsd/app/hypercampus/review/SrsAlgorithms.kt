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

import onion.w4v3xrmknycexlsd.app.hypercampus.currentDate
import onion.w4v3xrmknycexlsd.app.hypercampus.data.Card
import java.util.*
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

sealed class SrsAlgorithm(var ri: Double = 0.9, var calendar: Calendar = Calendar.getInstance()) {
    private val randomDisperse = 0.15 // std
    protected var newInterval = 4
    protected var isLapse = false

    // calculateInterval should update card values only relevant to this algorithm only
    abstract suspend fun updateParams(card: Card, grade: Float, recall: Boolean)

    // this is reserved for the current scheduling algorithm
    fun updateCard(card: Card): Card {
        card.had_lapsed = isLapse
        card.before_last_interval = currentInterval(card)
        card.last_interval = newInterval
        card.due = nextDue(randomDisperse(card.last_interval))
        return card
    }

    protected fun currentInterval(card: Card) =
        (card.last_interval + currentDate(calendar) - (card.due ?: (currentDate(calendar) - 3)))
            .coerceAtLeast(1)

    private fun nextDue(interval: Int): Int =
        currentDate(calendar) + interval

    private fun randomDisperse(oldInterval: Int): Int =
        oldInterval + ceil(
            (newInterval - oldInterval)
                    * (Random().nextGaussian() * randomDisperse).coerceIn(-0.5, 0.5)
        ).toInt()
}

object SM2 : SrsAlgorithm() {
    override suspend fun updateParams(card: Card, grade: Float, recall: Boolean) {
        val q = grade * 2.5 + if (recall) 2.5 else .0

        val newEf = (card.eFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))).coerceAtLeast(1.3)
        newInterval = if (!recall) 1 else when (card.last_interval) {
            0 -> 1
            1 -> 4
            else -> ceil(card.last_interval * newEf).toInt()
        }

        card.eFactor = newEf.toFloat()
        isLapse = recall
    }
}

@Suppress("LocalVariableName")
object HC1 : SrsAlgorithm() {
    private val h: (Float, Int) -> Float =
        { sigma, t -> exp(-exp(-sigma) * t).coerceIn(0.01f, 0.99f) } // forgetting curve
    private val f: (Float, Int, FloatArray) -> Float =
        { sigma, t, theta -> (theta dot xi(sigma, t)).coerceIn(0f, 2f) } // stability increase
    private val xi: (Float, Int) -> FloatArray =
        { sigma, t -> arrayOf(1f, sigma, h(sigma, t)).toFloatArray() } // feature functions
    private val h_: (Float, Int) -> Float =
        { sigma, t -> t * exp(-sigma - t * exp(-sigma)) } // derivatives for extended Kalman filter
    private val f_: (Float, Int, FloatArray) -> Float =
        { sigma, t, theta -> 1 + (theta dot xi_(sigma, t)) }
    private val xi_: (Float, Int) -> FloatArray =
        { sigma, t -> arrayOf(0f, 1f, h_(sigma, t)).toFloatArray() } // feature functions

    private const val omega_f = 0.68f // model uncertainty
    private const val omega_g = 0.04f // measurement variance

    private const val kalman_passes = 10

    override suspend fun updateParams(card: Card, grade: Float, recall: Boolean) {
        // dsigma = a*(sigma_max - sigma) + b*(1-rho)
        // = a*sigma_max+b -a*sigma -b*rho
        val alpha = card.params[0]
        val beta = if (!card.had_lapsed) card.params[1] else card.params[2]
        val sigma_max = card.params[3]
        var theta = floatArrayOf(alpha * sigma_max + beta, -alpha, -beta)
        var Psi_theta = card.sigma_params.toFloatArray()

        // predicting stability increase from last review
        val _sigma = card.former_stability
        val _Psi_sigma = card.kalman_psi
        val t = card.before_last_interval
        val T = currentInterval(card)
        var sigma = _sigma + f(_sigma, t, theta)
        var Psi_sigma = _Psi_sigma * f_(_sigma, t, theta).pow(2) + omega_f

        // extended Kalman filter
        repeat(kalman_passes) {
            val drho: Float = grade.coerceIn(0.01f, 0.99f) - h(sigma, T)
            val dPsi: Float = Psi_sigma * h_(sigma, T).pow(2) + omega_g
            val K: Float = Psi_sigma * h_(sigma, T) / dPsi
            sigma += K * drho
            Psi_sigma *= 1 - K * h_(sigma, T)
        }

        // Bayesian regression
        val invPsith = !Psi_theta
        Psi_theta = !(invPsith plus (1 / (omega_f + omega_g) * (xi(_sigma, t) outer xi(_sigma, t))))
        theta =
            Psi_theta * ((invPsith * theta) plus (1 / (omega_f + omega_g) * (sigma - _sigma) * xi(
                _sigma,
                t
            )))

        // predict new stability
        val theta_ = if (recall == card.had_lapsed) theta else
            floatArrayOf(theta[0], theta[1], if (recall) -card.params[2] else -card.params[1])
        val sigma_ = (sigma + (theta_ dot xi(sigma, T))).coerceAtLeast(sigma).coerceIn(2f, 10f)

        // project to interval
        newInterval = ceil(-ln(ri.coerceAtLeast(0.05)) * exp(sigma_)).toInt().coerceAtLeast(1)
        isLapse = recall

        // update card
        card.former_stability = sigma
        card.sigma_params = Psi_theta.toList()
        card.kalman_psi = Psi_sigma
        val alpha_ = -theta[1]
        val beta_ = -theta[2]
        val sigma_max_ = (theta[0] - beta_) / if (alpha_ == 0f) Float.MIN_VALUE else alpha_
        card.params = listOf(
            alpha_,
            if (!card.had_lapsed) beta_ else card.params[1],
            if (card.had_lapsed) beta_ else card.params[2],
            sigma_max_
        )
    }

    // matrix operations
    operator fun Float.times(m: FloatArray): FloatArray = m.map { this * it }.toFloatArray()
    private infix fun FloatArray.dot(v: FloatArray): Float =
        this.zip(v).map { (a, b) -> a * b }.fold(0f, Float::plus)

    private infix fun FloatArray.outer(v: FloatArray): FloatArray =
        Array(3) { i -> Array(3) { j -> this[i] * v[j] } }.flatten().toFloatArray()

    private infix fun FloatArray.plus(m: FloatArray): FloatArray =
        this.zip(m).map { (a, b) -> a + b }.toFloatArray()

    operator fun FloatArray.times(v: FloatArray): FloatArray {
        val result = FloatArray(3)
        result[0] = this.slice(0..2).toFloatArray() dot v
        result[1] = this.slice(3..5).toFloatArray() dot v
        result[2] = this.slice(6..8).toFloatArray() dot v
        return result
    }

    operator fun FloatArray.not(): FloatArray {
        val a = this[0]
        val b = this[1]
        val c = this[2]
        val d = this[3]
        val e = this[4]
        val f = this[5]
        val g = this[6]
        val h = this[7]
        val i = this[8]

        val A = e * i - f * h
        val B = d * i - f * g
        val C = d * h - e * g
        val D = b * i - c * h
        val E = a * i - c * g
        val F = a * h - b * g
        val G = b * f - c * e
        val H = a * f - c * d
        val I = a * e - b * d

        val det = (a * A - b * B + c * C)

        return 1 / det * floatArrayOf(
            A, -D, G,
            -B, E, -H,
            C, -F, I
        )
    }
}
