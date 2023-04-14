package com.miumiu.gratic.ui.setup.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.miumiu.gratic.R
import com.miumiu.gratic.data.remote.ws.Room
import com.miumiu.gratic.databinding.FragmentCreateRoomBinding
import com.miumiu.gratic.ui.setup.viewmodels.CreateRoomViewModel
import com.miumiu.gratic.util.Constants
import com.miumiu.gratic.util.hideKeyboard
import com.miumiu.gratic.util.navigateSafely
import com.miumiu.gratic.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateRoomFragment : Fragment() {

    private var _binding: FragmentCreateRoomBinding? = null
    private val binding: FragmentCreateRoomBinding
        get() = _binding!!

    private val viewModel: CreateRoomViewModel by viewModels()
    private val args: CreateRoomFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupRoomSizeSpinner()
        listenToEvents()

        binding.btnCreateRoom.setOnClickListener {
            binding.createRoomProgressBar.isVisible = true
            viewModel.createRoom(
                Room(
                    binding.etRoomName.text.toString(),
                    binding.tvMaxPersons.text.toString().toInt()
                )
            )
            requireActivity().hideKeyboard(binding.root)
        }
    }

    private fun listenToEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is CreateRoomViewModel.Event.CreateRoomEvent -> {
                            viewModel.joinRoom(args.username, event.room.name)
                        }

                        is CreateRoomViewModel.Event.InputEmptyError -> {
                            binding.createRoomProgressBar.isVisible = false
                            snackbar(R.string.error_field_empty)
                        }

                        is CreateRoomViewModel.Event.InputTooShortError -> {
                            binding.createRoomProgressBar.isVisible = false
                            snackbar(
                                getString(
                                    R.string.error_room_name_too_short,
                                    Constants.MIN_ROOM_NAME_LENGTH
                                )
                            )
                        }

                        is CreateRoomViewModel.Event.InputTooLongError -> {
                            binding.createRoomProgressBar.isVisible = false
                            snackbar(
                                getString(
                                    R.string.error_room_name_too_long,
                                    Constants.MAX_ROOM_NAME_LENGTH
                                )
                            )
                        }

                        is CreateRoomViewModel.Event.CreateRoomErrorEvent -> {
                            binding.createRoomProgressBar.isVisible = false
                            snackbar(event.error)
                        }

                        is CreateRoomViewModel.Event.JoinRoomEvent -> {
                            binding.createRoomProgressBar.isVisible = false
                            findNavController().navigateSafely(
                                R.id.action_createRoomFragment_to_drawingActivity,
                                args = Bundle().apply {
                                    putString("username", args.username)
                                    putString("roomName", event.roomName)
                                }
                            )
                        }

                        is CreateRoomViewModel.Event.JoinRoomErrorEvent -> {
                            binding.createRoomProgressBar.isVisible = false
                            snackbar(event.error)
                        }
                    }

                }
            }
        }
    }

    private fun setupRoomSizeSpinner() {
        val roomSizes = resources.getStringArray(R.array.room_size_array)
        val adapter = ArrayAdapter(requireContext(), R.layout.textview_room_size, roomSizes)
        binding.tvMaxPersons.setAdapter(adapter)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}