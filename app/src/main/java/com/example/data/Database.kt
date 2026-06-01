package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val colorHex: Long, // Use standard ARGB Long representation like Color.value
    val category: String, // Exams, Birthdays, Finances, Habits
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val category: String = "Daily Task", // "Daily Task" or "Reminder"
    val dateString: String = "" // Readable date like "June 5"
)

@Entity(tableName = "finances")
data class FinancialItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "EMI", "Debt", "Subscription", "Lending"
    val dueDate: String, // "5th of Month", etc.
    val isPaid: Boolean = false,
    val friendName: String? = null,
    val viaPayment: String? = null,
    val needsRepay: Boolean = true
)

@Dao
interface AppDao {
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEvent)

    @Delete
    suspend fun deleteEvent(event: CalendarEvent)

    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskStatus(id: Int, isCompleted: Boolean)

    @Query("SELECT * FROM finances ORDER BY id DESC")
    fun getAllFinances(): Flow<List<FinancialItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialItem(item: FinancialItem)

    @Delete
    suspend fun deleteFinancialItem(item: FinancialItem)

    @Query("UPDATE finances SET isPaid = :isPaid WHERE id = :id")
    suspend fun updateFinancialPaidStatus(id: Int, isPaid: Boolean)
}

@Database(entities = [Note::class, CalendarEvent::class, Task::class, FinancialItem::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "personal_os_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        context.deleteDatabase("personal_os_database")
                        val instance = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "personal_os_database"
                        )
                        .fallbackToDestructiveMigration()
                        .build()
                        INSTANCE = instance
                        instance
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        val instance = Room.inMemoryDatabaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java
                        )
                        .fallbackToDestructiveMigration()
                        .build()
                        INSTANCE = instance
                        instance
                    }
                }
            }
        }
    }
}

class AppRepository(private val dao: AppDao) {
    val allNotes = dao.getAllNotes()
    val allEvents = dao.getAllEvents()
    val allTasks = dao.getAllTasks()
    val allFinances = dao.getAllFinances()

    suspend fun insertNote(note: Note) = dao.insertNote(note)
    suspend fun deleteNote(note: Note) = dao.deleteNote(note)
    
    suspend fun insertEvent(event: CalendarEvent) = dao.insertEvent(event)
    suspend fun deleteEvent(event: CalendarEvent) = dao.deleteEvent(event)

    suspend fun insertTask(task: Task) = dao.insertTask(task)
    suspend fun deleteTask(task: Task) = dao.deleteTask(task)
    suspend fun updateTaskStatus(id: Int, isCompleted: Boolean) = dao.updateTaskStatus(id, isCompleted)

    suspend fun insertFinancialItem(item: FinancialItem) = dao.insertFinancialItem(item)
    suspend fun deleteFinancialItem(item: FinancialItem) = dao.deleteFinancialItem(item)
    suspend fun updateFinancialPaidStatus(id: Int, isPaid: Boolean) = dao.updateFinancialPaidStatus(id, isPaid)
}
