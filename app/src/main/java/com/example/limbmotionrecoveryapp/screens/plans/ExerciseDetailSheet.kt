package com.example.limbmotionrecoveryapp.screens.plans

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.limbmotionrecoveryapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class ExerciseDetailSheet : BottomSheetDialogFragment() {

    private lateinit var exercise: Exercise
    var onMarkDone: ((exerciseId: Int, scheduleId: Int) -> Unit)? = null

    companion object {
        fun newInstance(exercise: Exercise): ExerciseDetailSheet {
            return ExerciseDetailSheet().also { it.exercise = exercise }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.bottom_sheet_exercise, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tvSheetName).text = exercise.name
        view.findViewById<TextView>(R.id.tvSheetPhase).text = exercise.phase
        view.findViewById<TextView>(R.id.tvSheetSets).text = exercise.sets.toString()
        view.findViewById<TextView>(R.id.tvSheetReps).text = exercise.reps.toString()

        val layoutHold = view.findViewById<LinearLayout>(R.id.layoutSheetHold)
        if (exercise.holdSeconds > 0) {
            layoutHold.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvSheetHold).text = exercise.holdSeconds.toString()
        }

        val layoutNotes = view.findViewById<LinearLayout>(R.id.layoutSheetNotes)
        if (!exercise.notes.isNullOrBlank()) {
            layoutNotes.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvSheetNotes).text = exercise.notes
        }

        // GIF demo — shown inline when available, button hidden when GIF is loaded
        val ivGif = view.findViewById<ImageView>(R.id.ivExGif)
        val btnVideo = view.findViewById<MaterialButton>(R.id.btnWatchVideo)
        if (!exercise.gifUrl.isNullOrBlank()) {
            ivGif.visibility = View.VISIBLE
            btnVideo.visibility = View.GONE
            Glide.with(this)
                .asGif()
                .load(exercise.gifUrl)
                .centerCrop()
                .placeholder(android.R.color.darker_gray)
                .into(ivGif)
        }

        // Description from catalog
        val layoutDesc = view.findViewById<LinearLayout>(R.id.layoutSheetDescription)
        if (!exercise.description.isNullOrBlank()) {
            layoutDesc.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvSheetDescription).text = exercise.description
        }

        val btnDone = view.findViewById<MaterialButton>(R.id.btnMarkDone)
        val tvDone = view.findViewById<TextView>(R.id.tvAlreadyDone)
        if (exercise.completed) {
            tvDone.visibility = View.VISIBLE
        } else {
            btnDone.visibility = View.VISIBLE
            btnDone.setOnClickListener {
                onMarkDone?.invoke(exercise.id, exercise.scheduleId)
                dismiss()
            }
        }
    }
}
