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

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.image.picasso.PicassoImagesPlugin
import io.noties.markwon.recycler.MarkwonAdapter
import io.noties.markwon.recycler.table.TableEntry
import io.noties.markwon.recycler.table.TableEntryPlugin
import io.noties.markwon.urlprocessor.UrlProcessorRelativeToAbsolute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import onion.w4v3xrmknycexlsd.app.hypercampus.*
import onion.w4v3xrmknycexlsd.app.hypercampus.browse.Level
import onion.w4v3xrmknycexlsd.app.hypercampus.data.Card
import onion.w4v3xrmknycexlsd.app.hypercampus.data.HyperDataConverter
import onion.w4v3xrmknycexlsd.app.hypercampus.data.HyperViewModel
import onion.w4v3xrmknycexlsd.app.hypercampus.databinding.FragmentSrsBinding
import org.commonmark.ext.gfm.tables.TableBlock
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig
import javax.inject.Inject
import kotlin.random.Random

class SrsFragment : Fragment() {
    private val args: SrsFragmentArgs by navArgs()

    @Inject lateinit var modelFactory: ViewModelProvider.Factory
    private lateinit var binding: FragmentSrsBinding
    private lateinit var viewModel: HyperViewModel

    private var newCardList = mutableListOf<Card>()
    private var dueCardList = mutableListOf<Card>()
    private var saveCardList = mutableListOf<Card>()

    private var newCardMode: Int? = 0
    private var algorithm: SrsAlgorithm? = null
    private val algorithms = listOf(SM2,HC1)

    private var repeatUntil: Int = 0

    private lateinit var markwon: Markwon

    override fun onAttach(context: Context) {
        (context.applicationContext as HyperApp).hyperComponent.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this, modelFactory)[HyperViewModel::class.java]
        binding = FragmentSrsBinding.inflate(layoutInflater)

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resetOnClickListeners()
    }

    private fun resetOnClickListeners() {
        var selectedGrade = 50
        var selectedRecall = false

        binding.showAnswerButton.setOnClickListener {
            binding.questionLayout.visibility = View.INVISIBLE
            binding.answerLayout.visibility = View.VISIBLE
            binding.recallFeedback.visibility = View.VISIBLE
            binding.gradeSelector.visibility = View.INVISIBLE
            binding.wrongTextView.text = getString(R.string.wrong)
            binding.rightTextView.text = getString(R.string.right)
        }

        fun recallListener(recall: Boolean) {
            selectedRecall = recall
            binding.recallFeedback.visibility = View.INVISIBLE
            binding.gradeSelector.visibility = View.VISIBLE
            binding.wrongTextView.text = getString(R.string.unfamiliar)
            binding.rightTextView.text = getString(R.string.familiar)
        }

        binding.wrongButton.setOnClickListener { recallListener(false) }
        binding.rightButton.setOnClickListener { recallListener(true) }

        binding.returnButton.setOnClickListener { findNavController().navigateUp() }

        binding.gradeSelector.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedGrade = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) { }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let { handleGrade(selectedGrade/it.max.toFloat(), selectedRecall) }
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        markwon = Markwon.builder(requireContext())
            .usePlugin(TableEntryPlugin.create(requireContext()))
            .usePlugin(PicassoImagesPlugin.create(requireContext()))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.urlProcessor(UrlProcessorRelativeToAbsolute("file://${activity?.applicationContext?.getExternalFilesDir(null)?.absolutePath}/media/"))
                }

                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder.headingBreakHeight(0)
                }
            })
            .build()

        newCardMode = Integer.parseInt(prefs?.getString("srs_newcards","$MODE_LEARNT") ?: "$MODE_LEARNT")
        val algo = Integer.parseInt(prefs?.getString("srs_algorithm","$ALG_HC1") ?: "$ALG_HC1")
        val ri: Double = (prefs?.getInt("retention_index",90) ?: 90).toDouble()/100.0
        algorithm = when (algo) {
            ALG_SM2 -> SM2.also { it.ri = ri }
            ALG_HC1 -> HC1.also { it.ri = ri }
            else -> null
        }

        lifecycleScope.launch {
            fillCardSet()
            nextCard()
            intro()
        }

        (activity as HyperActivity).onBackPressedListener = object : HyperActivity.OnBackPressedListener {
            override fun doOnBackPressed(): Boolean {
                val lastCard = saveCardList.lastOrNull()
                lastCard?.let {
                    if (lastCard.due == null) {
                        newCardList.add(0, lastCard)
                    } else {
                        dueCardList.add(0, lastCard)
                    }
                    saveCardList.remove(lastCard)
                    repeatUntil++
                    nextCard()
                    return false
                }
                return true
            }
        }

        super.onActivityCreated(savedInstanceState)
    }

    private suspend fun fillCardSet() {
        dueCardList = when (args.level) {
            Level.COURSES -> viewModel.getDueFromCoursesAsync(args.full,args.units).toMutableList()
            Level.LESSONS -> viewModel.getDueFromLessonsAsync(args.full,args.units).toMutableList()
            Level.CARDS -> emptyList<Card>().toMutableList()
        }

        if (!args.full && args.level == Level.COURSES) {
            newCardList = viewModel.getNewCardsFromCoursesAsync(args.units).toMutableList()
        }
    }

    private fun nextCard() {
        binding.questionLayout.visibility = View.VISIBLE
        binding.answerLayout.visibility = View.INVISIBLE
        binding.noMoreQuestions.visibility = View.INVISIBLE
        binding.gradeSelector.progress = binding.gradeSelector.max/2
        infoShowable = true
        activity?.invalidateOptionsMenu()

        when {
            newCardList.isNotEmpty() -> {
                binding.currentCard = newCardList[0]
                when (newCardMode) {
                    MODE_LEARNT -> {}
                    MODE_INFO -> checkShowInfoFile()
                    MODE_DROPOUT -> doDropout()
                    MODE_INFO_DROPOUT -> { checkShowInfoFile(); doDropout() }
                }
                lifecycleScope.launch {
                    binding.currentColumnName =
                        "${viewModel.getCourseAsync(binding.currentCard!!.course_id).name}/" +
                                "${viewModel.getLessonAsync(binding.currentCard!!.lesson_id).name}: " +
                                binding.currentCard!!.a_col_name
                }
                return
            }

            dueCardList.isNotEmpty() -> {
                binding.currentCard = if (repeatUntil > 0) dueCardList[0] else dueCardList.elementAt(Random.nextInt(dueCardList.size))
                lifecycleScope.launch {
                    binding.currentColumnName =
                        "${viewModel.getCourseAsync(binding.currentCard!!.course_id).name}/" +
                                "${viewModel.getLessonAsync(binding.currentCard!!.lesson_id).name}: " +
                                binding.currentCard!!.a_col_name
                }
            }

            else -> {
                binding.questionLayout.visibility = View.INVISIBLE
                binding.answerLayout.visibility = View.INVISIBLE
                binding.noMoreQuestions.visibility = View.VISIBLE
                infoShowable = false
                activity?.invalidateOptionsMenu()
            }
        }
    }

    private val runNextCard = Runnable { nextCard() }
    private fun handleGrade(grade: Float, recall: Boolean) = lifecycleScope.launch {
        saveCardList.add(binding.currentCard!!)
        if (repeatUntil > 0) repeatUntil--
        val updatedCard = withContext(Dispatchers.Default) {
            for (alg in algorithms)
                alg.updateParams(binding.currentCard!!, grade, recall)
            algorithm?.updateCard(binding.currentCard!!)
        }

        updatedCard?.let { viewModel.update(it) }

        if (binding.currentCard!! in newCardList) {
            newCardList.remove(binding.currentCard!!)
            val course = viewModel.getCourseAsync(binding.currentCard!!.course_id)
            course.new_studied_today++
            viewModel.update(course)
        } else {
            dueCardList.remove(binding.currentCard!!)
        }

        Handler().postDelayed(runNextCard, 200)
    }

    private val alreadyShownCourses = mutableListOf<Int>()
    private fun checkShowInfoFile() {
        if (binding.currentCard!!.course_id !in alreadyShownCourses) {
            alreadyShownCourses.add(binding.currentCard!!.course_id)
            showInfoFile()
        }
    }

    private fun showInfoFile() {
        if (binding.currentCard?.info_file == null) {
            activity?.badSnack(getString(R.string.snack_noinfo))
            return
        }

        val txtView = RecyclerView(requireContext())
        val builder = activity?.let { MaterialAlertDialogBuilder(it) }
        builder?.setTitle(R.string.info_file_dialog)
            ?.setView(txtView)
            ?.setPositiveButton(R.string.ok) { _,_ -> }
        val dialog: Dialog? = builder?.create()
        dialog?.show()

        val info = HyperDataConverter(activity as HyperActivity).getInfoFile(binding.currentCard?.info_file!!)

        val adapter = MarkwonAdapter.builderTextViewIsRoot(R.layout.txtview)
            .include(TableBlock::class.java, TableEntry.create { b -> b
                .tableLayout(R.layout.tablelayout,R.id.tablelayout)
                .textLayoutIsRoot(R.layout.txtview)
                .build() })
            .build()
        adapter.setMarkdown(markwon,info)
        txtView.adapter = adapter
        txtView.layoutManager = LinearLayoutManager(requireContext(),RecyclerView.VERTICAL, false)
        adapter.notifyDataSetChanged()
    }

    private fun doDropout() {
        binding.wrongButton.setOnClickListener {
            binding.recallFeedback.visibility = View.INVISIBLE
            binding.wrongTextView.text = ""
            binding.rightTextView.text = ""
            newCardList.remove(binding.currentCard!!)
            newCardList.add(binding.currentCard!!)
            resetOnClickListeners()
            nextCard()
        }
    }

    private var infoShowable = true
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.app_bar_info).isVisible = infoShowable
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.app_bar_info)?.isVisible = infoShowable
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.app_bar_info) showInfoFile()
        return super.onOptionsItemSelected(item)
    }

    private fun intro() {
        if (newCardList.isNotEmpty() || dueCardList.isNotEmpty()) {
            val bg = ContextCompat.getColor(activity as Context,
                R.color.colorIntroBg
            )
            val fg = ContextCompat.getColor(activity as Context,
                R.color.colorIntroFg
            )

            val config = ShowcaseConfig()
            config.delay = 200

            val sequence = MaterialShowcaseSequence(activity, SRS_SHOW)

            sequence.setConfig(config)

            sequence.setOnItemShownListener { _, _ ->  (activity as HyperActivity).showing = true }

            sequence.addSequenceItem(
                MaterialShowcaseView.Builder(activity)
                    .setTarget(binding.questionCard)
                    .withRectangleShape()
                    .setContentText(getString(R.string.intro9))
                    .setMaskColour(bg)
                    .setDismissText(getString(R.string.got_it))
                    .setDismissTextColor(fg)
                    .setDismissOnTouch(true)
                    .setDismissOnTargetTouch(true)
                    .build()
            )

            sequence.addSequenceItem(
                MaterialShowcaseView.Builder(activity)
                    .setTarget(binding.showAnswerButton)
                    .withOvalShape()
                    .setContentText(getString(R.string.intro10))
                    .setMaskColour(bg)
                    .setDismissOnTouch(true)
                    .setDismissOnTargetTouch(true)
                    .setDismissTextColor(fg)
                    .build()
            )

            sequence.addSequenceItem(
                MaterialShowcaseView.Builder(activity)
                    .setTarget(binding.answerCard)
                    .setDismissText(getString(R.string.got_it))
                    .withRectangleShape()
                    .setContentText(getString(R.string.intro11))
                    .setMaskColour(bg)
                    .setDismissTextColor(fg)
                    .setDismissOnTouch(true)
                    .setDismissOnTargetTouch(true)
                    .build()
            )

            sequence.addSequenceItem(
                MaterialShowcaseView.Builder(activity)
                    .setTarget(binding.recallFeedback)
                    .withRectangleShape()
                    .setContentText(getString(R.string.intro12))
                    .setMaskColour(bg)
                    .setDismissOnTouch(true)
                    .setDismissOnTargetTouch(true)
                    .setDismissTextColor(fg)
                    .build()
            )

            sequence.addSequenceItem(
                MaterialShowcaseView.Builder(activity)
                    .setTarget(binding.gradeSelector)
                    .withRectangleShape()
                    .setContentText(getString(R.string.intro13))
                    .setMaskColour(bg)
                    .setDismissText(getString(R.string.got_it))
                    .setDismissOnTouch(true)
                    .setDismissOnTargetTouch(true)
                    .setDismissTextColor(fg)
                    .build()
            )

            sequence.setOnItemDismissedListener { _, pos ->
                if (pos == 1) {
                    binding.showAnswerButton.callOnClick()
                } else if (pos == 3) {
                    binding.wrongButton.callOnClick()
                }
                (activity as HyperActivity).showing = false
            }

            sequence.start()
        }
    }
}
