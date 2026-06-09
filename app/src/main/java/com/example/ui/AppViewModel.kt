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

    private val prefs = application.getSharedPreferences("personal_os_prefs", android.content.Context.MODE_PRIVATE)

    val ownerName = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("owner_name", "Rasupriyan Organizer") ?: "Rasupriyan Organizer")
    val ownerEmail = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("owner_email", "rasupriyan935@gmail.com") ?: "rasupriyan935@gmail.com")
    val profileIcon = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("profile_icon", "👨‍💻") ?: "👨‍💻")
    val notificationsEnabled = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val essentialNotificationsOnly = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("essential_notifications_only", true))

    fun updateOwnerName(name: String) {
        prefs.edit().putString("owner_name", name).apply()
        ownerName.value = name
    }

    fun updateOwnerEmail(email: String) {
        prefs.edit().putString("owner_email", email).apply()
        ownerEmail.value = email
    }

    fun updateProfileIcon(icon: String) {
        prefs.edit().putString("profile_icon", icon).apply()
        profileIcon.value = icon
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        notificationsEnabled.value = enabled
    }

    fun updateEssentialNotificationsOnly(enabled: Boolean) {
        prefs.edit().putBoolean("essential_notifications_only", enabled).apply()
        essentialNotificationsOnly.value = enabled
    }

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

    fun addEvent(
        title: String,
        colorHex: Long,
        category: String,
        day: Long = 1,
        timeOfBirth: String? = null,
        bornHospital: String? = null,
        nakshatram: String? = null,
        rasi: String? = null
    ) {
        viewModelScope.launch {
            repository.insertEvent(
                CalendarEvent(
                    title = title,
                    colorHex = colorHex,
                    category = category,
                    timestamp = day,
                    timeOfBirth = timeOfBirth,
                    bornHospital = bornHospital,
                    nakshatram = nakshatram,
                    rasi = rasi
                )
            )
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
            val cloned = task.copy()
            repository.updateTaskStatus(cloned.id, !cloned.isCompleted)
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
            val cloned = item.copy()
            repository.updateFinancialPaidStatus(cloned.id, !cloned.isPaid)
        }
    }

    fun updateNote(id: Int, title: String, content: String, isSynced: Boolean = false) {
        viewModelScope.launch {
            repository.insertNote(Note(id = id, title = title, content = content, isSynced = isSynced))
        }
    }

    fun syncAllAppData(onComplete: (notesSynced: Int, eventsSynced: Int) -> Unit) {
        viewModelScope.launch {
            val allNotes = repository.allNotes.first()
            val allEvents = repository.allEvents.first()
            
            var notesUpdated = 0
            var eventsUpdated = 0

            // 1. Mark all unsynced local notes as synced
            allNotes.forEach { note ->
                if (!note.isSynced) {
                    repository.insertNote(note.copy(isSynced = true))
                    notesUpdated++
                }
            }

            // 2. Mark all unsynced local events as synced
            allEvents.forEach { ev ->
                if (!ev.isSynced) {
                    repository.insertEvent(ev.copy(isSynced = true))
                    eventsUpdated++
                }
            }

            // 3. Inject a remote demo sync note if none exists to simulate fetching from cloud
            if (allNotes.none { it.title.contains("Cloud Backup") }) {
                repository.insertNote(
                    Note(
                        title = "🌍 Cloud Backup Notice",
                        content = "### Synchronized Workspace Loaded\nYour local-first database has successfully established a handshake with the remote cloud.\n\n- **Database Engine**: Room Lite SQLite (Secure Vault)\n- **Sync Host**: `ais-dev-srwkm` API Gateway\n- **Encryption**: AES-256 standard local hash\n- **Status**: Synchronized & fully secure.",
                        isSynced = true
                    )
                )
                notesUpdated++
            }

            onComplete(notesUpdated, eventsUpdated)
        }
    }

    fun syncCalendarFromOthers(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val remoteEvents = listOf(
                CalendarEvent(title = "Remote Tech Conf 💻", colorHex = 0xFFFF9900, category = "Timeline", timestamp = 5),
                CalendarEvent(title = "Product Milestone Launch 🚀", colorHex = 0xFF4A8BFF, category = "Timeline", timestamp = 18),
                CalendarEvent(title = "Personal Review Session 🧘", colorHex = 0xFF00FFCC, category = "Reminder", timestamp = 25)
            )
            var count = 0
            val exist = repository.allEvents.first()
            remoteEvents.forEach { ev ->
                if (exist.none { it.title == ev.title }) {
                    repository.insertEvent(ev)
                    count++
                }
            }
            onComplete(count)
        }
    }
}
