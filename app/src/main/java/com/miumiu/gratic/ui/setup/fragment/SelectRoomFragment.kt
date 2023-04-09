package com.miumiu.gratic.ui.setup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.miumiu.gratic.R
import com.miumiu.gratic.databinding.FragmentSelectRoomBinding
import com.miumiu.gratic.ui.setup.SetupViewModel
import com.miumiu.gratic.ui.setup.adapters.RoomAdapter
import com.miumiu.gratic.util.Constants.SEARCH_DELAY
import com.miumiu.gratic.util.navigateSafely
import com.miumiu.gratic.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SelectRoomFragment : Fragment() {

    private var _binding: FragmentSelectRoomBinding? = null
    private val binding: FragmentSelectRoomBinding
        get() = _binding!!

    private val viewModel: SetupViewModel by activityViewModels()

    private val args: SelectRoomFragmentArgs by navArgs()

    @Inject
    lateinit var roomAdapter: RoomAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        subscribeToObservers()
        listenToEvents()

        viewModel.getRooms("")

        var searchJob: Job? = null
        binding.etRoomName.addTextChangedListener {
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(SEARCH_DELAY)
                viewModel.getRooms(it.toString())
            }
        }

        binding.ibReload.setOnClickListener {
            binding.roomsProgressBar.isVisible = true
            binding.tvNoRoomsFound.isVisible = false
            viewModel.getRooms(binding.etRoomName.text.toString())
        }

        binding.btnCreateRoom.setOnClickListener {
            findNavController().navigateSafely(
                R.id.action_selectRoomFragment_to_createRoomFragment,
                Bundle().apply { putString("username", args.username) }
            )
        }

        roomAdapter.setOnRoomClickListener {
            viewModel.joinRoom(args.username, it.name)
        }
    }

    private fun listenToEvents() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.setupEvent.collect { event ->
                when (event) {
                    is SetupViewModel.SetupEvent.JoinRoomEvent -> {
                        findNavController().navigateSafely(
                            R.id.action_selectRoomFragment_to_drawingActivity,
                            args = Bundle().apply {
                                putString("username", args.username)
                                putString("roomName", event.roomName)
                            }
                        )
                    }

                    is SetupViewModel.SetupEvent.JoinRoomErrorEvent -> {
                        snackbar(event.error)
                    }

                    is SetupViewModel.SetupEvent.GetRoomErrorEvent -> {
                        binding.apply {
                            roomsProgressBar.isVisible = false
                            tvNoRoomsFound.isVisible = false
                        }
                        snackbar(event.error)
                    }

                    else -> Unit
                }

            }
        }
    }

    private fun subscribeToObservers() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.rooms.collect { event ->
                when (event) {
                    is SetupViewModel.SetupEvent.GetRoomLoadingEvent -> {
                        binding.roomsProgressBar.isVisible = true
                    }

                    is SetupViewModel.SetupEvent.GetRoomEvent -> {
                        binding.roomsProgressBar.isVisible = false
                        val isRoomsEmpty = event.rooms.isEmpty()
                        binding.tvNoRoomsFound.isVisible = isRoomsEmpty
                        lifecycleScope.launch {
                            roomAdapter.updateDataset(event.rooms)
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setupRecyclerView() {
        binding.rvRooms.apply {
            adapter = roomAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
}