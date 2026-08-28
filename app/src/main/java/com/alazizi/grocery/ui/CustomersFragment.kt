package com.alazizi.grocery.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alazizi.grocery.data.AppDatabase
import com.alazizi.grocery.data.Customer
import com.alazizi.grocery.databinding.FragmentCustomersBinding
import com.alazizi.grocery.databinding.ItemCustomerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CustomersFragment : Fragment() {
    private var _binding: FragmentCustomersBinding? = null
    private val b get() = _binding!!
    private lateinit var db: AppDatabase
    private val adapter = CustomerAdapter()
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentCustomersBinding.inflate(i, c, false); db = AppDatabase.get(requireContext())
        b.customersRecycler.layoutManager = LinearLayoutManager(requireContext()); b.customersRecycler.adapter = adapter
        b.addCustomerButton.setOnClickListener { showAddDialog() }
        lifecycleScope.launch { db.customers().observeAll().collectLatest { adapter.submit(it) } }
        return b.root
    }
    private fun showAddDialog() {
        val box = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 8, 32, 0) }
        val name = EditText(requireContext()).apply { hint = "اسم العميل" }
        val phone = EditText(requireContext()).apply { hint = "رقم الهاتف"; inputType = 3 }
        box.addView(name); box.addView(phone)
        AlertDialog.Builder(requireContext()).setTitle("إضافة عميل").setView(box).setNegativeButton("إلغاء", null).setPositiveButton("حفظ") { _, _ -> lifecycleScope.launch(Dispatchers.IO) { db.customers().insert(Customer(name = name.text.toString().trim(), phone = phone.text.toString().trim())) } }.show()
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
private class CustomerAdapter : RecyclerView.Adapter<CustomerAdapter.VH>() {
    private val items = mutableListOf<Customer>()
    fun submit(new: List<Customer>) { items.clear(); items.addAll(new); notifyDataSetChanged() }
    override fun onCreateViewHolder(p: ViewGroup, v: Int) = VH(ItemCustomerBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, p: Int) {
        val x = items[p]
        h.b.name.text = x.name
        h.b.phone.text = x.phone
        h.b.balance.text = String.format("الرصيد: %.2f", x.balance)
        h.itemView.setOnLongClickListener {
            AlertDialog.Builder(h.itemView.context)
                .setTitle(x.name)
                .setMessage("الهاتف: ${x.phone}\nالرصيد الحالي: ${x.balance}")
                .setPositiveButton("مشاركة") { _, _ ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "كشف حساب العميل ${x.name}\nالهاتف: ${x.phone}\nالرصيد: ${x.balance}")
                    }
                    h.itemView.context.startActivity(android.content.Intent.createChooser(intent, "مشاركة كشف الحساب"))
                }
                .setNegativeButton("إغلاق", null)
                .show()
            true
        }
    }
    class VH(val b: ItemCustomerBinding): RecyclerView.ViewHolder(b.root)
}
