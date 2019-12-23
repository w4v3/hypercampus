package onion.w4v3xrmknycexlsd.app.hypercampus


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import onion.w4v3xrmknycexlsd.app.hypercampus.databinding.FragmentEditCardBinding

class EditCard : Fragment() {
    private val args: EditCardArgs by navArgs()

    private lateinit var viewModel: HyperViewModel
    private lateinit var binding: FragmentEditCardBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditCardBinding.inflate(layoutInflater)

        binding.submitButton.setOnClickListener {
            if (args.cardId == -1) {
                viewModel.add(binding.editCard!!)
            } else {
                viewModel.update(binding.editCard!!)
            }
            val action = EditCardDirections.backToCards(args.lessonId)
            findNavController().navigate(action)
        }

        //setMenuOnClickListeners()

        findNavController().addOnDestinationChangedListener { _, _, _ -> activity?.let { hideSoftKeyboard(it) } }

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this).get(HyperViewModel::class.java)

        if (args.cardId == -1) {
            binding.editCard = Card(0,args.courseId,args.lessonId)
        } else {
            viewModel.getCard(args.cardId).observe(viewLifecycleOwner, Observer { data ->
                binding.editCard = data
                //binding.editQuestion.html = data.question
            })
        }

        super.onActivityCreated(savedInstanceState)
    }

/*
    private fun setMenuOnClickListeners() {
        binding.editQuestion.setOnTextChangeListener { binding.editCard?.question = it }

        binding.actionUndo.setOnClickListener { binding.editQuestion.undo() }

        binding.actionRedo.setOnClickListener { binding.editQuestion.redo() }

        binding.actionBold.setOnClickListener { binding.editQuestion.setBold() }

        binding.actionItalic.setOnClickListener { binding.editQuestion.setItalic() }

        binding.actionSubscript.setOnClickListener { binding.editQuestion.setSubscript() }

        binding.actionSuperscript.setOnClickListener { binding.editQuestion.setSuperscript() }

        binding.actionStrikethrough.setOnClickListener { binding.editQuestion.setStrikeThrough() }

        binding.actionUnderline.setOnClickListener { binding.editQuestion.setUnderline() }

        binding.actionTxtColor.setOnClickListener(object :
            View.OnClickListener {
            private var isChanged = false
            override fun onClick(v: View) {
                binding.editQuestion.setTextColor(if (isChanged) Color.BLACK else Color.RED)
                isChanged = !isChanged
            }
        })

        binding.actionBgColor.setOnClickListener(object :
            View.OnClickListener {
            private var isChanged = false
            override fun onClick(v: View) {
                binding.editQuestion.setTextBackgroundColor(if (isChanged) Color.TRANSPARENT else Color.YELLOW)
                isChanged = !isChanged
            }
        })

        binding.actionIndent.setOnClickListener { binding.editQuestion.setIndent() }

        binding.actionOutdent.setOnClickListener { binding.editQuestion.setOutdent() }

        binding.actionAlignLeft.setOnClickListener { binding.editQuestion.setAlignLeft() }

        binding.actionAlignCenter.setOnClickListener { binding.editQuestion.setAlignCenter() }

        binding.actionAlignRight.setOnClickListener { binding.editQuestion.setAlignRight() }

        binding.actionBlockquote.setOnClickListener { binding.editQuestion.setBlockquote() }

        binding.actionInsertBullets.setOnClickListener { binding.editQuestion.setBullets() }

        binding.actionInsertNumbers.setOnClickListener { binding.editQuestion.setNumbers() }

        binding.actionInsertImage.setOnClickListener {
            binding.editQuestion.insertImage(
                "http://www.1honeywan.com/dachshund/image/7.21/7.21_3_thumb.JPG",
                "dachshund"
            )
        }

        binding.actionInsertLink.setOnClickListener {
            binding.editQuestion.insertLink(
                "https://github.com/wasabeef",
                "wasabeef"
            )
        }
    }

 */
}
