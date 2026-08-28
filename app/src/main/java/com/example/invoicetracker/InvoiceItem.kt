package com.example.invoicetracker

data class InvoiceItem(
    var id: Long = System.currentTimeMillis(),
    var amount: Double = 0.0,
    var details: String = "",
    var balance: Double = 0.0
)