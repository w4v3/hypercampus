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

sealed class SrsAlgorithm(var fi: Double = 0.1, var calendar: Calendar = Calendar.getInstance()) {
    abstract suspend fun calculateInterval(card: Card, grade: Float, recall: Boolean): Card

    fun nextDue(interval: Int): Int {
        return currentDate(calendar) + interval
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

    override suspend fun calculateInterval(card: Card, grade: Float, recall: Boolean): Card {
        // past values
        val _rho = card.former_retrievability
        val _sigma = card.former_stability

        // estimate actual stability increase
        val rho: Float = grade.coerceIn(0.05f,0.95f)
        val t = card.last_interval + currentDate(calendar) - (card.due ?: (currentDate(calendar) - 4))
        val sigma = -ln(-ln(rho)/t)
        val dSigma = sigma - _sigma

        // prior
        val theta = card.params.toFloatArray()
        val Psi = card.sigma_params.toFloatArray()

        // infer if not a new card, otherwise take default values
        val (Psi_,theta_) = if (card.due != null) {
            val invPsi = !Psi
            val newPsi = !(invPsi plus (1/gamma * (h(_rho,_sigma) outer h(_rho,_sigma))))
            val newTheta = newPsi * ((invPsi * theta) plus (1/gamma * dSigma * h(_rho,_sigma)))
            (newPsi to newTheta)
        } else {
            (Psi to theta)
        }

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

    // matrix operations
    operator fun Float.times(m: FloatArray): FloatArray = m.map { this*it }.toFloatArray()
    private infix fun FloatArray.dot(v: FloatArray): Float = this.zip(v).map { (a,b) -> a * b}.fold(0f, Float::plus)
    private infix fun FloatArray.outer(v: FloatArray): FloatArray = Array(4) { i -> Array(4) { j -> this[i]*v[j] } }.flatten().toFloatArray()
    private infix fun FloatArray.plus(m: FloatArray): FloatArray = this.zip(m).map { (a,b) -> a + b }.toFloatArray()
    operator fun FloatArray.times(v: FloatArray): FloatArray {
        val result = FloatArray(4)
        result[0] = this.slice(0..3).toFloatArray() dot v
        result[1] = this.slice(4..7).toFloatArray() dot v
        result[2] = this.slice(8..11).toFloatArray() dot v
        result[3] = this.slice(12..15).toFloatArray() dot v
        return result
    }
    operator fun FloatArray.not(): FloatArray {
        val result = FloatArray(16)
        invertM(result,0,this,0)
        return result
    }
    // matrix inversion copied from android.opengl.Matrix, for testability
    fun invertM(
        mInv: FloatArray, mInvOffset: Int, m: FloatArray,
        mOffset: Int
    ): Boolean { // Invert a 4 x 4 matrix using Cramer's Rule
// transpose matrix
        val src0 = m[mOffset + 0]
        val src4 = m[mOffset + 1]
        val src8 = m[mOffset + 2]
        val src12 = m[mOffset + 3]
        val src1 = m[mOffset + 4]
        val src5 = m[mOffset + 5]
        val src9 = m[mOffset + 6]
        val src13 = m[mOffset + 7]
        val src2 = m[mOffset + 8]
        val src6 = m[mOffset + 9]
        val src10 = m[mOffset + 10]
        val src14 = m[mOffset + 11]
        val src3 = m[mOffset + 12]
        val src7 = m[mOffset + 13]
        val src11 = m[mOffset + 14]
        val src15 = m[mOffset + 15]
        // calculate pairs for first 8 elements (cofactors)
        val atmp0 = src10 * src15
        val atmp1 = src11 * src14
        val atmp2 = src9 * src15
        val atmp3 = src11 * src13
        val atmp4 = src9 * src14
        val atmp5 = src10 * src13
        val atmp6 = src8 * src15
        val atmp7 = src11 * src12
        val atmp8 = src8 * src14
        val atmp9 = src10 * src12
        val atmp10 = src8 * src13
        val atmp11 = src9 * src12
        // calculate first 8 elements (cofactors)
        val dst0 = (atmp0 * src5 + atmp3 * src6 + atmp4 * src7
                - (atmp1 * src5 + atmp2 * src6 + atmp5 * src7))
        val dst1 = (atmp1 * src4 + atmp6 * src6 + atmp9 * src7
                - (atmp0 * src4 + atmp7 * src6 + atmp8 * src7))
        val dst2 = (atmp2 * src4 + atmp7 * src5 + atmp10 * src7
                - (atmp3 * src4 + atmp6 * src5 + atmp11 * src7))
        val dst3 = (atmp5 * src4 + atmp8 * src5 + atmp11 * src6
                - (atmp4 * src4 + atmp9 * src5 + atmp10 * src6))
        val dst4 = (atmp1 * src1 + atmp2 * src2 + atmp5 * src3
                - (atmp0 * src1 + atmp3 * src2 + atmp4 * src3))
        val dst5 = (atmp0 * src0 + atmp7 * src2 + atmp8 * src3
                - (atmp1 * src0 + atmp6 * src2 + atmp9 * src3))
        val dst6 = (atmp3 * src0 + atmp6 * src1 + atmp11 * src3
                - (atmp2 * src0 + atmp7 * src1 + atmp10 * src3))
        val dst7 = (atmp4 * src0 + atmp9 * src1 + atmp10 * src2
                - (atmp5 * src0 + atmp8 * src1 + atmp11 * src2))
        // calculate pairs for second 8 elements (cofactors)
        val btmp0 = src2 * src7
        val btmp1 = src3 * src6
        val btmp2 = src1 * src7
        val btmp3 = src3 * src5
        val btmp4 = src1 * src6
        val btmp5 = src2 * src5
        val btmp6 = src0 * src7
        val btmp7 = src3 * src4
        val btmp8 = src0 * src6
        val btmp9 = src2 * src4
        val btmp10 = src0 * src5
        val btmp11 = src1 * src4
        // calculate second 8 elements (cofactors)
        val dst8 = (btmp0 * src13 + btmp3 * src14 + btmp4 * src15
                - (btmp1 * src13 + btmp2 * src14 + btmp5 * src15))
        val dst9 = (btmp1 * src12 + btmp6 * src14 + btmp9 * src15
                - (btmp0 * src12 + btmp7 * src14 + btmp8 * src15))
        val dst10 = (btmp2 * src12 + btmp7 * src13 + btmp10 * src15
                - (btmp3 * src12 + btmp6 * src13 + btmp11 * src15))
        val dst11 = (btmp5 * src12 + btmp8 * src13 + btmp11 * src14
                - (btmp4 * src12 + btmp9 * src13 + btmp10 * src14))
        val dst12 = (btmp2 * src10 + btmp5 * src11 + btmp1 * src9
                - (btmp4 * src11 + btmp0 * src9 + btmp3 * src10))
        val dst13 = (btmp8 * src11 + btmp0 * src8 + btmp7 * src10
                - (btmp6 * src10 + btmp9 * src11 + btmp1 * src8))
        val dst14 = (btmp6 * src9 + btmp11 * src11 + btmp3 * src8
                - (btmp10 * src11 + btmp2 * src8 + btmp7 * src9))
        val dst15 = (btmp10 * src10 + btmp4 * src8 + btmp9 * src9
                - (btmp8 * src9 + btmp11 * src10 + btmp5 * src8))
        // calculate determinant
        val det = src0 * dst0 + src1 * dst1 + src2 * dst2 + src3 * dst3
        if (det == 0.0f) {
            return false
        }
        // calculate matrix inverse
        val invdet = 1.0f / det
        mInv[mInvOffset] = dst0 * invdet
        mInv[1 + mInvOffset] = dst1 * invdet
        mInv[2 + mInvOffset] = dst2 * invdet
        mInv[3 + mInvOffset] = dst3 * invdet
        mInv[4 + mInvOffset] = dst4 * invdet
        mInv[5 + mInvOffset] = dst5 * invdet
        mInv[6 + mInvOffset] = dst6 * invdet
        mInv[7 + mInvOffset] = dst7 * invdet
        mInv[8 + mInvOffset] = dst8 * invdet
        mInv[9 + mInvOffset] = dst9 * invdet
        mInv[10 + mInvOffset] = dst10 * invdet
        mInv[11 + mInvOffset] = dst11 * invdet
        mInv[12 + mInvOffset] = dst12 * invdet
        mInv[13 + mInvOffset] = dst13 * invdet
        mInv[14 + mInvOffset] = dst14 * invdet
        mInv[15 + mInvOffset] = dst15 * invdet
        return true
    }
}
