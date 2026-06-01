package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).appDao()
    private val repository = AppRepository(dao)

    val notesUiState: StateFlow<List<Note>> = repository.allNotes
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val eventsUiState: StateFlow<List<CalendarEvent>> = repository.allEvents
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tasksUiState: StateFlow<List<Task>> = repository.allTasks
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val financesUiState: StateFlow<List<FinancialItem>> = repository.allFinances
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pre-populate data if the database is initially empty
        viewModelScope.launch {
            try {
                val currentTasks = repository.allTasks.first()
                if (currentTasks.isEmpty()) {
                    // Populate Default Tasks for India-based organizer
                    repository.insertTask(Task(title = "Morning yoga & pranayama 🧘‍♀", category = "Daily Task", dateString = "Daily"))
                    repository.insertTask(Task(title = "Complete advanced Kotlin layouts", category = "Daily Task", dateString = "June 1"))
                    repository.insertTask(Task(title = "Check GPay/PhonePe UPI Autopay mandates 💸", category = "Daily Task", dateString = "June 2"))
                    repository.insertTask(Task(title = "Order groceries from Blinkit/Zepto 🥦", category = "Daily Task", dateString = "Daily"))
                    repository.insertTask(Task(title = "Water balcony flower pots 🌱", category = "Daily Task", dateString = "Daily"))
                    repository.insertTask(Task(title = "HP Gas Cylinder booking 💻", category = "Reminder", dateString = "June 18"))
                }

                val currentEvents = repository.allEvents.first()
                if (currentEvents.isEmpty()) {
                    // Populate default calendar events and birthdays (Note: timestamp represents June day index)
                    // 0xFFFF4D4D for Crimson Red, 0xFFFFB399 for Orange, etc.
                    repository.insertEvent(CalendarEvent(title = "My Birthday Celebration 🎂", colorHex = 0xFFFF4D4D, category = "Birthdays", timestamp = 5))
                    repository.insertEvent(CalendarEvent(title = "Diwali prep sync with cousins 🪔", colorHex = 0xFFFF9900, category = "Timeline", timestamp = 18))
                    repository.insertEvent(CalendarEvent(title = "Dentist Appointment 🩺", colorHex = 0xFFA1EAC6, category = "Reminder", timestamp = 15))
                    repository.insertEvent(CalendarEvent(title = "Team Daily Scrum Meeting 👥", colorHex = 0xFF4A8BFF, category = "Timeline", timestamp = 1))
                }

                val currentFinances = repository.allFinances.first()
                if (currentFinances.isEmpty()) {
                    // Populate default Indian subscriptions, EMIs and debts (in INR ₹ equivalents)
                    repository.insertFinancialItem(FinancialItem(title = "Jio Fiber Broadband 🌐", amount = 999.00, type = "Subscription", dueDate = "5", isPaid = false))
                    repository.insertFinancialItem(FinancialItem(title = "YouTube Premium India 🎬", amount = 189.00, type = "Subscription", dueDate = "12", isPaid = true))
                    repository.insertFinancialItem(FinancialItem(title = "Spotify Premium Individual 🎵", amount = 119.00, type = "Subscription", dueDate = "18", isPaid = false))
                    repository.insertFinancialItem(FinancialItem(title = "HDFC Home Loan EMI 🏠", amount = 27400.00, type = "EMI", dueDate = "8", isPaid = false))
                    repository.insertFinancialItem(FinancialItem(title = "SBI Car Loan Instalment 🚗", amount = 12500.00, type = "EMI", dueDate = "15", isPaid = false))
                    repository.insertFinancialItem(FinancialItem(title = "Society Maintenance Bill 🏢", amount = 3500.00, type = "EMI", dueDate = "10", isPaid = false))
                    repository.insertFinancialItem(FinancialItem(title = "Recharge Mother's Airtel Plan 📱", amount = 299.00, type = "Subscription", dueDate = "22", isPaid = false))
                    repository.insertFinancialItem(FinancialItem(title = "Settle dues with Rahul 🤝", amount = 1500.00, type = "Debt", dueDate = "25", isPaid = false))
                }

                val currentNotes = repository.allNotes.first()
                if (currentNotes.isEmpty()) {
                    repository.insertNote(Note(title = "Personal OS Quickstart", content = "Welcome to your Personal OS Dashboard. Here you can organize your timeline, manage subscriptions, set reminders, and log details. Use the central + menu to get started!"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            repository.insertNote(Note(title = title, content = content))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun addEvent(title: String, colorHex: Long, category: String, day: Long = 1) {
        viewModelScope.launch {
            repository.insertEvent(CalendarEvent(title = title, colorHex = colorHex, category = category, timestamp = day))
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    fun addTask(title: String, category: String, dateString: String) {
        viewModelScope.launch {
            repository.insertTask(Task(title = title, category = category, dateString = dateString))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleTaskStatus(task: Task) {
        viewModelScope.launch {
            repository.updateTaskStatus(task.id, !task.isCompleted)
        }
    }

    fun addFinancialItem(
        title: String,
        amount: Double,
        type: String,
        dueDate: String,
        friendName: String? = null,
        viaPayment: String? = null,
        needsRepay: Boolean = true
    ) {
        viewModelScope.launch {
            repository.insertFinancialItem(
                FinancialItem(
                    title = title,
                    amount = amount,
                    type = type,
                    dueDate = dueDate,
                    friendName = friendName,
                    viaPayment = viaPayment,
                    needsRepay = needsRepay
                )
            )
        }
    }

    fun deleteFinancialItem(item: FinancialItem) {
        viewModelScope.launch {
            repository.deleteFinancialItem(item)
        }
    }

    fun toggleFinancialPaidStatus(item: FinancialItem) {
        viewModelScope.launch {
            repository.updateFinancialPaidStatus(item.id, !item.isPaid)
        }
    }
}
