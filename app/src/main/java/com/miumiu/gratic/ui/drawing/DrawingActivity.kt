package com.miumiu.gratic.ui.drawing

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.miumiu.gratic.R
import com.miumiu.gratic.databinding.ActivityDrawingBinding
import com.miumiu.gratic.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DrawingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding

    private val viewModel: DrawingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        subscribeToUiStateUpdates()

        binding.colorGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.checkRadioButton(checkedId)
        }
    }

    private fun selectColor(color: Int) {
        binding.drawingView.setColor(color)
        binding.drawingView.setThickness(Constants.DEFAULT_PAINT_THICKNESS)
    }

    private fun subscribeToUiStateUpdates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedColorButtonId.collect { id ->
                    binding.colorGroup.check(id)
                    when (id) {
                        R.id.rbRed -> selectColor(getColor(R.color.red))
                        R.id.rbOrange -> selectColor(getColor(R.color.orange))
                        R.id.rbYellow -> selectColor(getColor(R.color.yellow))
                        R.id.rbGreen -> selectColor(getColor(R.color.green))
                        R.id.rbCyan -> selectColor(getColor(R.color.cyan))
                        R.id.rbBlue -> selectColor(getColor(R.color.blue))
                        R.id.rbPurple -> selectColor(getColor(R.color.purple))
                        R.id.rbPurple -> selectColor(getColor(R.color.black))
                        R.id.rbEraser -> {
                            binding.drawingView.setColor(getColor(R.color.white))
                            binding.drawingView.setThickness(40f)
                        }
                    }
                }
            }
        }
    }
}