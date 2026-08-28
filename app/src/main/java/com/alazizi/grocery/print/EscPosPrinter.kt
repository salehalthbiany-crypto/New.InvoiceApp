package com.alazizi.grocery.print

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.OutputStream
import java.util.UUID

class EscPosPrinter(private val context: Context) {
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    fun pairedDevices(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (android.os.Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return emptyList()
        return adapter.bondedDevices?.toList().orEmpty()
    }
    fun print(device: BluetoothDevice, lines: List<String>) {
        if (android.os.Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) throw SecurityException("BLUETOOTH_CONNECT permission required")
        val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
        socket.use { s -> s.connect(); s.outputStream.use { out -> send(out, lines) } }
    }
    private fun send(out: OutputStream, lines: List<String>) {
        out.write(byteArrayOf(0x1B, 0x40))
        out.write("بقالة العزي للمواد الغذائية\n".toByteArray(Charsets.UTF_8))
        lines.forEach { out.write((it + "\n").toByteArray(Charsets.UTF_8)) }
        out.write(byteArrayOf(0x1D, 0x56, 0x00))
        out.flush()
    }
}
