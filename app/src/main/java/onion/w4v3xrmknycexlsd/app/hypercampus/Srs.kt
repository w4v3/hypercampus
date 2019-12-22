package onion.w4v3xrmknycexlsd.app.hypercampus

import android.content.Context
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import onion.w4v3xrmknycexlsd.app.hypercampus.databinding.FragmentSrsBinding
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class Srs : Fragment() {
    private val args: SrsArgs by navArgs()

    private lateinit var binding: FragmentSrsBinding
    private lateinit var viewModel: HyperViewModel

    private var newCardList = mutableListOf<Card>()
    private var reviewCardSet = mutableSetOf<Card>()

    private var newCardMode: Int? = 0
    private var algorithm: SrsAlgorithm? = null

    private var selectedGrade: Int = 50

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSrsBinding.inflate(layoutInflater)

        binding.showAnswerButton.setOnClickListener {
            binding.questionLayout.visibility = View.INVISIBLE
            binding.answerLayout.visibility = View.VISIBLE
        }

        binding.returnButton.setOnClickListener { findNavController().navigateUp() }

        binding.gradeSelector.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedGrade = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) { }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let { handleGrade(selectedGrade/it.max.toFloat()) }
            }
        })

        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        newCardMode = MODE_LEARNT
        val algo = Integer.parseInt(prefs?.getString("srs_algorithm","$ALG_SM2") ?: "$ALG_SM2")
        algorithm = when (algo) {
            ALG_SM2 -> SM2
            else -> null
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this).get(HyperViewModel::class.java)

        viewModel.runAsync {
            fillCardSet()
            nextCard()
            intro()
        }

        super.onActivityCreated(savedInstanceState)
    }

    private suspend fun fillCardSet() {
        reviewCardSet = when (args.level) {
            Level.COURSES -> viewModel.getDueFromCoursesAsync(args.full,args.units).await().toMutableSet()
            Level.LESSONS -> viewModel.getDueFromLessonsAsync(args.full,args.units).await().toMutableSet()
            Level.CARDS -> emptySet<Card>().toMutableSet()
        }

        if (!args.full && args.level==Level.COURSES) {
            newCardList = viewModel.getNewCardsAsync(args.units).await().toMutableList()
        }
    }

    private fun nextCard() {
        binding.questionLayout.visibility = View.VISIBLE
        binding.answerLayout.visibility = View.INVISIBLE
        binding.gradeSelector.progress = binding.gradeSelector.max/2

        when {
            newCardList.isNotEmpty() -> {
                when (newCardMode) {
                    MODE_INFO -> showInfoFile()
                    MODE_DROPOUT -> initiateDropout()
                    MODE_LEARNT -> { binding.currentCard = newCardList[0]; return }
                }
            }

            reviewCardSet.isNotEmpty() -> {
                binding.currentCard = reviewCardSet.elementAt(Random.nextInt(reviewCardSet.size))
            }

            else -> {
                binding.questionView.visibility = View.INVISIBLE
                binding.noMoreQuestions.visibility = View.VISIBLE
            }
        }
    }

    private val runNextCard = Runnable { nextCard() }
    private fun handleGrade(grade: Float) = viewModel.runAsync {
        val interval = algorithm!!.calculateInterval(binding.currentCard!!,grade)
        val currentDate = Calendar.getInstance(Locale.US)
        currentDate.add(Calendar.DATE,interval)
        val nextDueDate = SimpleDateFormat("yyyyMMdd", Locale.US).format(currentDate.time).toInt()

        binding.currentCard!!.due = nextDueDate
        viewModel.update(binding.currentCard!!)

        if (binding.currentCard!! in newCardList) {
            newCardList.remove(binding.currentCard!!)
            val course = viewModel.getCourseAsync(binding.currentCard!!.course_id).await()
            course.new_studied_today++
            viewModel.update(course)
        } else {
            reviewCardSet.remove(binding.currentCard!!)
        }

        Handler().postDelayed(runNextCard, 200)
    }

    private fun showInfoFile() {
        newCardList.clear()
    }

    private fun initiateDropout() {
        newCardList.clear()
    }

    private fun intro() {
        if (newCardList.isNotEmpty() || reviewCardSet.isNotEmpty()) {
            val bg = ContextCompat.getColor(activity as Context, R.color.colorIntroBg)
            val fg = ContextCompat.getColor(activity as Context, R.color.colorIntroFg)

            val config = ShowcaseConfig()
            config.delay = 200

            val sequence = MaterialShowcaseSequence(activity, SRS_SHOW)

            sequence.setConfig(config)

            sequence.setOnItemShownListener { _, _ ->  (activity as MainActivity).showing = true }

            sequence.addSequenceItem(
                MaterialShowcaseView.Builder(activity)
                    .setTarget(binding.questionCard)
                    .setDismissText(getString(R.string.got_it))
                    .withRectangleShape()
                    .setContentText(getString(R.string.intro9))
                    .setMaskColour(bg)
                    .setDismissTextColor(fg)
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
                    .build()
            )

            sequence.addSequenceItem(
                MaterialShowcaseView.Builder(activity)
                    .setTarget(binding.gradeSelector)
                    .withRectangleShape()
                    .setContentText(getString(R.string.intro12))
                    .setMaskColour(bg)
                    .setDismissText(getString(R.string.got_it))
                    .setDismissOnTouch(true)
                    .setDismissOnTargetTouch(true)
                    .setDismissTextColor(fg)
                    .build()
            )

            sequence.setOnItemDismissedListener { _, pos ->
                if (pos == 1) {
                    binding.questionLayout.visibility = View.INVISIBLE
                    binding.answerLayout.visibility = View.VISIBLE
                }
                (activity as MainActivity).showing = false
            }

            sequence.start()
        }
    }
}

sealed class SrsAlgorithm {
    abstract suspend fun calculateInterval(card: Card, grade: Float): Int
}

object SM2: SrsAlgorithm() {
    override suspend fun calculateInterval(card: Card, grade: Float): Int {
        return 0
    }
}
