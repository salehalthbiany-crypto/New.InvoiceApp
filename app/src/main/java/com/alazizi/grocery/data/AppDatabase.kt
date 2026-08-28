package com.alazizi.grocery.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Product::class, Customer::class, Invoice::class, InvoiceItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun products(): ProductDao
    abstract fun customers(): CustomerDao
    abstract fun invoices(): InvoiceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "alazizi.db").build().also { INSTANCE = it }
        }
    }
}
