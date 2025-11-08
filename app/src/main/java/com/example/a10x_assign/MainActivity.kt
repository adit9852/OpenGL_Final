package com.example.a10x_assign

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.a10x_assign.ui.roomviewer.RoomViewerFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, RoomViewerFragment())
                .commit()
        }
    }
}