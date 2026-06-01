package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CalendarEvent
import com.example.data.Note
import com.example.data.Task
import com.example.data.FinancialItem
import com.example.ui.theme.*

enum class AppTab {
    HOME, CALENDAR, FINANCES, NOTES
}

data class InAppNotificationData(
    val title: String,
    val message: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.NotificationsActive
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: AppViewModel = viewModel()) {
    val notes by viewModel.notesUiState.collectAsStateWithLifecycle()
    val events by viewModel.eventsUiState.collectAsStateWithLifecycle()
    val tasks by viewModel.tasksUiState.collectAsStateWithLifecycle()
    val finances by viewModel.financesUiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var activeNotification by remember { mutableStateOf<InAppNotificationData?>(null) }

    // Immersive, high-fidelity OS Boot Sequence state variables
    var showBootSequence by rememberSaveable { mutableStateOf(true) }
    val displayedBootLogs = remember { mutableStateListOf<String>() }
    var bootAlpha by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        displayedBootLogs.add("⚡ PERSONAL_OS BOOT INITIALIZING...")
        kotlinx.coroutines.delay(200)
        displayedBootLogs.add("🛰 MOUNTING RAM CACHE VIRTUAL BLOCK...")
        kotlinx.coroutines.delay(200)
        displayedBootLogs.add("🗃 CHECKING ROOM DATABASE DAO [STATUS: 200]")
        kotlinx.coroutines.delay(200)
        displayedBootLogs.add("⏰ INTEGRATING INDIAN STANDARD TIME: IST (GMT+5:30)")
        kotlinx.coroutines.delay(150)
        displayedBootLogs.add("✅ SECURE BOOT COMPLETE. COMPILING CHANNELS...")
        kotlinx.coroutines.delay(250)
        bootAlpha = 0f
        kotlinx.coroutines.delay(100)
        showBootSequence = false
    }

    val triggerNotification = remember {
        { title: String, message: String ->
            activeNotification = InAppNotificationData(title, message)
            NotificationHelper.showNotification(context, title, message)
        }
    }

    // Trigger local welcoming context for India dwellers
    LaunchedEffect(Unit) {
        triggerNotification(
            "Namaste & Welcome! 🇮🇳",
            "Personal OS is running on Indian Standard Time. Settle bills using Indian Rupee (₹) equivalents."
        )
    }

    // Auto-dismiss inside-app heads up alerts
    LaunchedEffect(activeNotification) {
        if (activeNotification != null) {
            kotlinx.coroutines.delay(4500)
            activeNotification = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Integrated premium central '+' bar
            FloatingTabBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                onAddClick = { showDialog = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    events = events,
                    tasks = tasks,
                    finances = finances,
                    onNavigateToFinances = { currentTab = AppTab.FINANCES },
                    onNavigateToCalendar = { currentTab = AppTab.CALENDAR },
                    onTriggerNotification = triggerNotification
                )
                AppTab.CALENDAR -> CalendarScreen(
                    viewModel = viewModel,
                    events = events,
                    tasks = tasks,
                    finances = finances,
                    onTriggerNotification = triggerNotification
                )
                AppTab.FINANCES -> FinancesScreen(
                    viewModel = viewModel,
                    finances = finances,
                    onTriggerNotification = triggerNotification
                )
                AppTab.NOTES -> NotesScreen(
                    viewModel = viewModel,
                    notes = notes,
                    onTriggerNotification = triggerNotification
                )
            }

            // Slide down heads-up floating notification banner with premium styling and pop up alignment
            AnimatedVisibility(
                visible = activeNotification != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                activeNotification?.let { notif ->
                    HeadsUpNotificationBanner(
                        title = notif.title,
                        message = notif.message,
                        icon = notif.icon,
                        onDismiss = { activeNotification = null }
                    )
                }
            }

            // Boot Screen sequence overlay
            if (showBootSequence) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF07070B).copy(alpha = bootAlpha))
                        .clickable(enabled = false) {} // Prevent click-through closing
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF00FFCC),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Personal OS v2.6.4",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FFCC)
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        displayedBootLogs.forEach { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal dialog to add entries (with smooth premium popup transition)
    if (showDialog) {
        AddEntryDialog(
            viewModel = viewModel,
            onDismiss = { showDialog = false },
            onTriggerNotification = { title, msg ->
                triggerNotification(title, msg)
            }
        )
    }
}

// ==========================================
// SCREEN 1: HOME SCREEN
// ==========================================
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    events: List<CalendarEvent>,
    tasks: List<Task>,
    finances: List<FinancialItem>,
    onNavigateToFinances: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onTriggerNotification: (String, String) -> Unit
) {
    var quickTaskTitle by remember { mutableStateOf("") }
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    // Cascading container transition animations
    var visible0 by remember { mutableStateOf(false) }
    var visible1 by remember { mutableStateOf(false) }
    var visible2 by remember { mutableStateOf(false) }
    var visible3 by remember { mutableStateOf(false) }
    var visible4 by remember { mutableStateOf(false) }
    var visible5 by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60)
        visible0 = true
        kotlinx.coroutines.delay(80)
        visible1 = true
        kotlinx.coroutines.delay(80)
        visible2 = true
        kotlinx.coroutines.delay(80)
        visible3 = true
        kotlinx.coroutines.delay(80)
        visible4 = true
        kotlinx.coroutines.delay(80)
        visible5 = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnimatedVisibility(
                visible = visible0,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "$greeting, Organizer",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Your Personal OS dashboard is compiled (IST)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    onTriggerNotification(
                                        "Daily Health Check 🇮🇳",
                                        "Remember to drink 3 liters of water and enjoy a cup of hot Chai! ☕"
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Metrics Section with custom elevations and padding
        item {
            AnimatedVisibility(
                visible = visible1,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val unfinishedTasks = tasks.count { !it.isCompleted }
                    val totalAmountDue = finances.filter { !it.isPaid }.sumOf { item -> item.amount }
                    
                    MetricCard(
                        title = "Daily Tasks",
                        value = "$unfinishedTasks Unfinished",
                        icon = Icons.Default.CheckCircle,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        accentColor = MintGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Pending Bills",
                        value = "₹${String.format("%.0f", totalAmountDue)}",
                        icon = Icons.Default.Payments,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        accentColor = Color(0xFFFF4D4D), // Red Accent
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToFinances
                    )
                }
            }
        }

        // Segment 1: Urgent Reminders & Tasks
        item {
            AnimatedVisibility(
                visible = visible2,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Daily Commitments",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Quick Add Task Bar
                        OutlinedTextField(
                            value = quickTaskTitle,
                            onValueChange = { quickTaskTitle = it },
                            placeholder = { Text("Add standard task...", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            trailingIcon = {
                                if (quickTaskTitle.isNotBlank()) {
                                    IconButton(onClick = {
                                        viewModel.addTask(quickTaskTitle, "Daily Task", "Daily")
                                        onTriggerNotification(
                                            "New Task Added ⚡",
                                            "Successfully logged: '${quickTaskTitle}'"
                                        )
                                        quickTaskTitle = ""
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Quick Add", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (tasks.isEmpty()) {
                            Text(
                                text = "No pending daily tasks or reminders. Standard slate clean!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                tasks.take(4).forEach { task ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .clickable {
                                                viewModel.toggleTaskStatus(task)
                                                onTriggerNotification(
                                                    "Task Code Updated ⚡",
                                                    "Task '${task.title}' is now marked as ${if (!task.isCompleted) "Completed ✅" else "Incomplete 🎯"}."
                                                )
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Toggle task",
                                            tint = if (task.isCompleted) MintGreen else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = task.dateString,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Segment 2: Upcoming Birthdays & Events Timeline
        item {
            AnimatedVisibility(
                visible = visible3,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
                exit = fadeOut()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upcoming Timeline & Birthdays",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(onClick = onNavigateToCalendar) {
                            Text("View Calendar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (events.isEmpty()) {
                        EmptyListCard("No birthdays or events entered on calendar.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            events.take(4).forEach { event ->
                                val isBirthday = event.category.equals("birthdays", ignoreCase = true)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isBirthday) SoftPink.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isBirthday) SoftPink.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Visual color indicator capsule
                                        Box(
                                            modifier = Modifier
                                                .size(width = 6.dp, height = 36.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(safeColorFromLong(event.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        
                                        // Custom Cake Icon for Birthdays
                                        if (isBirthday) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(SoftPink.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Cake,
                                                    contentDescription = "Birthday 🎂",
                                                    tint = SoftPink,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = event.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = if (isBirthday) "Birthday Celebration 🎂" else event.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isBirthday) SoftPink else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Text(
                                                text = "June ${event.timestamp}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Segment 3: Subscriptions and Dues Overview
        item {
            AnimatedVisibility(
                visible = visible4,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
                exit = fadeOut()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upcoming Billing EMIs & Subscriptions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(onClick = onNavigateToFinances) {
                            Text("Open Finances", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (finances.isEmpty()) {
                        EmptyListCard("No financial records saved.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            finances.take(3).forEach { item ->
                                val isNetflix = item.title.lowercase().contains("netflix")
                                val isYoutube = item.title.lowercase().contains("youtube") || item.title.lowercase().contains("yt")
                                val isSpotify = item.title.lowercase().contains("spotify")

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Custom Branding color circles representing "Financial Logo"
                                        val brandBgColor = when {
                                            isNetflix -> Color(0xFFE50914).copy(alpha = 0.15f)
                                            isYoutube -> Color(0xFFFF0000).copy(alpha = 0.15f)
                                            isSpotify -> Color(0xFF1DB954).copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        }
                                        val brandIconColor = when {
                                            isNetflix -> Color(0xFFFF5252)
                                            isYoutube -> Color(0xFFFF0000)
                                            isSpotify -> Color(0xFF1ED760)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        val brandIcon = when {
                                            isNetflix || isYoutube -> Icons.Default.PlayCircle
                                            isSpotify -> Icons.Default.MusicNote
                                            else -> Icons.Default.Payments
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(brandBgColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = brandIcon,
                                                contentDescription = null,
                                                tint = brandIconColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = "${item.type} • Due Day ${item.dueDate}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "₹${String.format("%.0f", item.amount)}",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (item.isPaid) "PAID ✅" else "UNPAID ⏳",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (item.isPaid) MintGreen else Color(0xFFFF4D4D),
                                                modifier = Modifier.clickable {
                                                    viewModel.toggleFinancialPaidStatus(item)
                                                    onTriggerNotification(
                                                        "Transaction Log 💸",
                                                        "'${item.title}' is now marked as ${if (!item.isPaid) "PAID ✅" else "UNPAID ⏳"}"
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Segment 4: India Specific Routine Reminders Panel
        item {
            AnimatedVisibility(
                visible = visible5,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
                exit = fadeOut()
            ) {
                IndiaRoutinesRemindersPanel(onTriggerNotification = onTriggerNotification)
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ==========================================
// SCREEN 2: HIGH CONTRAST CALENDAR (RED & BLACK)
// ==========================================
@Composable
fun CalendarScreen(
    viewModel: AppViewModel,
    events: List<CalendarEvent>,
    tasks: List<Task>,
    finances: List<FinancialItem>,
    onTriggerNotification: (String, String) -> Unit
) {
    var selectedDay by rememberSaveable { mutableStateOf(1) }
    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "June 2026",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF1E1E)))
                Text(
                    text = "High Contrast Mode: Red & Black • Precise Markdown Coloring",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF3333) // Sharp Red accent
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Calendar Grid System
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                // Matte Black Card
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.5.dp, Color(0xFFFF1E1E).copy(alpha = 0.5f)) // Thick red border
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Weekday headers: Grey & Red
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysOfWeek.forEachIndexed { index, day ->
                            val isWeekend = index >= 5
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isWeekend) Color(0xFFFF1E1E) else Color.Gray
                            )
                        }
                    }

                    // Days Grid Creator
                    val daysList = (1..30).toList()
                    val rowsCount = (daysList.size + 6) / 7

                    for (row in 0 until rowsCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0..6) {
                                val dayIndex = row * 7 + col
                                if (dayIndex < daysList.size) {
                                    val day = daysList[dayIndex]
                                    val isSelected = selectedDay == day

                                    // Filter events/dues at exact integer index
                                    val dayEvents = events.filter { it.timestamp == day.toLong() }
                                    val dayFinances = finances.filter {
                                        it.dueDate == day.toString() || it.dueDate.contains("June $day") || (it.dueDate.filter { c -> c.isDigit() } == day.toString())
                                    }

                                    val hasEvent = dayEvents.isNotEmpty()
                                    val hasFinance = dayFinances.isNotEmpty()

                                    // Dynamic Color Mapping Rules: High Contrast Red & Black + Users Highlight Colors
                                    val cellBgColor = when {
                                        isSelected -> Color(0xFFFF1E1E).copy(alpha = 0.35f) // Brand Red highlight glow
                                        hasEvent -> safeColorFromLong(dayEvents.first().colorHex).copy(alpha = 0.45f) // EXACT MARKED DOWN COLOR PILL
                                        hasFinance -> Color(0xFFFF9933).copy(alpha = 0.15f) // Warm Saffron due alert bg
                                        else -> Color(0xFF141414) // Pure space charcoal black
                                    }

                                    val cellBorderColor = when {
                                        isSelected -> Color(0xFFFF1E1E) // Sharp Red active indicator
                                        hasEvent -> safeColorFromLong(dayEvents.first().colorHex) // Glowing highlight with exact color hex
                                        else -> Color.White.copy(alpha = 0.04f)
                                    }

                                    val isWeekend = col >= 5
                                    val textWeight = if (isSelected || hasEvent) FontWeight.Bold else FontWeight.Normal
                                    val textColor = when {
                                        isSelected -> Color(0xFFFF1E1E)
                                        hasEvent -> safeColorFromLong(dayEvents.first().colorHex) // EXACT custom markdown color
                                        isWeekend -> Color(0xFFFF5252) // Weekend red hue
                                        else -> Color.White
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(cellBgColor)
                                            .border(
                                                width = if (isSelected || hasEvent) 1.5.dp else 1.dp,
                                                color = cellBorderColor,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedDay = day }
                                            .padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = textWeight,
                                                fontSize = 12.sp
                                            ),
                                            color = textColor
                                        )

                                        // Indicator bubbles
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            dayEvents.take(2).forEach { ev ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(safeColorFromLong(ev.colorHex))
                                                )
                                            }
                                            if (hasFinance) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFF9933)) // Indian Saffron alert billing dot
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expanded Selected Day Schedule Itemizer
        item {
            val selectedDayEvents = events.filter { it.timestamp == selectedDay.toLong() }
            val selectedDayFinances = finances.filter {
                it.dueDate == selectedDay.toString() || it.dueDate.contains("June $selectedDay") || (it.dueDate.filter { c -> c.isDigit() } == selectedDay.toString())
            }

            Text(
                text = "Day Schedule: June $selectedDay",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (selectedDayEvents.isEmpty() && selectedDayFinances.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No milestones, birthdays or EMI commitments scheduled for today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    selectedDayEvents.forEach { event ->
                        val isBirthday = event.category.equals("birthdays", ignoreCase = true)
                        Card(
                            modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                            border = BorderStroke(1.2.dp, safeColorFromLong(event.colorHex).copy(alpha = 0.8f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                              ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(safeColorFromLong(event.colorHex))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isBirthday) "Birthday Celebration 🎂" else "Event • ${event.category}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isBirthday) SoftPink else Color.LightGray
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.deleteEvent(event)
                                    onTriggerNotification(
                                        "Milestone Cleared 🧹",
                                        "Successfully deleted timeline milestone event: '${event.title}'"
                                    )
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF3333).copy(alpha = 0.8f))
                                }
                            }
                        }
                    }

                    selectedDayFinances.forEach { fin ->
                        Card(
                            modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (fin.isPaid) MintGreen.copy(alpha = 0.15f) else Color(0xFFFF1E1E).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = if (fin.isPaid) MintGreen else Color(0xFFFF3333),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fin.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${fin.type} • ₹${String.format("%.0f", fin.amount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.LightGray
                                    )
                                }
                                TextButton(onClick = {
                                    viewModel.toggleFinancialPaidStatus(fin)
                                    onTriggerNotification(
                                        "Transaction Processed 💸",
                                        "'${fin.title}' status shifted to ${if (!fin.isPaid) "PAID ✅" else "UNPAID ⏳"}"
                                    )
                                }) {
                                    Text(
                                        text = if (fin.isPaid) "Paid ✅" else "Mark Paid",
                                        color = if (fin.isPaid) MintGreen else Color(0xFFFF5252),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.deleteFinancialItem(fin)
                                    onTriggerNotification(
                                        "Commitment Cleared 🧹",
                                        "Successfully deleted billing entry: '${fin.title}'"
                                    )
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF3333).copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// ==========================================
// SCREEN 3: FINANCIAL DEBTS & SUBSCRIPTION HUB
// ==========================================
@Composable
fun FinancesScreen(
    viewModel: AppViewModel,
    finances: List<FinancialItem>,
    onTriggerNotification: (String, String) -> Unit
) {
    var filterSelected by remember { mutableStateOf("All") }

    val filteredList = finances.filter {
        filterSelected == "All" || it.type.equals(filterSelected, ignoreCase = true)
    }

    val totalSpent = finances.filter { it.isPaid }.sumOf { it.amount }
    val totalUnpaid = finances.filter { !it.isPaid }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Financial Logbook",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = Color.White
            )
            Text(
                text = "Keep track of outstanding debts, mobile recharges, Netflix/YT premium and EMIs.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Stats Card overview (Rupee localized)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Monthly Balance Summary (IST)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Settled payout", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                  text = "₹${String.format("%.0f", totalSpent)}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MintGreen
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Due commitment", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                  text = "₹${String.format("%.0f", totalUnpaid)}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFF4D4D)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Dynamic category horizontal chips selector
        item {
            val categories = listOf("All", "EMI", "Debt", "Subscription", "Friend Borrow")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                categories.forEach { category ->
                    val isSelected = filterSelected == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { filterSelected = category }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                EmptyListCard("No financial records stored in category: $filterSelected")
            }
        } else {
            items(filteredList) { item ->
                val isNetflix = item.title.lowercase().contains("netflix")
                val isYoutube = item.title.lowercase().contains("youtube") || item.title.lowercase().contains("yt")
                val isSpotify = item.title.lowercase().contains("spotify")
                val isFriendBorrow = item.type == "Friend Borrow"

                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Branding Circle for UI Polish ("Financial Logo")
                        val brandBgColor = when {
                            isFriendBorrow -> Color(0xFFFB8C00).copy(alpha = 0.15f) // Warm visual orange for friend trades
                            isNetflix -> Color(0xFFE50914).copy(alpha = 0.15f)
                            isYoutube -> Color(0xFFFF0000).copy(alpha = 0.15f)
                            isSpotify -> Color(0xFF1DB954).copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val brandIconColor = when {
                            isFriendBorrow -> Color(0xFFF57C00)
                            isNetflix -> Color(0xFFFF5252)
                            isYoutube -> Color(0xFFFF0000)
                            isSpotify -> Color(0xFF1ED760)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val brandIcon = when {
                            isFriendBorrow -> Icons.Default.Person
                            isNetflix || isYoutube -> Icons.Default.PlayCircle
                            isSpotify -> Icons.Default.MusicNote
                            else -> Icons.Default.Payments
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(brandBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = brandIcon,
                                contentDescription = item.title,
                                tint = brandIconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isFriendBorrow) {
                                Text(
                                    text = "Friend: ${item.friendName ?: "Friend"} • via ${item.viaPayment ?: "UPI"}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(6.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (item.needsRepay) Color(0xFFFF1E1E).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Text(
                                            text = if (item.needsRepay) "REPAYMENT DUE" else "NO REPAY",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (item.needsRepay) Color(0xFFFF5252) else Color.Gray,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "Date: June ${item.dueDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(6.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text(
                                            text = item.type.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "Due Day: ${item.dueDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            // Monospace Amount Layout Rupee localized
                            Text(
                                text = "₹${String.format("%.0f", item.amount)}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(
                                onClick = {
                                    if (!isFriendBorrow || item.needsRepay) {
                                        viewModel.toggleFinancialPaidStatus(item)
                                        onTriggerNotification(
                                            if (isFriendBorrow) "Repayment Status Updated 🤝" else "EMI Status Updated 💸",
                                            if (isFriendBorrow) {
                                                "Bill for friend '${item.friendName}' scheduled as ${if (!item.isPaid) "REPAID ✅" else "PENDING ⏳"}."
                                            } else {
                                                "Bill '${item.title}' has been scheduled as ${if (!item.isPaid) "PAID ✅" else "UNPAID ⏳"}."
                                            }
                                        )
                                    }
                                },
                                enabled = !isFriendBorrow || item.needsRepay,
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (isFriendBorrow) {
                                        if (!item.needsRepay) "GIFT / NO REPAY"
                                        else if (item.isPaid) "REPAID ✅"
                                        else "REPAY 🤝"
                                    } else {
                                        if (item.isPaid) "PAID ✅" else "MARK PAID"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (isFriendBorrow && !item.needsRepay) Color.Gray
                                            else if (item.isPaid) MintGreen 
                                            else Color(0xFFFF4D4D)
                                )
                            }
                        }

                        IconButton(onClick = {
                            viewModel.deleteFinancialItem(item)
                            onTriggerNotification(
                                "Finance Record Removed 🧹",
                                "Removed billing record: '${item.title}' from dashboard."
                            )
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF4D4D).copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// ==========================================
// SCREEN 4: DAILY CANVAS NOTES SCREEN
// ==========================================
@Composable
fun NotesScreen(
    viewModel: AppViewModel,
    notes: List<Note>,
    onTriggerNotification: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Daily Canvas Logs",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = Color.White
            )
            Text(
                text = "Capture long form goals, workout regimens, or technical brain dumps here.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (notes.isEmpty()) {
            item {
                EmptyListCard("No personal logs recorded on canvas.")
            }
        } else {
            items(notes) { note ->
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            IconButton(onClick = {
                                viewModel.deleteNote(note)
                                onTriggerNotification(
                                    "Log Canvas Cleared 🧹",
                                    "Log record: '${note.title}' has been successfully deleted."
                                )
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = Color(0xFFFF5252).copy(alpha = 0.8f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// ==========================================
// CUSTOM DIALOG: PREMIUM POP-UP (SMOOTH SLIDE ANIMATION)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onTriggerNotification: (String, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Event, 1: Note, 2: Task, 3: Finance
    
    // Core states
    var titleText by remember { mutableStateOf("") }
    
    // Lifecycled database states collected contextually for duplicate scanning
    val notes by viewModel.notesUiState.collectAsStateWithLifecycle()
    val events by viewModel.eventsUiState.collectAsStateWithLifecycle()
    val tasks by viewModel.tasksUiState.collectAsStateWithLifecycle()
    val finances by viewModel.financesUiState.collectAsStateWithLifecycle()

    // Interactive custom scheduler states
    var birthdayMonth by remember { mutableStateOf("June") }
    var reminderHour by remember { mutableStateOf("09") }
    var reminderMinute by remember { mutableStateOf("00") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Event configs
    var eventCategory by remember { mutableStateOf("Timeline") }
    var eventDay by remember { mutableStateOf(1) }
    var selectedColorLong by remember { mutableStateOf(PastelBlue.value.toLong()) }

    // Note configs
    var noteContent by remember { mutableStateOf("") }

    // Task configs
    var taskCategory by remember { mutableStateOf("Daily Task") }
    var taskDay by remember { mutableStateOf(1) }

    // Finance configs
    var financeAmountText by remember { mutableStateOf("") }
    var financeType by remember { mutableStateOf("EMI") }
    var financeDay by remember { mutableStateOf(1) }
    var friendName by remember { mutableStateOf("") }
    var viaPayment by remember { mutableStateOf("UPI") }
    var repayNeeded by remember { mutableStateOf(true) }

    // Vibrant highlight color selection list (expanded including deep crimson / red and custom blacks)
    val colorsList = listOf(
        "Crimson" to Color(0xFFFF4D4D), // Cherry/Crimson Red (Vibrant)
        "Red" to Color(0xFFFF1E1E),     // Extreme pure Red
        "Pink" to SoftPink,
        "Green" to MintGreen,
        "Blue" to PastelBlue,
        "Yellow" to CreamYellow,
        "Lavender" to Lavender,
        "Orange" to SunsetOrange,
        "Coral" to CoralRose,
        "Teal" to TurquoiseTeal,
        "Gray" to Color(0xFF6B6B6B),
        "Dark border" to Color(0xFF2A2A2A) // Sleek Black/Charcoal highlight
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Animation state to slide in card smoothly
        var animateIn by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            animateIn = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = animateIn,
                enter = slideInVertically(initialOffsetY = { it / 3 }, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut(),
                modifier = Modifier.clickable(enabled = false) {} // Prevent click-through closing
            ) {
                Card(
                     modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "+ Write Personal Entry",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Symmetrical Tab Selector Navigation Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val tabsList = listOf(
                                Triple(0, Icons.Default.Event, "Event"),
                                Triple(1, Icons.Default.Description, "Note"),
                                Triple(2, Icons.Default.CheckCircle, "Task"),
                                Triple(3, Icons.Default.Payments, "Finance")
                            )
                            tabsList.forEach { (index, icon, label) ->
                                val isSelected = selectedTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { 
                                            selectedTab = index 
                                            errorMessage = null // Reset message on tab switch
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp
                                            ),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Shared Title Input Field
                        OutlinedTextField(
                            value = titleText,
                            onValueChange = { 
                                titleText = it 
                                errorMessage = null // clear error when typing
                            },
                            placeholder = { Text("What is this entry about?") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        // Render Tab specific attributes
                        when (selectedTab) {
                            0 -> { // EVENT CONFIGURATOR
                                Text("Event Category", style = MaterialTheme.typography.titleSmall)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val cats = listOf("Timeline", "Birthdays", "Reminder")
                                    cats.forEach { cat ->
                                        val isSel = eventCategory == cat
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { 
                                                    eventCategory = cat 
                                                    errorMessage = null
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = cat,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                if (eventCategory == "Birthdays") {
                                    Text("Select Custom Birth Month", style = MaterialTheme.typography.titleSmall)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                                        months.forEach { m ->
                                            val isM = birthdayMonth == m
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                    .clickable { birthdayMonth = m; errorMessage = null }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = m, 
                                                    style = MaterialTheme.typography.labelSmall, 
                                                    color = if (isM) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                    
                                    Text("Select Exact Day of Month", style = MaterialTheme.typography.titleSmall)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Slider(
                                            value = eventDay.toFloat(),
                                            onValueChange = { eventDay = it.toInt(); errorMessage = null },
                                            valueRange = 1f..31f,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "Day $eventDay", 
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), 
                                            color = MaterialTheme.colorScheme.primary, 
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                } else if (eventCategory == "Reminder") {
                                    Text("Selected June Target Day", style = MaterialTheme.typography.titleSmall)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Slider(
                                            value = eventDay.toFloat(),
                                            onValueChange = { eventDay = it.toInt(); errorMessage = null },
                                            valueRange = 1f..30f,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "June $eventDay",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }

                                    // Complete Time Selection Selector for reminders
                                    Text("Complete Time Settings (IST Standard)", style = MaterialTheme.typography.titleSmall)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Hour: $reminderHour hh", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                            Slider(
                                                value = reminderHour.toFloat(),
                                                onValueChange = { reminderHour = String.format("%02d", it.toInt()); errorMessage = null },
                                                valueRange = 0f..23f
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Min: $reminderMinute mm", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                            Slider(
                                                value = reminderMinute.toFloat(),
                                                onValueChange = { reminderMinute = String.format("%02d", it.toInt()); errorMessage = null },
                                                valueRange = 0f..59f
                                            )
                                        }
                                    }
                                } else {
                                    Text("June Schedule Day (1-30)", style = MaterialTheme.typography.titleSmall)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Slider(
                                            value = eventDay.toFloat(),
                                            onValueChange = { eventDay = it.toInt(); errorMessage = null },
                                            valueRange = 1f..30f,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "Day $eventDay",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }

                                // Color picker Highlight Selection
                                Text("Choose Markdown Color Highlight", style = MaterialTheme.typography.titleSmall)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        colorsList.forEach { (_, col) ->
                                            val isSelected = selectedColorLong == col.value.toLong()
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(col)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 0.dp,
                                                        color = if (isSelected) Color.White else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { selectedColorLong = col.value.toLong() }
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Swipe to see more colors",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            1 -> { // NOTE DESCRIPTION
                                OutlinedTextField(
                                    value = noteContent,
                                    onValueChange = { noteContent = it; errorMessage = null },
                                    placeholder = { Text("Compile content parameters, details or links inside canvas...") },
                                    maxLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                            2 -> { // TASK SCHEDULER
                                Text("Task Priority Category", style = MaterialTheme.typography.titleSmall)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val cats = listOf("Daily Task", "Reminder")
                                    cats.forEach { cat ->
                                        val isSel = taskCategory == cat
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { 
                                                    taskCategory = cat 
                                                    errorMessage = null 
                                                }
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = cat,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Text("June Schedule Day (1-30)", style = MaterialTheme.typography.titleSmall)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = taskDay.toFloat(),
                                        onValueChange = { taskDay = it.toInt(); errorMessage = null },
                                        valueRange = 1f..30f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Day $taskDay",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }

                                if (taskCategory == "Reminder") {
                                    Text("Configure Target Time for Reminder", style = MaterialTheme.typography.titleSmall)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Hour: $reminderHour hh", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                            Slider(
                                                value = reminderHour.toFloat(),
                                                onValueChange = { reminderHour = String.format("%02d", it.toInt()); errorMessage = null },
                                                valueRange = 0f..23f
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Min: $reminderMinute mm", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                            Slider(
                                                value = reminderMinute.toFloat(),
                                                onValueChange = { reminderMinute = String.format("%02d", it.toInt()); errorMessage = null },
                                                valueRange = 0f..59f
                                            )
                                         }
                                    }
                                }
                            }
                            3 -> { // FINANCE TRANSACTION
                                OutlinedTextField(
                                    value = financeAmountText,
                                    onValueChange = { financeAmountText = it; errorMessage = null },
                                    label = { Text("Amount (₹)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )

                                Text("Commitment Category", style = MaterialTheme.typography.titleSmall)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val types = listOf("EMI", "Debt", "Subscription", "Friend Borrow")
                                    types.forEach { type ->
                                        val isSel = financeType == type
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { 
                                                    financeType = type 
                                                    errorMessage = null
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = type,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                if (financeType == "Friend Borrow") {
                                    OutlinedTextField(
                                        value = friendName,
                                        onValueChange = { friendName = it; errorMessage = null },
                                        label = { Text("Friend's Name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )

                                    OutlinedTextField(
                                        value = viaPayment,
                                        onValueChange = { viaPayment = it; errorMessage = null },
                                        label = { Text("Via Payment Method (e.g. UPI, cash)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Repay Needed?", style = MaterialTheme.typography.titleSmall)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            listOf(true to "Repay (YES)", false to "No Repay (NO)").forEach { (value, label) ->
                                                val isSelected = repayNeeded == value
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) Color(0xFFFF1E1E) else MaterialTheme.colorScheme.surfaceVariant)
                                                        .clickable { repayNeeded = value }
                                                        .padding(vertical = 12.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Text("Due Day of Month (1-30)", style = MaterialTheme.typography.titleSmall)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = financeDay.toFloat(),
                                        onValueChange = { financeDay = it.toInt(); errorMessage = null },
                                        valueRange = 1f..30f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Day $financeDay",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }

                        // Intelligent Duplicate Validation Warning Screen Banner
                        if (errorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.15f)),
                                border = BorderStroke(1.2.dp, Color(0xFFFF5252)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Duplicate Validation Fault",
                                        tint = Color(0xFFFF4D4D)
                                    )
                                    Text(
                                        text = errorMessage!!,
                                        color = Color(0xFFFF4D4D),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
 
                        // Action Buttons - Symmetrical Alignment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Cancel", style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (titleText.isNotBlank()) {
                                        errorMessage = null
                                        val trimmedTitle = titleText.trim()

                                        when (selectedTab) {
                                            0 -> { // EVENT CONFIGS
                                                val finalTitle = when (eventCategory) {
                                                    "Birthdays" -> {
                                                        val base = if (!trimmedTitle.contains("🎂")) "$trimmedTitle 🎂" else trimmedTitle
                                                        "$base ($birthdayMonth $eventDay)"
                                                    }
                                                    "Reminder" -> {
                                                        val base = if (!trimmedTitle.contains("⏰")) "$trimmedTitle ⏰" else trimmedTitle
                                                        "$base at $reminderHour:$reminderMinute"
                                                    }
                                                    else -> trimmedTitle
                                                }

                                                // Perform Intelligent Duplicate Detection scan
                                                val isDuplicate = events.any {
                                                    it.title.equals(finalTitle, ignoreCase = true) &&
                                                    it.category.equals(eventCategory, ignoreCase = true) &&
                                                    it.timestamp == eventDay.toLong()
                                                }

                                                if (isDuplicate) {
                                                    errorMessage = "⚠️ [Duplicate Entry]: '$finalTitle' already recorded in June $eventDay."
                                                    onTriggerNotification("Duplicate Entry Halted ⚠️", "An exact twin title/type already exists on this timeline.")
                                                    return@Button
                                                }

                                                viewModel.addEvent(finalTitle, selectedColorLong, eventCategory, eventDay.toLong())
                                                onTriggerNotification("Milestone Tracked 📅", "Successfully logged $eventCategory: '$finalTitle' for June $eventDay.")
                                            }
                                            1 -> { // NOTE DESCRIPTION
                                                // Scan existing notes for duplicate title
                                                val isDuplicate = notes.any {
                                                    it.title.equals(trimmedTitle, ignoreCase = true)
                                                }

                                                if (isDuplicate) {
                                                    errorMessage = "⚠️ [Duplicate Entry]: A note styled '$trimmedTitle' already exists."
                                                    onTriggerNotification("Duplicate Entry Halted ⚠️", "A Canvas Log with this title already exists.")
                                                    return@Button
                                                }

                                                viewModel.addNote(trimmedTitle, noteContent)
                                                onTriggerNotification("Canvas Log Bound 📝", "Successfully compiled text log: '$trimmedTitle' onto personal workspace.")
                                            }
                                            2 -> { // TASK SCHEDULER
                                                val finalTitle = if (taskCategory == "Reminder") {
                                                    if (!trimmedTitle.contains("⏰")) "$trimmedTitle ⏰" else trimmedTitle
                                                } else {
                                                    trimmedTitle
                                                }
                                                val targetDayStr = if (taskCategory == "Reminder") {
                                                    "June $taskDay at $reminderHour:$reminderMinute"
                                                } else {
                                                    "June $taskDay"
                                                }

                                                // Scan existing tasks for duplicate
                                                val isDuplicate = tasks.any {
                                                    it.title.equals(finalTitle, ignoreCase = true) &&
                                                    it.category.equals(taskCategory, ignoreCase = true) &&
                                                    it.dateString.equals(targetDayStr, ignoreCase = true)
                                                }

                                                if (isDuplicate) {
                                                    errorMessage = "⚠️ [Duplicate Entry]: '$finalTitle' already scheduled on $targetDayStr."
                                                    onTriggerNotification("Duplicate Entry Halted ⚠️", "An identical Task is already listed on this timeline.")
                                                    return@Button
                                                }

                                                viewModel.addTask(finalTitle, taskCategory, targetDayStr)
                                                onTriggerNotification("Task Scheduled 🎯", "Assigned $taskCategory: '$finalTitle' due $targetDayStr.")
                                            }
                                            3 -> { // FINANCE TRANSACTION
                                                val amt = financeAmountText.toDoubleOrNull() ?: 0.0
                                                
                                                // Scan existing finances for duplicate
                                                val isDuplicate = finances.any {
                                                    it.title.equals(trimmedTitle, ignoreCase = true) &&
                                                    it.type.equals(financeType, ignoreCase = true) &&
                                                    it.dueDate.equals(financeDay.toString(), ignoreCase = true)
                                                }

                                                if (isDuplicate) {
                                                    errorMessage = "⚠️ [Duplicate Entry]: Finance commitment '$trimmedTitle' already due on Day $financeDay."
                                                    onTriggerNotification("Duplicate Entry Halted ⚠️", "An identical payout commitment already exists on Day $financeDay.")
                                                    return@Button
                                                }

                                                if (financeType == "Friend Borrow") {
                                                    viewModel.addFinancialItem(
                                                        title = trimmedTitle,
                                                        amount = amt,
                                                        type = financeType,
                                                        dueDate = financeDay.toString(),
                                                        friendName = friendName.ifBlank { "Friend" },
                                                        viaPayment = viaPayment.ifBlank { "UPI" },
                                                        needsRepay = repayNeeded
                                                    )
                                                    onTriggerNotification(
                                                        "Friend Lending Tracked 🤝",
                                                        "Logged ₹${String.format("%.0f", amt)} borrowed from ${friendName.ifBlank { "Friend" }} via ${viaPayment.ifBlank { "UPI" }}."
                                                    )
                                                } else {
                                                    viewModel.addFinancialItem(trimmedTitle, amt, financeType, financeDay.toString())
                                                    onTriggerNotification("EMI logged successfully 💸", "Outstanding payout registered: '$trimmedTitle' (₹${String.format("%.0f", amt)}) due June $financeDay.")
                                                }
                                            }
                                        }
                                        onDismiss()
                                    }
                                },
                                enabled = titleText.isNotBlank(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Parameters", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// INTEGRATED SLEEK NAVIGATION BAR CORES
// ==========================================
@Composable
fun FloatingTabBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)), // Deep space slate back
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            border = BorderStroke(1.2.dp, Color(0xFFFF1E1E).copy(alpha = 0.35f)), // Glowing Red outline matching theme
            modifier = Modifier.fillMaxWidth(0.96f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TAB 1: HOME
                TabIconCompact(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = currentTab == AppTab.HOME,
                    onClick = { onTabSelected(AppTab.HOME) },
                    modifier = Modifier.weight(1f)
                )

                // TAB 2: CALENDAR
                TabIconCompact(
                    icon = Icons.Default.CalendarMonth,
                    label = "Calendar",
                    isSelected = currentTab == AppTab.CALENDAR,
                    onClick = { onTabSelected(AppTab.CALENDAR) },
                    modifier = Modifier.weight(1f)
                )

                // CENTRAL "+" BUTTON - GENTLE INFINITE PULSE
                val pulseScale by rememberInfiniteTransition(label = "pulse_trans").animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_scale"
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .scale(pulseScale)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFF1E1E), Color(0xFFFF5252))
                            )
                        )
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Entry",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // TAB 3: FINANCES
                TabIconCompact(
                    icon = Icons.Default.Payments,
                    label = "Finances",
                    isSelected = currentTab == AppTab.FINANCES,
                    onClick = { onTabSelected(AppTab.FINANCES) },
                    modifier = Modifier.weight(1f)
                )

                // TAB 4: CANVAS NOTES
                TabIconCompact(
                    icon = Icons.Default.Description,
                    label = "Canvas",
                    isSelected = currentTab == AppTab.NOTES,
                    onClick = { onTabSelected(AppTab.NOTES) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TabIconCompact(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tab_scale"
    )
    val tintColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFF4D4D) else Color(0xFF8E8E93),
        animationSpec = tween(durationMillis = 200),
        label = "tab_tint"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFF1E1E).copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "tab_container"
    )

    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                ),
                color = tintColor
            )
        }
    }
}

@Composable
fun EmptyListCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

fun safeColorFromLong(colorHex: Long): Color {
    val uLongVal = colorHex.toULong()
    return if ((uLongVal and 0xFFFFFFFF00000000UL) == 0UL) {
        Color(colorHex)
    } else {
        Color(uLongVal)
    }
}

// ==========================================
// HEADING-UP BRAND LOCALIZED SYSTEM ALERT BANNER
// ==========================================
@Composable
fun HeadsUpNotificationBanner(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Notifications,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(top = 16.dp, start = 8.dp, end = 8.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)), // Deep space pitch black
        border = BorderStroke(1.5.dp, Color(0xFFFF1E1E).copy(alpha = 0.5f)) // Beautiful glowing red outline
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFF1E1E).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Banner",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==========================================
// INDIA ROUTINES REMINDER PANEL
// ==========================================
@Composable
fun IndiaRoutinesRemindersPanel(
    onTriggerNotification: (String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🇮🇳",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "India Routine Reminders (IST)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap any localized card to schedule instant testing alerts:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Sub-grid of localized items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Item 1: Broadband & Fiber Payouts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .clickable {
                            onTriggerNotification(
                                "Broadband Refill Alert 🌐",
                                "[JioFiber / Airtel Xstream] Bill payment for ₹825 is due soon (IST)."
                            )
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Color(0xFF1E88E5),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Jio/Airtel Fiber Refill",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Estimated Budget: ₹825 (Monthly)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Item 2: Indian Tatkal Ticket Alert
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .clickable {
                            onTriggerNotification(
                                "Tatkal Alert (IRCTC) 🎫",
                                "Booking window for your selected AC train ticket opens tomorrow morning at 10:00 AM IST!"
                            )
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Train,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Tatkal Reservation (10 AM)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "AC booking window opens soon",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Item 3: LPG Cylinder booking
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .clickable {
                            onTriggerNotification(
                                "LPG Gas Cylinder Refill ⛽",
                                "[HP Gas / Indane] Order prompt: schedule cylinder booking today to avoid shipping delay."
                            )
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = Color(0xFFFB8C00),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LPG Cylinder Refill prompt",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Schedule HP/Indane booking reminder (IST)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
