package com.miumiu.gratic.ui.drawing

import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.miumiu.gratic.R
import com.miumiu.gratic.data.remote.ws.models.DrawAction
import com.miumiu.gratic.data.remote.ws.models.GameError
import com.miumiu.gratic.data.remote.ws.models.JoinRoomHandshake
import com.miumiu.gratic.databinding.ActivityDrawingBinding
import com.miumiu.gratic.ui.drawing.adapters.ChatMessageAdapter
import com.miumiu.gratic.util.Constants
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DrawingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding

    private val viewModel: DrawingViewModel by viewModels()
    private val args: DrawingActivityArgs by navArgs()

    @Inject
    lateinit var clientId: String

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var rvPlayers: RecyclerView

    private lateinit var chatMessageAdapter: ChatMessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        subscribeToUiStateUpdates()
        listenToConnectionEvents()
        listenToSocketEvents()
        setupRecyclerView()

        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        toggle.syncState()

        binding.drawingView.roomName = args.roomName

        val header = layoutInflater.inflate(R.layout.nav_drawer_header, binding.navView)
        rvPlayers = header.findViewById(R.id.rvPlayers)
        binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        binding.ibPlayers.setOnClickListener {
            binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            binding.root.openDrawer(GravityCompat.START)
        }

        binding.root.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) {
                binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }

            override fun onDrawerStateChanged(newState: Int) = Unit

        })

        binding.ibUndo.setOnClickListener {

            if (binding.drawingView.isUserDrawing) {
                binding.drawingView.undo()
                viewModel.sendBaseModel(DrawAction(DrawAction.ACTION_UNDO))
            }
        }

        binding.colorGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.checkRadioButton(checkedId)
        }

        binding.drawingView.setOnDrawListener {
            if (binding.drawingView.isUserDrawing) {
                viewModel.sendBaseModel(it)
            }
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connectionProgressBarVisible.collect { isVisible ->
                    binding.connectionProgressBar.isVisible = isVisible
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.chooseWordOverlayVisible.collect { isVisible ->
                    binding.chooseWordOverlay.isVisible = isVisible
                }
            }
        }
    }


    private fun listenToSocketEvents() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.socketEvent.collect { event ->
                when (event) {
                    is DrawingViewModel.SocketEvent.DrawDataEvent -> {
                        val drawData = event.data
                        if (!binding.drawingView.isUserDrawing) {
                            when (drawData.motionEvent) {
                                MotionEvent.ACTION_DOWN -> binding.drawingView.startedTouchExternally(
                                    drawData
                                )

                                MotionEvent.ACTION_MOVE -> binding.drawingView.movedTouchExternally(
                                    drawData
                                )

                                MotionEvent.ACTION_UP -> binding.drawingView.releaseTouchExternally(
                                    drawData
                                )
                            }
                        }
                    }

                    is DrawingViewModel.SocketEvent.UndoEvent -> {
                        binding.drawingView.undo()
                    }

                    is DrawingViewModel.SocketEvent.GameErrorEvent -> {
                        when (event.data.errorType) {
                            GameError.ERROR_ROOM_NOT_FOUND -> finish()
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun listenToConnectionEvents() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.connectionEvent.collect { event ->
                when (event) {
                    is WebSocket.Event.OnConnectionOpened<*> -> {
                        viewModel.sendBaseModel(
                            JoinRoomHandshake(
                                args.username, args.roomName, clientId
                            )
                        )
                        viewModel.setConnectionProgressBarVisibility(false)
                    }

                    is WebSocket.Event.OnConnectionFailed -> {
                        viewModel.setConnectionProgressBarVisibility(false)
                        Snackbar.make(
                            binding.root,
                            R.string.error_connection_failed,
                            Snackbar.LENGTH_LONG
                        ).show()
                        event.throwable.printStackTrace()
                    }

                    is WebSocket.Event.OnConnectionClosed -> {
                        viewModel.setConnectionProgressBarVisibility(false)
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun setupRecyclerView() = binding.rvChat.apply {
        chatMessageAdapter = ChatMessageAdapter(args.username)
        adapter = chatMessageAdapter
        layoutManager = LinearLayoutManager(this@DrawingActivity)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}