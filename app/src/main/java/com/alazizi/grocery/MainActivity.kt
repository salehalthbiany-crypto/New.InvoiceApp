package com.alazizi.grocery

import android.Manifest
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.alazizi.grocery.databinding.ActivityMainBinding
import com.alazizi.grocery.print.EscPosPrinter
import com.alazizi.grocery.ui.CustomersFragment
import com.alazizi.grocery.ui.HistoryFragment
import com.alazizi.grocery.ui.InvoiceFragment

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private var pendingPrint: Pair<List<String>, Double>? = null
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val ok = result.values.all { it }
        if (ok) pendingPrint?.let { showPrinterPicker(it.first, it.second) } else Toast.makeText(this, "يجب السماح بصلاحية Bluetooth للطباعة", Toast.LENGTH_LONG).show()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater); setContentView(b.root)
        b.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                com.alazizi.grocery.R.id.nav_invoice -> supportFragmentManager.commit { replace(R.id.container, InvoiceFragment()) }
                com.alazizi.grocery.R.id.nav_history -> supportFragmentManager.commit { replace(R.id.container, HistoryFragment()) }
                com.alazizi.grocery.R.id.nav_customers -> supportFragmentManager.commit { replace(R.id.container, CustomersFragment()) }
            }; true
        }
        b.bottomNav.selectedItemId = R.id.nav_invoice
    }
    fun printCurrentInvoice(lines: List<Triple<String, Double, Double>>, total: Double) {
        pendingPrint = Pair(lines.map { "${it.first} | ${it.second} × ${it.third} = ${it.second * it.third}" } + "الإجمالي: $total", total)
        val permissions = if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN) else emptyArray()
        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray()) else pendingPrint?.let { showPrinterPicker(it.first, it.second) }
    }
    private fun showPrinterPicker(lines: List<String>, total: Double) {
        val printer = EscPosPrinter(this); val devices = printer.pairedDevices()
        if (devices.isEmpty()) { Toast.makeText(this, "لا توجد طابعات Bluetooth مقترنة. اربط الطابعة من إعدادات Bluetooth أولاً.", Toast.LENGTH_LONG).show(); return }
        val labels = devices.map { if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) "Bluetooth device" else "${it.name ?: "بدون اسم"}\n${it.address}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("اختيار الطابعة").setItems(labels) { _, which ->
            Thread { try { printer.print(devices[which], lines); runOnUiThread { Toast.makeText(this, "تم إرسال الفاتورة للطابعة", Toast.LENGTH_SHORT).show() } } catch (e: Exception) { runOnUiThread { Toast.makeText(this, "فشل الطباعة: ${e.message}", Toast.LENGTH_LONG).show() } } }.start()
        }.setNegativeButton("إلغاء", null).show()
    }
}
