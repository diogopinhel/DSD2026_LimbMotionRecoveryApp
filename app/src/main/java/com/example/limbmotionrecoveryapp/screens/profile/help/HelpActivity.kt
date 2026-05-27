package com.example.limbmotionrecoveryapp.screens.profile.help

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.R

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        // Back button
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        // Contact Support button
        findViewById<Button>(R.id.btn_contact_support).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@limbmotion.com")
                putExtra(Intent.EXTRA_SUBJECT, "App Support Request")
            }
            startActivity(intent)
        }
    }
}
