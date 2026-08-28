package com.alazizi.grocery.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alazizi.grocery.data.*
import com.alazizi.grocery.databinding.FragmentInvoiceBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InvoiceFragment : Fragment() {
    private var _binding: FragmentInvoiceBinding? = null
    private val b get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var adapter: InvoiceLineAdapter
    private var customers = emptyList<Customer>()
    private var lastSavedId = 0L

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentInvoiceBinding.inflate(i, c, false)
        db = AppDatabase.get(requireContext())
        setup()
        return b.root
    }

    private fun setup() {
        adapter = InvoiceLineAdapter(requireContext(), db.products(), ::updateTotal) { pos -> adapter.removeRow(pos) }
        b.itemsRecycler.layoutManager = LinearLayoutManager(requireContext())
        b.itemsRecycler.adapter = adapter
        b.addRowButton.setOnClickListener { adapter.addRow(); b.itemsRecycler.scrollToPosition(adapter.itemCount - 1) }
        b.newButton.setOnClickListener { newInvoice() }
        b.saveButton.setOnClickListener { saveInvoice() }
        b.shareButton.setOnClickListener { shareInvoice() }
        b.printButton.setOnClickListener { (activity as? com.alazizi.grocery.MainActivity)?.printCurrentInvoice(buildLines(), currentTotal()) }
        lifecycleScope.launch { db.customers().observeAll().collectLatest { customers = it; b.customerField.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, it.map(Customer::name))) } }
        lifecycleScope.launch { db.products().observeAll().collectLatest { /* database-backed suggestions are queried per row; no RecyclerView refresh here */ } }
        updateTotal()
    }

    private fun buildLines() = adapter.rows.filter { it.name.isNotBlank() && it.qty > 0 }.map { Triple(it.name, it.qty, it.price) }
    private fun currentTotal() = buildLines().sumOf { it.second * it.third }
    private fun updateTotal() { b.totalText.text = String.format("الإجمالي: %.2f", currentTotal()) }

    private fun saveInvoice() {
        val lines = buildLines()
        if (lines.isEmpty()) { Toast.makeText(requireContext(), "أضف صنفاً واحداً على الأقل", Toast.LENGTH_SHORT).show(); return }
        lifecycleScope.launch(Dispatchers.IO) {
            val invoice = Invoice(customerName = b.customerField.text.toString().trim(), createdAt = System.currentTimeMillis(), total = lines.sumOf { it.second * it.third })
            val id = db.invoices().insert(invoice)
            db.invoices().insertItems(lines.map { InvoiceItem(invoiceId = id, productName = it.first, quantity = it.second, unitPrice = it.third, lineTotal = it.second * it.third) })
            lines.forEach { db.products().insert(Product(name = it.first, lastPrice = it.third)) }
            withContext(Dispatchers.Main) { lastSavedId = id; Toast.makeText(requireContext(), "تم حفظ الفاتورة #$id", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun shareInvoice() {
        val lines = buildLines()
        val text = buildString {
            append("بقالة العزي للمواد الغذائية\nفاتورة\n")
            b.customerField.text?.takeIf { it.isNotBlank() }?.let { append("العميل: $it\n") }
            lines.forEach { append("${it.first} — ${it.second} × ${it.third} = ${it.second * it.third}\n") }
            append("الإجمالي: ${currentTotal()}")
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "مشاركة الفاتورة"))
    }

    private fun newInvoice() { adapter.reset(); b.customerField.setText(""); lastSavedId = 0L; updateTotal() }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
