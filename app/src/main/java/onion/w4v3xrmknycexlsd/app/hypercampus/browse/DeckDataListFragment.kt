package onion.w4v3xrmknycexlsd.app.hypercampus.browse

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import onion.w4v3xrmknycexlsd.app.hypercampus.*
import onion.w4v3xrmknycexlsd.app.hypercampus.data.*
import onion.w4v3xrmknycexlsd.app.hypercampus.databinding.DeckdataListBinding
import onion.w4v3xrmknycexlsd.app.hypercampus.databinding.DialogAddCourseBinding
import onion.w4v3xrmknycexlsd.app.hypercampus.databinding.DialogAddLessonBinding
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig
import javax.inject.Inject

open class DeckDataListFragment : Fragment(),
    DeckDataAdapter.OnItemClickListener {
    private val args: DeckDataListFragmentArgs by navArgs()

    private lateinit var label: String

    private var currentCourse: Course? = null
    private var currentLesson: Lesson? = null

    @Inject lateinit var modelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: HyperViewModel
    private lateinit var binding: DeckdataListBinding
    private lateinit var adapter: DeckDataAdapter

    private var multiSelect: Boolean = false
    private var selected: MutableList<DeckData> = mutableListOf()
    private var selectionMode: ActionMode? = null

    override fun onAttach(context: Context) {
        (context.applicationContext as HyperApp).hyperComponent.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DeckdataListBinding.inflate(layoutInflater, container, false)

        adapter =
            DeckDataAdapter(this)
        binding.list.adapter = adapter

        setHasOptionsMenu(true)

        label = when (args.level) {
            Level.COURSES -> getString(
                R.string.course
            )
            Level.LESSONS -> getString(
                R.string.lesson
            )
            Level.CARDS -> getString(
                R.string.card
            )
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this, modelFactory)[HyperViewModel::class.java]

        lifecycleScope.launch {
            val toObserve = when (args.level) {
                Level.COURSES -> viewModel.allCourses
                Level.LESSONS -> viewModel.getCourseLessons(args.dataId)
                Level.CARDS -> viewModel.getLessonCards(args.dataId)
            }

            toObserve.observe(viewLifecycleOwner, Observer { data ->
                data?.let { adapter.setData(it); updateCounts() }
            })

            when (args.level) {
                Level.COURSES -> {}
                Level.LESSONS -> {
                    currentCourse = viewModel.getCourseAsync(args.dataId)
                    (activity as HyperActivity).binding.appBar.title = currentCourse?.name
                }
                Level.CARDS -> {
                    currentLesson = viewModel.getLessonAsync(args.dataId)
                    (activity as HyperActivity).binding.appBar.title =
                        "${viewModel.getCourseAsync(currentLesson!!.course_id).symbol} > ${currentLesson?.name}"
                }
            }


            // app intro
            val pref = withContext(Dispatchers.IO) { activity?.getSharedPreferences("material_showcaseview_prefs", Context.MODE_PRIVATE) }
            if (pref?.getBoolean("first_time_$label", true) == true) {
                if (args.level == Level.COURSES) {
                    val builder = activity?.let { MaterialAlertDialogBuilder(it) }
                    builder?.setTitle(R.string.app_name)
                        ?.setMessage(R.string.welcome)
                        ?.setIcon(R.drawable.ic_logo)
                        ?.setPositiveButton(getString(R.string.start_tour)) { _, _ ->
                            intro()
                        }
                        ?.setNegativeButton(getString(R.string.no_thx)) { _, _ ->
                            with(pref.edit()) {
                                for (s in SHOWCASE) putInt("status_$s", -1)
                                apply()
                            }
                        }

                    val dialog: Dialog? = builder?.create()
                    dialog?.show()
                } else {
                    intro()
                }

                with(pref.edit()) {
                    putBoolean("first_time_$label", false)
                    apply()
                }
            }
        }

        findNavController().addOnDestinationChangedListener { _, _, _ -> selected.clear(); selectionMode?.invalidate() }

        super.onActivityCreated(savedInstanceState)
    }

    override fun onItemClick(item: DeckData) {
        when {
            item in selected -> {
                selected.remove(item)
                selectionMode?.invalidate()
            }
            multiSelect -> {
                selected.add(item)
                selectionMode?.invalidate()
            }
            else -> {
                val action =
                    when (args.level) {
                        Level.COURSES -> CoursesListDirections.nextAction(
                            (item as Course).id
                        )
                        Level.LESSONS -> LessonsListDirections.nextAction(
                            (item as Lesson).id
                        )
                        Level.CARDS -> CardsListDirections.nextAction(
                            (item as Card).id,
                            lessonId = currentLesson!!.id,
                            courseId = currentLesson!!.course_id
                        )
                    }
                findNavController().navigate(action)
            }
        }
    }

    override fun onButtonClick(item: DeckData) {
        val action = when (args.level) {
            Level.COURSES -> CoursesListDirections.actionToSrs(
                intArrayOf((item as Course).id),
                Level.COURSES
            )
            Level.LESSONS -> LessonsListDirections.actionToSrs(
                intArrayOf((item as Lesson).id),
                Level.LESSONS
            )
            Level.CARDS -> null
        }
        action?.let { findNavController().navigate(it) }
    }

    override fun onItemLongClick(item: DeckData) {
        when {
            item in selected -> {
                selected.remove(item)
                selectionMode?.invalidate()
            }
            multiSelect -> {
                selected.add(item)
                selectionMode?.invalidate()
            }
            else -> {
                selected.add(item)
                multiSelect = true
                selectionMode = activity?.startActionMode(callback)
            }
        }
    }

    private val callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater: MenuInflater = mode.menuInflater
            inflater.inflate(R.menu.selection_menu, menu)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.app_bar_delete -> {
                    deleteSelected()
                    true
                }
                R.id.app_bar_edit -> {
                    editSelected()
                    mode.finish()
                    true
                }
                else -> false
            }
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            when (selected.size) {
                0 -> mode.finish()
                1 -> menu.findItem(R.id.app_bar_edit).isVisible = true
                else -> menu.findItem(R.id.app_bar_edit).isVisible = false
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            selected.clear()
            adapter.deselectAll()
            multiSelect = false
            selectionMode = null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        (menu.findItem(R.id.app_bar_search).actionView as SearchView).apply {
            setIconifiedByDefault(true)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    adapter.filter(query)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    adapter.filter(newText)
                    return true
                }
            })
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.app_bar_add -> {
                if (args.level == Level.CARDS) {
                    val action =
                        CardsListDirections.nextAction(
                            courseId = currentLesson!!.course_id,
                            lessonId = currentLesson!!.id
                        )
                    findNavController().navigate(action)
                    return true
                }

                val cardBinding = when (args.level) {
                    Level.COURSES -> DialogAddCourseBinding.inflate(layoutInflater)
                    Level.LESSONS -> DialogAddLessonBinding.inflate(layoutInflater)
                    Level.CARDS -> null
                }

                when (args.level) {
                    Level.COURSES -> (cardBinding as DialogAddCourseBinding).editCourse =
                        Course(0)
                    Level.LESSONS -> (cardBinding as DialogAddLessonBinding).editLesson =
                        currentCourse?.let {
                            Lesson(
                                0,
                                it.id
                            )
                        }
                    Level.CARDS -> {}
                }

                val builder = activity?.let { MaterialAlertDialogBuilder(it) }
                builder?.setTitle(getString(R.string.new_,label))
                    ?.setView(cardBinding?.root)
                    ?.setPositiveButton(getString(R.string.add)) { _, _ ->
                        when (args.level) {
                            Level.COURSES -> (cardBinding as DialogAddCourseBinding).editCourse?.let {
                                viewModel.add(it)
                            }
                            Level.LESSONS -> (cardBinding as DialogAddLessonBinding).editLesson?.let {
                                viewModel.add(it)
                            }
                            Level.CARDS -> {}
                        }
                    }
                    ?.setNegativeButton(getString(R.string.cancel)) { _, _ -> }

                val dialog: Dialog? = builder?.create()
                dialog?.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun deleteSelected() {
        val builder =  activity?.let { MaterialAlertDialogBuilder(it) }
        builder?.setTitle(resources.getQuantityString(R.plurals.deleting_dialog, selected.size, selected.size, label))
            ?.setMessage(getString(R.string.are_you_sure))
            ?.setPositiveButton(getString(R.string.ok)) { _, _ ->
                for (i in selected) {
                    viewModel.delete(i)
                }
                selectionMode?.finish()
            }
            ?.setNegativeButton(getString(R.string.cancel)) { _, _ -> }

        val dialog: Dialog? = builder?.create()
        dialog?.show()
    }

    fun editSelected() {
        if (args.level == Level.CARDS) {
            val action =
                CardsListDirections.nextAction(
                    (selected[0] as Card).id,
                    courseId = currentLesson!!.course_id,
                    lessonId = currentLesson!!.id
                )
            findNavController().navigate(action)
            return
        }

        val cardBinding = when (args.level) {
            Level.COURSES -> DialogAddCourseBinding.inflate(layoutInflater)
            Level.LESSONS -> DialogAddLessonBinding.inflate(layoutInflater)
            Level.CARDS -> null
        }

        when (args.level) {
            Level.COURSES -> (cardBinding as DialogAddCourseBinding).editCourse =
                selected[0] as Course
            Level.LESSONS -> (cardBinding as DialogAddLessonBinding).editLesson =
                selected[0] as Lesson
            Level.CARDS -> {
            }
        }

        val builder =  activity?.let { MaterialAlertDialogBuilder(it) }
        builder?.setTitle(getString(R.string.edit_, label))
            ?.setView(cardBinding?.root)
            ?.setPositiveButton(getString(R.string.edit_dialog)) { _, _ ->
                when (args.level) {
                    Level.COURSES -> (cardBinding as DialogAddCourseBinding).editCourse?.let {
                        viewModel.update(it)
                    }
                    Level.LESSONS -> (cardBinding as DialogAddLessonBinding).editLesson?.let {
                        viewModel.update(it)
                    }
                    Level.CARDS -> {}
                }
            }
            ?.setNegativeButton(getString(R.string.cancel)) { _, _ -> }

        val dialog: Dialog? = builder?.create()
        dialog?.show()
    }

    private fun updateCounts() = lifecycleScope.launch {
        when (args.level) {
            Level.COURSES -> {
                adapter.setDueCounts(viewModel.countDuePerCourseAsync())
                adapter.setNewCounts(viewModel.countNewPerCourseAsync())
            }
            Level.LESSONS -> adapter.setNewCounts(viewModel.countDuePerLessonAsync(args.dataId))
            Level.CARDS -> {}
        }}

    private fun insertSamples() {
        viewModel.add(Course( 1, "🌍", "Geography" ))
        viewModel.add(Course( 2, "🔤", "Fancy English Words" ))
        viewModel.add(Lesson( 1, 1, "🏰", "Capitals" ))
        viewModel.add(Lesson(2, 2, "1", "Shakespeare" ))
        viewModel.add(Card( 0, 1, 1, "Spain", "Madrid" ))
        viewModel.add(Card( 0, 1, 1, "Egypt", "Kairo" ))
        viewModel.add(Card( 0, 1, 1, "India", "New Delhi" ))
        viewModel.add(Card( 0, 2, 2, "simular", "false, counterfeit" ) )
        viewModel.add(Card( 0, 2, 2, "to perpend", "to ponder" ) )
    }

    private fun intro() {
        val bg = ContextCompat.getColor(activity as Context, R.color.colorIntroBg )
        val fg = ContextCompat.getColor(activity as Context, R.color.colorIntroFg )

        val config = ShowcaseConfig()
        config.delay = 200

        val sequence = MaterialShowcaseSequence(activity, when (args.level) {
            Level.COURSES -> COURSE_SHOW
            Level.LESSONS -> LESSON_SHOW
            Level.CARDS -> CARD_SHOW
        })

        sequence.setConfig(config)

        sequence.setOnItemShownListener { _, _ ->  (activity as HyperActivity).showing = true }
        sequence.setOnItemDismissedListener { _, _ ->  (activity as HyperActivity).showing = false }

        when (args.level) {
            Level.COURSES -> {
                val item = binding.dummyCourse

                sequence.addSequenceItem(MaterialShowcaseView.Builder(activity)
                    .setTarget(activity?.findViewById(R.id.app_bar_add))
                    .setContentText(getString(R.string.intro1))
                    .setMaskColour(bg)
                    .setDismissOnTouch(true)
                    .setDismissOnTargetTouch(true)
                    .build()
                )

                sequence.addSequenceItem(MaterialShowcaseView.Builder(activity)
                    .setTarget(item.findViewById(R.id.review_button))
                    .setContentText(getString(R.string.intro2))
                    .setDismissText(getString(R.string.got_it))
                    .setMaskColour(bg)
                    .setDismissTextColor(fg)
                    .withOvalShape()
                    .build()
                )

                sequence.addSequenceItem(MaterialShowcaseView.Builder(activity)
                    .setTarget((activity as HyperActivity).binding.floatingActionButton)
                    .setDismissText(getString(R.string.got_it))
                    .setContentText(getString(R.string.intro3))
                    .setShapePadding(64)
                    .setMaskColour(bg)
                    .setDismissTextColor(fg)
                    .build()
                )
                sequence.addSequenceItem(MaterialShowcaseView.Builder(activity)
                    .setTarget(item)
                    .setContentText(getString(R.string.intro4))
                    .withRectangleShape(true)
                    .setMaskColour(bg)
                    .setDismissOnTargetTouch(true)
                    .setDismissOnTouch(true)
                    .build()
                )

                sequence.setOnItemDismissedListener { _, position ->
                    when (position) {
                        0 -> insertSamples()
                        3 -> findNavController().navigate(R.id.next_action)
                    }
                }
            }
            Level.LESSONS -> {
                val item = binding.dummyCourse
                sequence.addSequenceItem(MaterialShowcaseView.Builder(activity)
                    .setTarget(item)
                    .setContentText(getString(R.string.intro5))
                    .withRectangleShape(true)
                    .setMaskColour(bg)
                    .setDismissOnTouch(true)
                    .setDismissOnTargetTouch(true)
                    .build()
                )

                sequence.setOnItemDismissedListener { _, _ -> findNavController().navigate(R.id.next_action) }
            }
            Level.CARDS -> {
                val item = binding.dummyWord
                sequence.addSequenceItem(MaterialShowcaseView.Builder(activity)
                    .setTarget(item.findViewById(R.id.label_short))
                    .setDismissText(getString(R.string.got_it))
                    .withRectangleShape()
                    .setContentText(getString(R.string.intro6))
                    .setMaskColour(bg)
                    .setDismissTextColor(fg)
                    .build()
                )

                sequence.addSequenceItem(MaterialShowcaseView.Builder(activity)
                    .setTarget(item.findViewById(R.id.label_full))
                    .setDismissText(getString(R.string.got_it))
                    .withRectangleShape()
                    .setContentText(getString(R.string.intro7))
                    .setMaskColour(bg)
                    .setDismissTextColor(fg)
                    .build()
                )

                sequence.addSequenceItem(MaterialShowcaseView.Builder(activity)
                    .setTarget((activity as HyperActivity).binding.floatingActionButton)
                    .setShapePadding(64)
                    .setContentText(getString(R.string.intro8))
                    .setMaskColour(bg)
                    .setDismissOnTargetTouch(true)
                    .setDismissOnTouch(true)
                    .build()
                )
                sequence.setOnItemDismissedListener { _, position -> if (position == 2) findNavController().navigate(R.id.action_to_srs) }
            }
        }
        sequence.start()
    }
}

enum class Level { COURSES, LESSONS, CARDS }

class CoursesList : DeckDataListFragment()
class LessonsList : DeckDataListFragment()
class CardsList : DeckDataListFragment()
