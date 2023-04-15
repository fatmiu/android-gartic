package com.miumiu.gratic.ui.drawing

import android.graphics.Color
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
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.miumiu.data.models.BaseModel
import com.miumiu.gratic.R
import com.miumiu.gratic.data.remote.ws.Room
import com.miumiu.gratic.data.remote.ws.models.ChatMessage
import com.miumiu.gratic.data.remote.ws.models.DrawAction
import com.miumiu.gratic.data.remote.ws.models.GameError
import com.miumiu.gratic.data.remote.ws.models.JoinRoomHandshake
import com.miumiu.gratic.data.remote.ws.models.PlayerData
import com.miumiu.gratic.databinding.ActivityDrawingBinding
import com.miumiu.gratic.ui.dialogs.LeaveDialog
import com.miumiu.gratic.ui.drawing.adapters.ChatMessageAdapter
import com.miumiu.gratic.ui.drawing.adapters.PlayerAdapter
import com.miumiu.gratic.util.Constants
import com.miumiu.gratic.util.hideKeyboard
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DrawingActivity : AppCompatActivity(), LifecycleEventObserver {

    private lateinit var binding: ActivityDrawingBinding

    private val viewModel: DrawingViewModel by viewModels()
    private val args: DrawingActivityArgs by navArgs()

    @Inject
    lateinit var clientId: String

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var rvPlayers: RecyclerView

    @Inject
    lateinit var playerAdapter: PlayerAdapter

    private lateinit var chatMessageAdapter: ChatMessageAdapter

    private var updateChatJob: Job? = null
    private var updatePlayersJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycle.addObserver(this)
        subscribeToUiStateUpdates()
        listenToConnectionEvents()
        listenToSocketEvents()
        setupRecyclerView()
//        registerOnBackPressed()

        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        toggle.syncState()

        binding.drawingView.roomName = args.roomName

        chatMessageAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        val header = layoutInflater.inflate(R.layout.nav_drawer_header, binding.navView)
        rvPlayers = header.findViewById(R.id.rvPlayers)
        binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        rvPlayers.apply {
            adapter = playerAdapter
            layoutManager = LinearLayoutManager(this@DrawingActivity)
        }

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

        binding.ibSend.setOnClickListener {
            viewModel.sendChatMessage(
                ChatMessage(
                    args.username,
                    args.roomName,
                    binding.etMessage.text.toString(),
                    System.currentTimeMillis()
                )
            )
            binding.etMessage.text?.clear()
            hideKeyboard(binding.root)
        }

        binding.drawingView.setPathDataChangedListener {
            viewModel.setPathDate(it)
        }

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

    private fun setColorGroupVisibility(isVisible: Boolean) {
        binding.colorGroup.isVisible = isVisible
        binding.ibUndo.isVisible = isVisible
    }

    private fun setMessageInputVisibility(isVisible: Boolean) {
        binding.apply {
            tilMessage.isVisible = isVisible
            ibSend.isVisible = isVisible
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pathData.collect { pathData ->
                    binding.drawingView.setPaths(pathData)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.chat.collect { chat ->
                    if (chatMessageAdapter.chatObjects.isEmpty()) {
                        updateChatMessageList(chat)
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.newWords.collect {
                    val newWords = it.newWords
                    if (newWords.isEmpty()) return@collect

                    binding.apply {
                        btnFirstWord.text = newWords[0]
                        btnSecondWord.text = newWords[1]
                        btnThirdWord.text = newWords[2]

                        btnFirstWord.setOnClickListener {
                            viewModel.chooseWord(newWords[0], args.roomName)
                            viewModel.setChooseWordOverlayVisibility(false)
                        }
                        btnSecondWord.setOnClickListener {
                            viewModel.chooseWord(newWords[1], args.roomName)
                            viewModel.setChooseWordOverlayVisibility(false)
                        }
                        btnThirdWord.setOnClickListener {
                            viewModel.chooseWord(newWords[2], args.roomName)
                            viewModel.setChooseWordOverlayVisibility(false)
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.gameState.collect { gameState ->
                    binding.apply {
                        tvCurWord.text = gameState.word
                        val isUserDrawing = gameState.drawingPlayer == args.username
                        setColorGroupVisibility(isUserDrawing)
                        setMessageInputVisibility(!isUserDrawing)
                        ibUndo.isEnabled = isUserDrawing
                        drawingView.isUserDrawing = isUserDrawing
                        ibMic.isVisible = !isUserDrawing
                        drawingView.isEnabled = isUserDrawing
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.players.collect { players ->
                    updatePlayersList(players)

                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.phaseTime.collect { time ->
                    binding.roundTimerProgressBar.progress = time.toInt()
                    binding.tvRemainingTimeChooseWord.text = (time / 1000L).toString()

                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.phase.collect { phase ->
                    when (phase.phase) {
                        Room.Phase.WAITING_FOR_PLAYERS -> {
                            binding.tvCurWord.text = getString(R.string.waiting_for_players)
                            viewModel.cancelTimer()
                            viewModel.setConnectionProgressBarVisibility(false)
                            binding.roundTimerProgressBar.progress =
                                binding.roundTimerProgressBar.max
                        }

                        Room.Phase.WAITING_FOR_START -> {
                            binding.roundTimerProgressBar.max = phase.time.toInt()
                            binding.tvCurWord.text = getString(R.string.waiting_for_start)
                        }

                        Room.Phase.NEW_ROUND -> {
                            phase.drawingPlayer?.let { player ->
                                binding.tvCurWord.text =
                                    getString(R.string.player_is_drawing, player)
                            }
                            binding.apply {
                                drawingView.isEnabled = false
                                drawingView.setColor(Color.BLACK)
                                drawingView.setThickness(Constants.DEFAULT_PAINT_THICKNESS)
                                roundTimerProgressBar.max = phase.time.toInt()
                                val isUserDrawingPlayer = phase.drawingPlayer == args.username
                                binding.chooseWordOverlay.isVisible = isUserDrawingPlayer
                            }
                        }

                        Room.Phase.GAME_RUNNING -> {
                            binding.chooseWordOverlay.isVisible = false
                            binding.roundTimerProgressBar.max = phase.time.toInt()
                        }

                        Room.Phase.SHOW_WORD -> {
                            binding.apply {
                                if (drawingView.isDrawing) {
                                    drawingView.finishOffDrawing()
                                }
                                drawingView.isEnabled = false
                                drawingView.setColor(Color.BLACK)
                                drawingView.setThickness(Constants.DEFAULT_PAINT_THICKNESS)
                                roundTimerProgressBar.max = phase.time.toInt()
                            }
                        }

                        else -> Unit
                    }
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

                                MotionEvent.ACTION_UP -> binding.drawingView.releasedTouchExternally(
                                    drawData
                                )
                            }
                        }
                    }

                    is DrawingViewModel.SocketEvent.RoundDrawInfoEvent -> {
                        binding.drawingView.update(event.data)
                    }

                    is DrawingViewModel.SocketEvent.GameStateEvent -> {
                        binding.drawingView.clear()
                    }

                    is DrawingViewModel.SocketEvent.ChatMessageEvent -> {
                        addChatObjectToRecyclerView(event.data)
                    }

                    is DrawingViewModel.SocketEvent.ChosenWordEvent -> {
                        binding.tvCurWord.text = event.data.chosenWord
                        binding.ibUndo.isEnabled = false
                    }

                    is DrawingViewModel.SocketEvent.AnnouncementEvent -> {
                        addChatObjectToRecyclerView(event.data)
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

    override fun onPause() {
        super.onPause()
        binding.rvChat.layoutManager?.onSaveInstanceState()
    }

    private fun updatePlayersList(players: List<PlayerData>) {
        updatePlayersJob?.cancel()
        updatePlayersJob = lifecycleScope.launch {
            playerAdapter.updateDataset(players)
        }
    }

    private fun updateChatMessageList(chat: List<BaseModel>) {
        updateChatJob?.cancel()
        updateChatJob = lifecycleScope.launch {
            chatMessageAdapter.updateDataset(chat)
        }
    }

    private suspend fun addChatObjectToRecyclerView(chatObject: BaseModel) {
        val canScrollDown = binding.rvChat.canScrollVertically(1)
        updateChatMessageList(chatMessageAdapter.chatObjects + chatObject)
        updateChatJob?.join()
        if (!canScrollDown) {
            binding.rvChat.scrollToPosition(chatMessageAdapter.chatObjects.size - 1)
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

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_STOP) {
            viewModel.disconnect()
        }
    }

    override fun onBackPressed() {
        LeaveDialog().apply {
            setPositiveClickListener {
                viewModel.disconnect()
                finish()
            }
        }.show(supportFragmentManager, null)
    }
}