package com.meditation.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.meditation.tracker.data.model.MeditationMode
import com.meditation.tracker.data.model.SessionHistory

class Converters {
    @TypeConverter
    fun fromMode(mode: MeditationMode): String = mode.name

    @TypeConverter
    fun toMode(name: String): MeditationMode = runCatching {
        MeditationMode.valueOf(name)
    }.getOrDefault(MeditationMode.LONG_DURATION)
}

@Database(entities = [SessionHistory::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
