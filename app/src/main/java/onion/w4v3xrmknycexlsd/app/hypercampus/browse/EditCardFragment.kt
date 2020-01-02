package onion.w4v3xrmknycexlsd.app.hypercampus.browse


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import onion.w4v3xrmknycexlsd.app.hypercampus.*
import onion.w4v3xrmknycexlsd.app.hypercampus.data.Card
import onion.w4v3xrmknycexlsd.app.hypercampus.data.HyperDataConverter
import onion.w4v3xrmknycexlsd.app.hypercampus.data.HyperViewModel
import onion.w4v3xrmknycexlsd.app.hypercampus.databinding.FragmentEditCardBinding
import javax.inject.Inject

class EditCardFragment : Fragment() {
    private val args: EditCardFragmentArgs by navArgs()

    @Inject lateinit var modelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: HyperViewModel
    private lateinit var binding: FragmentEditCardBinding

    override fun onAttach(context: Context) {
        (context.applicationContext as HyperApp).hyperComponent.inject(this)
        super.onAttach(context)
    }

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
            val action =
                EditCardFragmentDirections.backToCards(
                    args.lessonId
                )
            findNavController().navigate(action)
        }

        binding.addImage.setOnClickListener { lifecycleScope.launch {
            if (binding.editAnswer.hasFocus()) {
                binding.editCard!!.answer += "\n" + HyperDataConverter(requireActivity() as HyperActivity).addMedia(binding.editCard!!.id,FILE_IMAGE)
            } else {
                binding.editCard!!.question += "\n" + HyperDataConverter(requireActivity() as HyperActivity).addMedia(binding.editCard!!.id,FILE_IMAGE)
            }
            binding.invalidateAll()
        }}
        binding.addSound.setOnClickListener { lifecycleScope.launch {
            if (binding.editAnswer.hasFocus()) {
                binding.editCard!!.answer += "\n" + HyperDataConverter(requireActivity() as HyperActivity).addMedia(binding.editCard!!.id,FILE_AUDIO)
            } else {
                binding.editCard!!.question += "\n" + HyperDataConverter(requireActivity() as HyperActivity).addMedia(binding.editCard!!.id,FILE_AUDIO)
            }
            binding.invalidateAll()
        }}

        findNavController().addOnDestinationChangedListener { _, _, _ -> activity?.let { hideSoftKeyboard(it) } }

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this, modelFactory)[HyperViewModel::class.java]

        if (args.cardId == -1) {
            binding.editCard = Card(0, args.courseId, args.lessonId)
        } else {
            viewModel.getCard(args.cardId).observe(viewLifecycleOwner, Observer { data ->
                binding.editCard = data
                //binding.editQuestion.html = data.question
            })
        }

        super.onActivityCreated(savedInstanceState)
    }
}
