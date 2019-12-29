package onion.w4v3xrmknycexlsd.app.hypercampus.browse

import android.content.Context
import android.util.TypedValue
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import onion.w4v3xrmknycexlsd.app.hypercampus.R
import onion.w4v3xrmknycexlsd.app.hypercampus.data.Card
import onion.w4v3xrmknycexlsd.app.hypercampus.data.Course
import onion.w4v3xrmknycexlsd.app.hypercampus.data.DeckData
import onion.w4v3xrmknycexlsd.app.hypercampus.data.Lesson
import java.util.*

class DeckDataAdapter(
    private val listener: OnItemClickListener
): RecyclerView.Adapter<DeckDataAdapter.DeckDataViewHolder>() {
    private var deckData = mutableListOf<DeckData>()
    private var dataCopy = mutableListOf<DeckData>()
    private var newCounts = mutableListOf<Int>()
    private var dueCounts = mutableListOf<Int>()

    private val mOnClickListener: View.OnClickListener
    private val mOnLongClickListener: View.OnLongClickListener

    private var selectedViews: MutableList<View> = mutableListOf()

    lateinit var firstView: DeckDataViewHolder

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

    private fun select(v: View){
        selectedViews.add(v)
        v.setBackgroundColor(v.context.getThemeColor(R.attr.colorControlHighlight))
    }

    private fun deselect(v: View){
        selectedViews.remove(v)
        v.setBackgroundColor(v.context.getThemeColor(R.attr.colorSurface))
    }

    @ColorInt
    fun Context.getThemeColor(@AttrRes attribute: Int) = TypedValue().let { theme.resolveAttribute(attribute, it, true); it.data }

    open inner class DeckDataViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val shortLabelView: TextView = itemView.findViewById(R.id.label_short)
        val fullLabelView: TextView = itemView.findViewById(R.id.label_full)
        val reviewButton: TextView? = if (deckData[0] !is Card) itemView.findViewById(
            R.id.review_button
        ) else null

        fun bind(data: DeckData){
            itemView.setOnClickListener(mOnClickListener)
            itemView.setOnLongClickListener(mOnLongClickListener)
            itemView.tag = data
            reviewButton?.setOnClickListener(mOnClickListener)
            reviewButton?.tag = data
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckDataViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val itemView = when(deckData[0]) {
            is Course -> inflater.inflate(R.layout.courses_list_item, parent, false)
            is Lesson -> inflater.inflate(R.layout.courses_list_item, parent, false)
            is Card -> inflater.inflate(R.layout.words_list_item, parent, false)
        }
        return DeckDataViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DeckDataViewHolder, position: Int) {
        when(val current = deckData[position]){
            is Course -> {
                holder.shortLabelView.text = current.symbol
                holder.fullLabelView.text = current.name
                holder.reviewButton?.text = holder.reviewButton?.context?.getString(
                    R.string.due_new,dueCounts.getOrElse(dataCopy.indexOf(current)) {0},
               newCounts.getOrElse(dataCopy.indexOf(current)) {0})
            }
            is Lesson -> {
                holder.shortLabelView.text = current.symbol
                holder.fullLabelView.text = current.name
                holder.reviewButton?.text = holder.reviewButton?.context?.getString(
                    R.string.due,dueCounts.getOrElse(dataCopy.indexOf(current)) {0})
            }
            is Card -> {
                holder.shortLabelView.text = current.question
                holder.fullLabelView.text = current.answer
            }
        }
        holder.bind(deckData[position])

        if (position == 0) {
            firstView = holder
            (listener as DeckDataListFragment).intro()
        }
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

    fun deselectAll() {
        for (v in selectedViews) v.setBackgroundColor(v.context.getThemeColor(R.attr.colorSurface))
        selectedViews.clear()
    }

    interface OnItemClickListener {
        fun onItemClick(item: DeckData)
        fun onItemLongClick(item: DeckData)
        fun onButtonClick(item: DeckData)
    }

    override fun getItemCount() = deckData.size

    fun filter(text: String) {
        deckData.clear()
        if(text.isEmpty()){
            deckData.addAll(dataCopy)
        } else {
            for (item in dataCopy){
                when(item) {
                    is Course ->
                        if(item.symbol.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault())) || item.name.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault()))){
                            deckData.add(item)
                        }
                    is Lesson ->
                        if(item.symbol.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault())) || item.name.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault()))){
                            deckData.add(item)
                        }
                    is Card ->
                        if(item.question.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault())) || item.answer.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault()))){
                            deckData.add(item)
                        }
                }
            }
        }
        notifyDataSetChanged()
    }
}