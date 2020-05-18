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

package onion.w4v3xrmknycexlsd.app.hypercampus.browse

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import onion.w4v3xrmknycexlsd.app.hypercampus.R
import onion.w4v3xrmknycexlsd.app.hypercampus.STATUS_DISABLED
import onion.w4v3xrmknycexlsd.app.hypercampus.currentDate
import onion.w4v3xrmknycexlsd.app.hypercampus.data.Card
import onion.w4v3xrmknycexlsd.app.hypercampus.data.Course
import onion.w4v3xrmknycexlsd.app.hypercampus.data.DeckData
import onion.w4v3xrmknycexlsd.app.hypercampus.data.Lesson
import onion.w4v3xrmknycexlsd.app.hypercampus.getThemeColor
import java.util.*

class DeckDataAdapter(
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<DeckDataAdapter.DeckDataViewHolder>() {
    private var deckData = mutableListOf<DeckData>()
    private var statData = mutableListOf<IntArray>()
    private var dataCopy = mutableListOf<DeckData>()
    private var newCounts = mutableListOf<Int>()
    private var dueCounts = mutableListOf<Int>()

    private val mOnClickListener: View.OnClickListener
    private val mOnLongClickListener: View.OnLongClickListener

    private var selectedViews: MutableList<View> = mutableListOf()

    var showingStats = false

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as DeckData
            if (v is Button) {
                listener.onButtonClick(item)
            } else {
                if (v in selectedViews) {
                    deselect(v)
                } else {
                    select(v)
                }

                listener.onItemClick(item)
            }
        }

        mOnLongClickListener = View.OnLongClickListener { v ->
            val item = v.tag as DeckData

            if (v in selectedViews) {
                deselect(v)
            } else {
                select(v)
            }

            listener.onItemLongClick(item)
            true
        }
    }

    open inner class DeckDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val shortLabelView: TextView = itemView.findViewById(R.id.label_short)
        val fullLabelView: TextView = itemView.findViewById(R.id.label_full)
        val reviewButton: TextView? = if (deckData[0] !is Card) itemView.findViewById(
            R.id.review_button
        ) else null
        val statsView: View = itemView.findViewById(R.id.stats_view)

        fun bind(data: DeckData) {
            itemView.setOnClickListener(mOnClickListener)
            itemView.setOnLongClickListener(mOnLongClickListener)
            itemView.tag = data
            reviewButton?.setOnClickListener(mOnClickListener)
            reviewButton?.tag = data
            statsView.visibility = if (showingStats) View.VISIBLE else View.GONE
            checkDisableColor(data, itemView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckDataViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val itemView = when (deckData[0]) {
            is Course -> inflater.inflate(R.layout.courses_list_item, parent, false)
            is Lesson -> inflater.inflate(R.layout.courses_list_item, parent, false)
            is Card -> inflater.inflate(R.layout.words_list_item, parent, false)
        }
        return DeckDataViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DeckDataViewHolder, position: Int) {
        when (val current = deckData[position]) {
            is Course -> {
                holder.shortLabelView.text = current.symbol
                holder.fullLabelView.text = current.name
                holder.reviewButton?.text = holder.reviewButton?.context?.getString(
                    R.string.due_new, dueCounts.getOrNull(dataCopy.indexOf(current)) ?: "…",
                    newCounts.getOrNull(dataCopy.indexOf(current)) ?: "…"
                )
                if (showingStats) {
                    val stats = statData.getOrNull(position)
                    val totalnum = stats?.getOrNull(0) ?: 0
                    val newnum = stats?.getOrNull(1) ?: 0
                    val disablenum = stats?.getOrNull(2) ?: 0
                    val learntnum = totalnum - newnum - disablenum
                    holder.statsView.findViewById<TextView>(R.id.label_numcards).text = "$totalnum"
                    holder.statsView.findViewById<TextView>(R.id.label_cardprops).text =
                        "$learntnum/$newnum/$disablenum"
                    holder.statsView.findViewById<TextView>(R.id.label_cardpercs).text =
                        "%.1f/%.1f/%.1f".format(
                            learntnum * 100f / totalnum,
                            newnum * 100f / totalnum,
                            disablenum * 100f / totalnum
                        )
                }
            }
            is Lesson -> {
                holder.shortLabelView.text = current.symbol
                holder.fullLabelView.text = current.name
                holder.reviewButton?.text = holder.reviewButton?.context?.getString(
                    R.string.due, dueCounts.getOrNull(dataCopy.indexOf(current)) ?: "…"
                )
                if (showingStats) {
                    val stats = statData.getOrNull(position)
                    val totalnum = stats?.getOrNull(0) ?: 0
                    val newnum = stats?.getOrNull(1) ?: 0
                    val disablenum = stats?.getOrNull(2) ?: 0
                    val learntnum = totalnum - newnum - disablenum
                    holder.statsView.findViewById<TextView>(R.id.label_numcards).text = "$totalnum"
                    holder.statsView.findViewById<TextView>(R.id.label_cardprops).text =
                        "$learntnum/$newnum/$disablenum"
                    holder.statsView.findViewById<TextView>(R.id.label_cardpercs).text =
                        "%.1f/%.1f/%.1f".format(
                            learntnum * 100f / totalnum,
                            newnum * 100f / totalnum,
                            disablenum * 100f / totalnum
                        )
                }
            }
            is Card -> {
                holder.shortLabelView.text = current.question
                holder.fullLabelView.text = current.answer
                if (showingStats) {
                    holder.statsView.findViewById<TextView>(R.id.label_due).text =
                        if (current.due != null) "${current.due!! - currentDate()}d" else "-"
                    holder.statsView.findViewById<TextView>(R.id.label_rhosig).text =
                        "%.1f".format(current.former_stability)
                    holder.statsView.findViewById<TextView>(R.id.label_hcparams).text =
                        "%.2f/%.2f(%.2f)/%.1f".format(
                            current.params[0],
                            current.params[1],
                            current.params[2],
                            current.params[3]
                        )
                    holder.statsView.findViewById<TextView>(R.id.label_smparams).text =
                        "%.1f".format(current.eFactor)
                }
            }
        }
        holder.bind(deckData[position])
    }

    internal fun setData(data: List<DeckData>) {
        this.deckData = data.toMutableList()
        this.dataCopy = data.toMutableList()
        notifyDataSetChanged()
    }

    internal fun setDueCounts(data: List<Int>) {
        this.dueCounts = data.toMutableList()
        notifyDataSetChanged()
    }

    internal fun setNewCounts(data: List<Int>) {
        this.newCounts = data.toMutableList()
        notifyDataSetChanged()
    }

    internal fun toggleStats(data: List<IntArray>? = null) {
        showingStats = !showingStats
        if (data != null) {
            statData.clear()
            statData.addAll(data)
        }
        notifyDataSetChanged()
    }

    private fun select(v: View) {
        selectedViews.add(v)
        v.setBackgroundColor(v.context.getThemeColor(R.attr.colorControlHighlight))
    }

    private fun deselect(v: View) {
        selectedViews.remove(v)
        v.setBackgroundColor(v.context.getThemeColor(R.attr.colorSurface))
        checkDisableColor(v.tag as DeckData, v)
    }

    fun deselectAll() {
        // need to handle one by one to prevent ConcurrentModificationException
        for (v in selectedViews) {
            v.setBackgroundColor(v.context.getThemeColor(R.attr.colorSurface))
            checkDisableColor(v.tag as DeckData, v)
        }
        selectedViews.clear()
    }

    private fun checkDisableColor(data: DeckData, v: View) {
        if (data is Card && data.status == STATUS_DISABLED)
            v.setBackgroundColor(v.context.getThemeColor(R.attr.colorAccent))
    }

    interface OnItemClickListener {
        fun onItemClick(item: DeckData)
        fun onItemLongClick(item: DeckData)
        fun onButtonClick(item: DeckData)
    }

    override fun getItemCount() = deckData.size

    fun filter(text: String) {
        deckData.clear()
        if (text.isEmpty()) {
            deckData.addAll(dataCopy)
        } else {
            for (item in dataCopy) {
                when (item) {
                    is Course ->
                        if (item.symbol.toLowerCase(Locale.getDefault())
                                .contains(text.toLowerCase(Locale.getDefault())) || item.name.toLowerCase(
                                Locale.getDefault()
                            ).contains(text.toLowerCase(Locale.getDefault()))
                        ) {
                            deckData.add(item)
                        }
                    is Lesson ->
                        if (item.symbol.toLowerCase(Locale.getDefault())
                                .contains(text.toLowerCase(Locale.getDefault())) || item.name.toLowerCase(
                                Locale.getDefault()
                            ).contains(text.toLowerCase(Locale.getDefault()))
                        ) {
                            deckData.add(item)
                        }
                    is Card ->
                        if (item.question.toLowerCase(Locale.getDefault())
                                .contains(text.toLowerCase(Locale.getDefault())) || item.answer.toLowerCase(
                                Locale.getDefault()
                            ).contains(text.toLowerCase(Locale.getDefault()))
                        ) {
                            deckData.add(item)
                        }
                }
            }
        }
        notifyDataSetChanged()
    }
}