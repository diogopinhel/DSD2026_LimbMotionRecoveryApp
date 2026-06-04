package com.example.limbmotionrecoveryapp.screens.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import com.example.limbmotionrecoveryapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SensorJointMappingFragment : BottomSheetDialogFragment() {

    // Callback to return the mapping when confirmed
    var onMappingConfirmed: ((Map<String, String>) -> Unit)? = null

    // Sensor IDs passed in from outside
    private var sensor1Id: String = "AA:BB:CC:DD"
    private var sensor2Id: String = "EE:FF:GG:HH"

    companion object {
        private const val ARG_SENSOR1 = "sensor1"
        private const val ARG_SENSOR2 = "sensor2"

        fun newInstance(sensor1: String, sensor2: String): SensorJointMappingFragment {
            val fragment = SensorJointMappingFragment()
            val args = Bundle()
            args.putString(ARG_SENSOR1, sensor1)
            args.putString(ARG_SENSOR2, sensor2)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensor1Id = arguments?.getString(ARG_SENSOR1) ?: "AA:BB:CC:DD"
        sensor2Id = arguments?.getString(ARG_SENSOR2) ?: "EE:FF:GG:HH"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sensor_joint_mapping, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set sensor IDs
        view.findViewById<TextView>(R.id.tvSensor1Id).text = "Sensor: $sensor1Id"
        view.findViewById<TextView>(R.id.tvSensor2Id).text = "Sensor: $sensor2Id"

        // Joint options
        val jointOptions = listOf("knee", "hip", "ankle", "shoulder", "elbow", "wrist")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            jointOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spinner1 = view.findViewById<Spinner>(R.id.spinnerSensor1Joint)
        val spinner2 = view.findViewById<Spinner>(R.id.spinnerSensor2Joint)
        spinner1.adapter = adapter
        spinner2.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            jointOptions
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Confirm button
        view.findViewById<Button>(R.id.btnConfirmMapping).setOnClickListener {
            val mapping = mapOf(
                sensor1Id to spinner1.selectedItem.toString(),
                sensor2Id to spinner2.selectedItem.toString()
            )
            onMappingConfirmed?.invoke(mapping)
            dismiss()
        }

        // Cancel
        view.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            dismiss()
        }
    }
}