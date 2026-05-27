package com.example.limbmotionrecoveryapp.screens.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.screens.auth.LoginActivity
import com.example.limbmotionrecoveryapp.screens.profile.help.HelpActivity
import com.example.limbmotionrecoveryapp.screens.profile.privacy.PrivacyActivity
import com.example.limbmotionrecoveryapp.screens.profile.settings.SettingsActivity

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val cachedName = prefs.getString("userName", "") ?: ""
        val cachedEmail = prefs.getString("userEmail", "") ?: ""

        // Wire up row clicks using actual XML IDs
        view.findViewById<LinearLayout>(R.id.row_settings)?.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        view.findViewById<LinearLayout>(R.id.row_help)?.setOnClickListener {
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }
        view.findViewById<LinearLayout>(R.id.row_privacy)?.setOnClickListener {
            startActivity(Intent(requireContext(), PrivacyActivity::class.java))
        }
        view.findViewById<LinearLayout>(R.id.row_recovery)?.setOnClickListener {
            Toast.makeText(requireContext(), "My Recovery — Coming soon", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.row_appointments)?.setOnClickListener {
            Toast.makeText(requireContext(), "Appointments — Coming soon", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.row_medications)?.setOnClickListener {
            Toast.makeText(requireContext(), "Medications — Coming soon", Toast.LENGTH_SHORT).show()
        }

        // TODO: add logout button to fragment_profile.xml if needed

        if (token.isNotBlank()) {
            viewModel.load(token, cachedName, cachedEmail)
        }
    }
}
