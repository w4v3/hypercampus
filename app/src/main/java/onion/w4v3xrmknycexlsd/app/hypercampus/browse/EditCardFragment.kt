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

    @Inject
    lateinit var modelFactory: ViewModelProvider.Factory
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
            if (binding.editCard!!.id == 0) {
                viewModel.add(binding.editCard!!)
            } else {
                viewModel.update(binding.editCard!!)
            }
            val action = EditCardFragmentDirections.backToCards(args.lessonId)
            findNavController().navigate(action)
        }

        binding.addImage.setOnClickListener {
            lifecycleScope.launch {
                val name =
                    viewModel.getCourseAsync(args.courseId).name + "_" + viewModel.getLessonAsync(
                        args.lessonId
                    ).name + "_0"
                val tag = HyperDataConverter(requireActivity() as HyperActivity).addMedia(
                    name,
                    FILE_IMAGE
                )
                if (binding.editAnswer.hasFocus()) {
                    binding.editCard!!.answer += "\n" + tag
                } else {
                    binding.editCard!!.question += "\n" + tag
                }
                binding.invalidateAll()
            }
        }
        binding.addSound.setOnClickListener {
            lifecycleScope.launch {
                val name =
                    viewModel.getCourseAsync(args.courseId).name + "_" + viewModel.getLessonAsync(
                        args.lessonId
                    ).name + "_0"
                val tag = HyperDataConverter(requireActivity() as HyperActivity).addMedia(
                    name,
                    FILE_AUDIO
                )
                if (binding.editAnswer.hasFocus()) {
                    binding.editCard!!.answer += "\n" + tag
                } else {
                    binding.editCard!!.question += "\n" + tag
                }
                binding.invalidateAll()
            }
        }

        findNavController().addOnDestinationChangedListener { _, _, _ ->
            activity?.let {
                hideSoftKeyboard(
                    it
                )
            }
        }

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
            })
        }

        super.onActivityCreated(savedInstanceState)
    }
}
