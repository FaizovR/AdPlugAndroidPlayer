package com.example.adplugplayer

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adplugplayer.adapters.ListSelectionRecyclerViewAdapter
import com.example.adplugplayer.util.FileManager
import com.omicronapplications.andpluglib.IAndPlugCallback
import com.omicronapplications.andpluglib.IPlayer
import com.omicronapplications.andpluglib.IPlayer.PlayerState
import com.omicronapplications.andpluglib.PlayerController
import java.io.File

class MainActivity : AppCompatActivity(), IAndPlugCallback, ListSelectionRecyclerViewAdapter.OnListListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private val TAG1 = "MainActivity"
    private val PERMISSIONS: Array<String> = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    private val REQUEST_PERMISSIONS: Int = 12345
    private val PERMISSION_COUNT:Int = 1

    @SuppressLint("NewApi")
    private fun arePermissionDenied(): Boolean {
        Log.d(TAG1, "arePermissionDenied: ")
        for (i in 0 until  PERMISSION_COUNT) {
            if (checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                return true
            }
        }
        return false
    }

    @SuppressLint("NewApi")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG1, "onRequestPermissionsResult: ")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (arePermissionDenied()) {
            (this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
        } else {
            onResume()
        }
    }

    private lateinit var mSkipPreviousButton: Button
    private lateinit var mPauseButton: Button
    private lateinit var mPlayButton: Button
    private lateinit var mStopButton: Button
    private lateinit var mSkipNextButton: Button

    private lateinit var mCurrentDirectory: TextView
    private lateinit var mCurrentlyPlayingTextView: TextView

    private lateinit var listsRecyclerView: RecyclerView

    private val TAG = "PlayerActivity"
    private var mController: PlayerController? = null
    var mPlayer: IPlayer? = null

    private var mIndex = -1

    private var fileManager: FileManager? = null
    private val songs: MutableList<String> = mutableListOf()
    private var mLoadedSong: File? = null

    private lateinit var sharedPreferences: SharedPreferences

    init {
        System.loadLibrary("andplug")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        mController = PlayerController(this, applicationContext)
        mController?.create()

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        mSkipPreviousButton = findViewById(R.id.btn_skip_previous)
        mPauseButton = findViewById(R.id.btn_pause)
        mPlayButton = findViewById(R.id.btn_play)
        mStopButton = findViewById(R.id.btn_stop)
        mSkipNextButton = findViewById(R.id.btn_skip_next)

        mCurrentlyPlayingTextView = findViewById(R.id.tv_current_playing)

        fileManager = FileManager(
            applicationContext,
            FileManager.STORAGE_EXTERNAL_1
        )

        val dirs = fileManager!!.listFiles()
        var id = 0
        for ( file in dirs) {
            if (!file.name.toString().contains(".raw")) {
                songs.add(id, file.name)
                id++
            }
        }

        mCurrentDirectory = findViewById(R.id.tv_directory)
        mCurrentDirectory.text = filesDir.name

        listsRecyclerView = findViewById(R.id.lists_recyclerview)

        listsRecyclerView.layoutManager = LinearLayoutManager(this)
        listsRecyclerView.adapter =
            ListSelectionRecyclerViewAdapter(
                songs, this
            )

        mSkipPreviousButton.setOnClickListener {
            mIndex -= 1
            if (mIndex < 0) {
                mIndex = songs.size - 1
            }
            val fileName: String = songs[mIndex]
            val file: File? = fileManager?.getFile(fileName)
            mPlayer = mController?.service
            mPlayer?.load(file?.absolutePath)
            mLoadedSong = file
            mPlayer?.play()
        }

        mPauseButton.setOnClickListener {
            mPlayer = mController?.service
        }

        mStopButton.setOnClickListener {
            mPlayer = mController?.service
            mPlayer?.stop()
        }

        mPlayButton.setOnClickListener {
            mPlayer = mController?.service
            if (mPlayer?.state == PlayerState.PLAYING) {
                mPlayer?.pause()
            } else if (mPlayer?.state == PlayerState.PAUSED) {
                mPlayer?.play()
            }
        }

        mSkipNextButton.setOnClickListener {
            mIndex += 1
            if (mIndex >= songs.size) {
                mIndex = 0
            }
            val fileName = songs[mIndex]
            val file = fileManager?.getFile(fileName)
            mPlayer = mController?.service
            mPlayer?.load(file?.absolutePath)
            mLoadedSong = file
            mPlayer?.play()
        }
    }

    override fun onDestroy() {
        if (mController != null) {
            mController!!.destroy()
            mController = null
        }
        super.onDestroy()
    }

    override fun onResume() {
        Log.d(TAG1, "onResume: ")
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS)
            return
        }
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent: Intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNewState(
        request: IPlayer.PlayerRequest?,
        state: IPlayer.PlayerState?,
        info: String?
    ) {
        val title = mPlayer?.title
        val author = mPlayer?.author
        val desc = mPlayer?.desc
        when (state) {
            PlayerState.DEFAULT -> {

            }
            PlayerState.CREATED -> {

            }
            PlayerState.LOADED -> {
                mCurrentlyPlayingTextView.text = mPlayer!!.song
            }
            PlayerState.PLAYING -> {
                mPlayButton.setText(R.string.Pause)
            }
            PlayerState.PAUSED -> {
                mPlayButton.setText(R.string.Play)
            }
            PlayerState.STOPPED -> {
                mPlayButton.setText(R.string.Play)
            }
            PlayerState.ENDED -> {
                mPlayButton.setText(R.string.Play)
            }
            PlayerState.ERROR -> {
                Toast.makeText(this, "Failed to load song!", Toast.LENGTH_SHORT).show()
                mCurrentlyPlayingTextView.text = ""
            }
            PlayerState.FATAL -> {

            }
        }
    }

    override fun onServiceDisconnected() {
        Log.d(TAG, "onServiceDisconnected: ")
        if (mPlayer != null) {
            mPlayer?.stop()
            mPlayer?.unload()
            mPlayer?.uninitialize()
            mPlayer = null
        }
    }

    override fun onServiceConnected() {
        Log.d(TAG, "onServiceConnected: ")
        mPlayer = mController!!.service
        if (mPlayer != null) {
            Log.d(TAG, "onServiceConnected: init")
            Log.d(TAG, "onServiceConnected:")
            mPlayer!!.initialize(
                sharedPreferences.getString("sample_rate", "48000")!!.toInt(),
                sharedPreferences.getString("sample_rate", "48000")!!.toInt() == 16,
                sharedPreferences.getBoolean("channels", true),
                sharedPreferences.getString("mix", "LR")!!.contains("L"),
                sharedPreferences.getString("mix", "LR")!!.contains("R"),
                sharedPreferences.getString("buffers", "1")!!.toInt(),
                sharedPreferences.getString("samples", "262144")!!.toInt())
            mPlayer!!.repeat = true
        }
    }

    override fun onListClick(position: Int) {
        mPlayer = mController!!.service
        if (mPlayer != null) {
            mIndex = position
            val fileName = songs[mIndex]
            val file = fileManager?.getFile(fileName)
            mPlayer = mController?.service
            mPlayer?.load(file?.absolutePath)
            mLoadedSong = file
            mPlayer?.play()
        }
    }

    private fun reload() {
        if (mPlayer != null) {
            mPlayer!!.uninitialize()
            Log.d(TAG, "Playerinit: ")
            mPlayer!!.initialize(
                sharedPreferences.getString("sample_rate", "48000")!!.toInt(),
                sharedPreferences.getString("sample_rate", "48000")!!.toInt() == 16,
                sharedPreferences.getBoolean("channels", true),
                sharedPreferences.getString("mix", "LR")!!.contains("L"),
                sharedPreferences.getString("mix", "LR")!!.contains("R"),
                sharedPreferences.getString("buffers", "1")!!.toInt(),
                sharedPreferences.getString("samples", "262144")!!.toInt())
            mPlayer!!.repeat = true
            if (mLoadedSong != null) {
                mPlayer?.load(mLoadedSong!!.absolutePath)
                mPlayer?.play()
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences1: SharedPreferences?, key: String?) {
        mPlayer = mController!!.service
        Log.d(TAG, "onSharedPreferenceChanged: ")
        reload()
    }
}