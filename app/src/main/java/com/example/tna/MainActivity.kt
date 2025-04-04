package com.example.tna

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Video Background
        val videoView = findViewById<VideoView>(R.id.videoBackground)
        val videoPath = "android.resource://$packageName/${R.raw.bg}"
        videoView.setVideoURI(Uri.parse(videoPath))

        // Loop the video playback
        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true
            mediaPlayer.setVolume(0f, 0f) // Mute the video
        }
        videoView.start()

        // Redirect to LoginActivity on button click
        val getStartedButton = findViewById<Button>(R.id.getStartedButton)
        getStartedButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java)) // Fixed class name
        }
    }
}
