package com.alazizi.grocery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alazizi.grocery.data.AppDatabase
import com.alazizi.grocery.data.Invoice
import com.alazizi.grocery.databinding.FragmentHistoryBinding
import com.alazizi.grocery.databinding.ItemInvoiceHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val b get() = _binding!!
    private lateinit var db: AppDatabase
    private val adapter = HistoryAdapter()
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(i, c, false)
        db = AppDatabase.get(requireContext())
        b.historyRecycler.layoutManager = LinearLayoutManager(requireContext()); b.historyRecycler.adapter = adapter
        lifecycleScope.launch { db.invoices().observeAll().collectLatest { adapter.submit(it) } }
        return b.root
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

private class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.VH>() {
    private val items = mutableListOf<Invoice>()
    private val fmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    fun submit(new: List<Invoice>) { items.clear(); items.addAll(new); notifyDataSetChanged() }
    override fun onCreateViewHolder(p: ViewGroup, v: Int) = VH(ItemInvoiceHistoryBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, p: Int) { val x = items[p]; h.b.title.text = "فاتورة #${x.id}"; h.b.date.text = fmt.format(Date(x.createdAt)); h.b.total.text = String.format("الإجمالي: %.2f", x.total); h.b.preview.text = "العميل: ${x.customerName.ifBlank { "نقدي" }}" }
    class VH(val b: ItemInvoiceHistoryBinding): RecyclerView.ViewHolder(b.root)
}
