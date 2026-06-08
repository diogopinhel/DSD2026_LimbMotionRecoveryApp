package com.example.limbmotionrecoveryapp.screens.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.screens.auth.LoginActivity
import com.example.limbmotionrecoveryapp.screens.home.TutorialBookmarks
import com.example.limbmotionrecoveryapp.screens.profile.help.HelpActivity
import com.example.limbmotionrecoveryapp.screens.profile.privacy.PrivacyActivity
import com.example.limbmotionrecoveryapp.screens.profile.settings.SettingsActivity

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()
    private var rootView: View? = null

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            val token = prefs.getString("token", "") ?: ""
            val name = prefs.getString("userName", "") ?: ""
            val email = prefs.getString("userEmail", "") ?: ""
            viewModel.load(token, name, email)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false).also { rootView = it }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val cachedName = prefs.getString("userName", "") ?: ""
        val cachedEmail = prefs.getString("userEmail", "") ?: ""

        val tvInitials = view.findViewById<TextView>(R.id.tvInitials)
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = view.findViewById<TextView>(R.id.tvUserEmail)
        val tagsContainer = view.findViewById<LinearLayout>(R.id.tagsContainer)
        val tvStatRom = view.findViewById<TextView>(R.id.tvStatRom)
        val tvStatAdherence = view.findViewById<TextView>(R.id.tvStatAdherence)
        val tvStatStreak = view.findViewById<TextView>(R.id.tvStatStreak)

        view.findViewById<FrameLayout>(R.id.btnEdit).setOnClickListener {
            val currentData = viewModel.state.value
            val intent = Intent(requireContext(), EditProfileActivity::class.java).apply {
                putExtra("name", currentData?.name ?: cachedName)
                putExtra("email", currentData?.email ?: cachedEmail)
            }
            editProfileLauncher.launch(intent)
        }

        view.findViewById<LinearLayout>(R.id.itemMyRecovery).setOnClickListener {
            startActivity(Intent(requireContext(), MyRecoveryActivity::class.java))
        }
        view.findViewById<LinearLayout>(R.id.itemSavedVideos).setOnClickListener {
            startActivity(Intent(requireContext(), SavedVideosActivity::class.java))
        }
        view.findViewById<LinearLayout>(R.id.itemPainLog).setOnClickListener {
            startActivity(Intent(requireContext(), PainLogActivity::class.java))
        }
        view.findViewById<LinearLayout>(R.id.itemSettings).setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        view.findViewById<LinearLayout>(R.id.itemHelp).setOnClickListener {
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }
        view.findViewById<LinearLayout>(R.id.itemPrivacy).setOnClickListener {
            startActivity(Intent(requireContext(), PrivacyActivity::class.java))
        }

        view.findViewById<LinearLayout>(R.id.btnLogout).setOnClickListener {
            prefs.edit().clear().apply()
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }

        viewModel.state.observe(viewLifecycleOwner) { data ->
            tvUserName.text = data.name
            tvUserEmail.text = data.email
            tvInitials.text = initials(data.name)

            tagsContainer.removeAllViews()
            if (data.conditionLabel.isNotBlank()) {
                tagsContainer.addView(makeTag(data.conditionLabel))
            }
            if (data.conditionDate.isNotBlank()) {
                tagsContainer.addView(makeTag(formatDate(data.conditionDate)))
            }

            tvStatRom.text = if (data.currentRomDegrees > 0) "${data.currentRomDegrees}°" else "—"
            tvStatAdherence.text = if (data.adherencePercent > 0) "${data.adherencePercent}%" else "—"
            tvStatStreak.text = if (data.streakWeeks > 0) "${data.streakWeeks}wk" else "—"
        }

        if (token.isNotBlank()) {
            viewModel.load(token, cachedName, cachedEmail)
        } else {
            tvUserName.text = cachedName
            tvUserEmail.text = cachedEmail
            tvInitials.text = initials(cachedName)
        }
    }

    override fun onResume() {
        super.onResume()
        updateSavedBadge()
    }

    private fun updateSavedBadge() {
        val view = rootView ?: return
        val badge = view.findViewById<TextView>(R.id.tvSavedBadge)
        val count = TutorialBookmarks.getAll(requireContext()).size
        if (count > 0) {
            badge.text = count.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }

    private fun initials(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0][0]}${parts[1][0]}".uppercase()
            parts.size == 1 && parts[0].isNotEmpty() -> parts[0][0].uppercaseChar().toString()
            else -> "?"
        }
    }

    private fun formatDate(iso: String): String {
        return try {
            val parts = iso.split("-")
            if (parts.size == 3) {
                val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                val m = parts[1].toInt() - 1
                "${months.getOrElse(m) { parts[1] }} ${parts[2]}, ${parts[0]}"
            } else iso
        } catch (_: Exception) { iso }
    }

    private fun makeTag(text: String): TextView {
        val dp = resources.displayMetrics.density
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 11f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding((10 * dp).toInt(), (3 * dp).toInt(), (10 * dp).toInt(), (3 * dp).toInt())
            background = androidx.core.content.ContextCompat.getDrawable(
                requireContext(), R.drawable.bg_user_tag
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (6 * dp).toInt() }
        }
    }
}
