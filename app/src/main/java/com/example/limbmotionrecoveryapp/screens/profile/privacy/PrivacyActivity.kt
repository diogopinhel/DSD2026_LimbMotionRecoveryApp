package com.example.limbmotionrecoveryapp.screens.profile.privacy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.R

class PrivacyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        // Back button
        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }

        // View Full Privacy Policy button
        findViewById<Button>(R.id.btn_view_full_policy).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://limbmotion.com/privacy"))
            startActivity(intent)
        }
    }
}
