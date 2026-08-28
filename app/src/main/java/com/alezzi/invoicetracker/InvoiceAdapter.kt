package com.alezzi.invoicetracker

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class InvoiceAdapter(
    private val items: MutableList<InvoiceItem>,
    private val onItemChanged: () -> Unit
) : RecyclerView.Adapter<InvoiceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val etAmount: EditText = view.findViewById(R.id.etAmount)
        val etDetails: EditText = view.findViewById(R.id.etDetails)
        val tvBalance: TextView = view.findViewById(R.id.tvBalance)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteRow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.etAmount.setText(if (item.amount > 0) item.amount.toString() else "")
        holder.etDetails.setText(item.details)
        holder.tvBalance.text = String.format(Locale.US, "%.2f", item.balance)

        holder.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (holder.etAmount.hasFocus()) {
                    val valStr = s?.toString() ?: ""
                    item.amount = valStr.toDoubleOrNull() ?: 0.0
                    onItemChanged()
                }
            }
        })

        holder.etDetails.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (holder.etDetails.hasFocus()) {
                    item.details = s?.toString() ?: ""
                }
            }
        })

        holder.btnDelete.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            val pos = if (currentPos != RecyclerView.NO_POSITION) currentPos else holder.absoluteAdapterPosition
            if (pos != RecyclerView.NO_POSITION && items.size > 0) {
                items.removeAt(pos)
                notifyItemRemoved(pos)
                notifyItemRangeChanged(pos, items.size)
                onItemChanged()
            }
        }
    }

    override fun getItemCount() = items.size
}