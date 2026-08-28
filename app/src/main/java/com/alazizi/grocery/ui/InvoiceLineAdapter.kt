package com.alazizi.grocery.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alazizi.grocery.data.ProductDao
import com.alazizi.grocery.databinding.ItemInvoiceLineBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InvoiceLineAdapter(
    private val context: Context,
    private val productDao: ProductDao,
    private val onChanged: () -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<InvoiceLineAdapter.VH>() {
    data class Row(var name: String = "", var qty: Double = 1.0, var price: Double = 0.0)
    val rows = mutableListOf(Row())
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    init { setHasStableIds(true) }
    override fun getItemId(position: Int): Long = position.toLong() + 1L
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemInvoiceLineBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(position)

    inner class VH(private val b: ItemInvoiceLineBinding) : RecyclerView.ViewHolder(b.root) {
        private var bound = RecyclerView.NO_POSITION
        private var bindingNow = false
        private var namesAdapter: ArrayAdapter<String>? = null

        init {
            b.nameField.addTextChangedListener(SimpleTextWatcher { text ->
                val p = bound
                if (bindingNow || p !in rows.indices) return@SimpleTextWatcher
                rows[p].name = text
                scope.launch {
                    val result = withContext(Dispatchers.IO) { productDao.suggest(text.trim()) }
                    namesAdapter?.run { clear(); addAll(result) }
                }
                onChanged()
            })
            b.qtyField.addTextChangedListener(SimpleTextWatcher { text ->
                val p = bound
                if (bindingNow || p !in rows.indices) return@SimpleTextWatcher
                rows[p].qty = text.toDoubleOrNull() ?: 0.0
                recalc()
            })
            b.priceField.addTextChangedListener(SimpleTextWatcher { text ->
                val p = bound
                if (bindingNow || p !in rows.indices) return@SimpleTextWatcher
                rows[p].price = text.toDoubleOrNull() ?: 0.0
                recalc()
            })
            b.deleteButton.setOnClickListener { onDelete(bindingAdapterPosition) }
            b.nameField.setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_TAB) {
                    b.qtyField.requestFocus(); true
                } else false
            }
        }

        fun bind(position: Int) {
            bound = position
            val row = rows[position]
            bindingNow = true
            b.nameField.setText(row.name)
            b.qtyField.setText(if (row.qty % 1.0 == 0.0) row.qty.toInt().toString() else row.qty.toString())
            b.priceField.setText(if (row.price == 0.0) "" else row.price.toString())
            bindingNow = false
            b.lineTotalText.text = String.format("%.2f", row.qty * row.price)

            namesAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line)
            b.nameField.setAdapter(namesAdapter)
            b.nameField.threshold = 1
            b.nameField.setOnItemClickListener { _, _, _, _ ->
                val p = bindingAdapterPosition
                if (p >= 0) rows[p].name = b.nameField.text.toString().trim()
                onChanged()
            }
        }
        private fun recalc() {
            val p = bound
            if (p >= 0 && p < rows.size) b.lineTotalText.text = String.format("%.2f", rows[p].qty * rows[p].price)
            onChanged()
        }
    }

    class SimpleTextWatcher(private val onText: (String) -> Unit) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onText(s?.toString().orEmpty()) }
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }

    fun addRow() { rows.add(Row()); notifyItemInserted(rows.lastIndex) }
    fun removeRow(position: Int) { if (position in rows.indices) { if (rows.size == 1) rows[0] = Row() else { rows.removeAt(position); notifyItemRemoved(position); notifyItemRangeChanged(position, rows.size - position) }; onChanged() } }
    fun reset() { rows.clear(); rows.add(Row()); notifyDataSetChanged(); onChanged() }
}
