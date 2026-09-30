package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        GuestEntity::class,
        BusBookingEntity::class,
        WishEntity::class,
        SharedPhotoEntity::class,
        GiftTargetEntity::class,
        GiftCollectorEntity::class,
        EventNotificationEntity::class
    ],
    // v2: regali senza importi, cassiere delle quote uniche, niente quote per singolo regalo.
    // Il DB è una cache dello snapshot del server (o dei dati demo): in caso di cambio schema
    // viene ricreato da zero e ripopolato.
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun guestDao(): GuestDao
    abstract fun busBookingDao(): BusBookingDao
    abstract fun wishDao(): WishDao
    abstract fun photoDao(): PhotoDao
    abstract fun giftDao(): GiftDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "neuro_party_database"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
