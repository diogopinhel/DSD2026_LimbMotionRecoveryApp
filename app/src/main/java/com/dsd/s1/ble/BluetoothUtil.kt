package com.dsd.s1.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context

/**
 * Stub implementation of BluetoothUtil for the dpinhel branch.
 */
object BluetoothUtil {
    fun isBluetoothEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        return manager?.adapter?.isEnabled == true
    }

    fun getBluetoothAdapter(context: Context): android.bluetooth.BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        return manager?.adapter
    }

    fun hasBluetoothPermissions(context: Context): Boolean {
        val pm = context.packageManager
        val hasBle = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE)
        val perm1 = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.BLUETOOTH_SCAN
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val perm2 = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return hasBle && (perm1 || perm2)
    }
}