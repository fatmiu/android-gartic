package com.miumiu.gratic.ui.username

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.miumiu.gratic.R
import com.miumiu.gratic.databinding.FragmentUsernameBinding
import com.miumiu.gratic.util.Constants.MAX_USERNAME_LENGTH
import com.miumiu.gratic.util.Constants.MIN_USERNAME_LENGTH
import com.miumiu.gratic.util.hideKeyboard
import com.miumiu.gratic.util.navigateSafely
import com.miumiu.gratic.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UsernameFragment : Fragment() {

    private var _binding: FragmentUsernameBinding? = null
    private val binding: FragmentUsernameBinding
        get() = _binding!!

    private val viewModel: UsernameViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsernameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToEvents()

        binding.btnNext.setOnClickListener {
            viewModel.validateUsernameAndNavigateToSelectRoom(
                binding.etUsername.text.toString()
            )
            requireActivity().hideKeyboard(binding.root)
        }
    }

    private fun listenToEvents() {


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        UsernameViewModel.Event.InputEmptyError -> {
                            snackbar(R.string.error_field_empty)
                        }

                        UsernameViewModel.Event.InputTooLongError -> {
                            snackbar(
                                getString(
                                    R.string.error_username_too_long,
                                    MAX_USERNAME_LENGTH
                                )
                            )
                        }

                        UsernameViewModel.Event.InputTooShortError -> {
                            snackbar(
                                getString(
                                    R.string.error_username_too_short,
                                    MIN_USERNAME_LENGTH
                                )
                            )
                        }

                        is UsernameViewModel.Event.NavigateToSelectRoomEvent -> {
                            findNavController().navigateSafely(
                                R.id.action_usernameFragment_to_selectRoomFragment,
                                args = Bundle().apply { putString("username", event.username) }
                            )
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}