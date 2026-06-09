package com.example.ui

import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.items
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

// Custom Extension for dotted boundary present day effect
fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp = 12.dp
) = this.drawBehind {
    val strokeWidth = width.toPx()
    val r = cornerRadius.toPx()
    val stroke = Stroke(
        width = strokeWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
    )
}

enum class AppTab {
    HOME, CALENDAR, FINANCES, PROFILE
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

    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val essentialNotificationsOnly by viewModel.essentialNotificationsOnly.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permission given, notifications will post smoothly
            }
        }
    )

    LaunchedEffect(notificationsEnabled) {
        if (notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var currentTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var initialDialogTab by rememberSaveable { mutableStateOf(0) }
    var initialDialogDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSettingsOverlay by rememberSaveable { mutableStateOf(false) }
    var activeNotification by remember { mutableStateOf<InAppNotificationData?>(null) }

    // Auto-Hiding Floating Navigation Dock: automatically slips away when scrolling down, glides back when scrolling upward
    var isFloatingTabBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15f) {
                    isFloatingTabBarVisible = false
                } else if (available.y > 15f) {
                    isFloatingTabBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    // Clean, modern, snappy opening sequence (no diagnostic backend-process logs)
    var showBootSequence by rememberSaveable { mutableStateOf(true) }
    var bootAlpha by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000)
        bootAlpha = 0f
        kotlinx.coroutines.delay(200)
        showBootSequence = false
    }

    val triggerNotification = remember(notificationsEnabled, essentialNotificationsOnly) {
        { title: String, message: String ->
            if (notificationsEnabled) {
                val isReminderOrUpcomingOrSecurity = title.contains("Reminder", ignoreCase = true) ||
                        title.contains("Upcoming", ignoreCase = true) ||
                        title.contains("Welcome", ignoreCase = true) ||
                        title.contains("Namaste", ignoreCase = true) ||
                        title.contains("Alert", ignoreCase = true) ||
                        title.contains("Upcoming Timeline", ignoreCase = true) ||
                        title.contains("Sync", ignoreCase = true) ||
                        title.contains("Updated", ignoreCase = true) ||
                        title.contains("Status", ignoreCase = true) ||
                        title.contains("Change", ignoreCase = true) ||
                        title.contains("Deleted", ignoreCase = true) ||
                        title.contains("Purged", ignoreCase = true) ||
                        title.contains("Task", ignoreCase = true) ||
                        title.contains("Scheduled", ignoreCase = true) ||
                        title.contains("Birthday", ignoreCase = true) ||
                        title.contains("Schedule", ignoreCase = true) ||
                        title.contains("EMI", ignoreCase = true) ||
                        title.contains("Lending", ignoreCase = true) ||
                        title.contains("Milestone", ignoreCase = true) ||
                        title.contains("Tracked", ignoreCase = true) ||
                        title.contains("Canvas Log", ignoreCase = true) ||
                        message.contains("Reminder", ignoreCase = true) ||
                        message.contains("Upcoming", ignoreCase = true) ||
                        title.contains("Security", ignoreCase = true) ||
                        title.contains("Validation", ignoreCase = true)

                val shouldShow = if (essentialNotificationsOnly) {
                    isReminderOrUpcomingOrSecurity
                } else {
                    true
                }

                if (shouldShow) {
                    activeNotification = InAppNotificationData(title, message)
                    NotificationHelper.showNotification(context, title, message)
                }
            }
        }
    }

    // Trigger local welcoming context for India dwellers (essential notification)
    LaunchedEffect(Unit) {
        triggerNotification(
            "Namaste & Welcome! 🇮🇳",
            "Personal OS is running on Indian Standard Time. Settle bills using Indian Rupee (₹) equivalents."
        )
    }

    var hasRunDailyCheck by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(tasks, events, finances) {
        if (!hasRunDailyCheck && (tasks.isNotEmpty() || events.isNotEmpty() || finances.isNotEmpty())) {
            hasRunDailyCheck = true
            val todayCalendar = java.util.Calendar.getInstance()
            val todayDay = todayCalendar.get(java.util.Calendar.DAY_OF_MONTH)
            val todayMonthIndex = todayCalendar.get(java.util.Calendar.MONTH) // 0-indexed
            val monthNames = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            val todayMonthName = monthNames[todayMonthIndex]

            // 1. Birthdays due today
            val birthdaysToday = events.filter {
                it.category.equals("Birthdays", ignoreCase = true) &&
                (it.timestamp == todayDay.toLong() || it.title.contains("$todayMonthName $todayDay", ignoreCase = true) || it.title.contains(todayMonthName, ignoreCase = true))
            }
            birthdaysToday.forEach { bday ->
                triggerNotification(
                    "Birthday Alert 🎂 Within OS",
                    "Remember to celebrate: ${bday.title} on this fine day!"
                )
            }

            // 2. Tasks due today
            val tasksToday = tasks.filter {
                !it.isCompleted && (
                    it.dateString.contains("$todayMonthName $todayDay", ignoreCase = true) ||
                    it.dateString.contains(todayMonthName, ignoreCase = true) ||
                    it.dateString.equals("Daily", ignoreCase = true)
                )
            }
            if (tasksToday.isNotEmpty()) {
                val taskSample = tasksToday.first().title
                val remainingCount = tasksToday.size - 1
                val messageText = if (remainingCount > 0) {
                    "Pending agenda item: '$taskSample' plus $remainingCount other task(s) today."
                } else {
                    "Pending agenda item: '$taskSample' is scheduled for today."
                }
                triggerNotification("Today's Task Agenda 🎯", messageText)
            }

            // 3. Outstanding finances due today (e.g., day index matches today)
            val financesToday = finances.filter {
                !it.isPaid && (
                    it.dueDate == todayDay.toString() ||
                    it.dueDate.contains("$todayDay", ignoreCase = true)
                )
            }
            financesToday.forEach { bill ->
                triggerNotification(
                    "Outstanding Bill Alert 💸",
                    "Payout commitment of ₹${String.format("%.0f", bill.amount)} for '${bill.title}' is due today."
                )
            }
        }
    }

    // Auto-dismiss inside-app heads up alerts
    LaunchedEffect(activeNotification) {
        if (activeNotification != null) {
            kotlinx.coroutines.delay(4500)
            activeNotification = null
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = isFloatingTabBarVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                // Integrated premium central '+' bar
                FloatingTabBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    onAddClick = { showDialog = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val slideDirection = if (targetState.ordinal > initialState.ordinal) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                    slideIntoContainer(
                        towards = slideDirection,
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    ) togetherWith slideOutOfContainer(
                        towards = slideDirection,
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    )
                },
                label = "tab_transition",
                modifier = Modifier.fillMaxSize()
            ) { targetTab ->
                when (targetTab) {
                    AppTab.HOME -> HomeScreen(
                        viewModel = viewModel,
                        notes = notes,
                        events = events,
                        tasks = tasks,
                        finances = finances,
                        onNavigateToFinances = { currentTab = AppTab.FINANCES },
                        onNavigateToCalendar = { currentTab = AppTab.CALENDAR },
                        onNavigateToProfile = { currentTab = AppTab.PROFILE },
                        onTriggerNotification = triggerNotification,
                        onOpenSettings = { showSettingsOverlay = true }
                    )
                    AppTab.CALENDAR -> CalendarScreen(
                        viewModel = viewModel,
                        events = events,
                        tasks = tasks,
                        finances = finances,
                        onTriggerNotification = triggerNotification,
                        onAddClick = { tab, day ->
                            initialDialogTab = tab
                            initialDialogDay = day
                            showDialog = true
                        }
                    )
                    AppTab.FINANCES -> FinancesScreen(
                        viewModel = viewModel,
                        finances = finances,
                        onTriggerNotification = triggerNotification,
                        onAddFinanceClick = {
                            initialDialogTab = 3
                            initialDialogDay = null
                            showDialog = true
                        }
                    )
                    AppTab.PROFILE -> ProfileScreen(
                        viewModel = viewModel,
                        events = events,
                        tasks = tasks,
                        finances = finances,
                        onOpenSettings = { showSettingsOverlay = true },
                        onTriggerNotification = triggerNotification
                    )
                }
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
                        .background(Color(0xFF0F0F0F).copy(alpha = bootAlpha))
                        .clickable(enabled = false) {} // Prevent click-through closing
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        // Styled pulsing modern loader
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF1E1E),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Text(
                            text = "Personal OS Syncing",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Settle bills, track milestones & organize daily routines.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
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
            },
            initialTab = initialDialogTab,
            initialDay = initialDialogDay
        )
    }

    if (showSettingsOverlay) {
        SettingsOverlayDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsOverlay = false },
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
fun NoteReaderDialog(
    note: Note,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A8BFF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    notes: List<Note>,
    events: List<CalendarEvent>,
    tasks: List<Task>,
    finances: List<FinancialItem>,
    onNavigateToFinances: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onTriggerNotification: (String, String) -> Unit,
    onOpenSettings: () -> Unit
) {
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
    var showBirthdaysPopup by remember { mutableStateOf(false) }

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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$greeting, Organizer",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Your Personal OS dashboard is compiled (IST) 🇮🇳",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        value = "$unfinishedTasks Active",
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

        // Segment 1: Daily Action Items & Tasks (Add & Delete)
        item {
            var quickTaskText by rememberSaveable { mutableStateOf("") }
            AnimatedVisibility(
                visible = visible2,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Daily Action Items & Tasks",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Log tasks to tick off or purge as they complete.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Quick Add Row
                        OutlinedTextField(
                            value = quickTaskText,
                            onValueChange = { quickTaskText = it },
                            placeholder = { Text("What needs to be done?", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) },
                            singleLine = true,
                            trailingIcon = {
                                if (quickTaskText.isNotBlank()) {
                                    IconButton(onClick = {
                                        viewModel.addTask(quickTaskText, "Daily Task", "June 1")
                                        onTriggerNotification("Task Scheduled 🎯", "Assigned Daily Task: '$quickTaskText' due June 1.")
                                        quickTaskText = ""
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color(0xFF4A8BFF))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4A8BFF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        if (tasks.isEmpty()) {
                            Text(
                                text = "Zero active agenda items. Create one above!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                tasks.take(5).forEach { task ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF141414))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Toggle task",
                                            tint = if (task.isCompleted) MintGreen else Color.Gray,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clickable { 
                                                    viewModel.toggleTaskStatus(task) 
                                                    val stateLabel = if (task.isCompleted) "Reopened" else "Completed"
                                                    onTriggerNotification("Task Status Changed 🔄", "Task '${task.title}' was marked as $stateLabel.")
                                                }
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            color = if (task.isCompleted) Color.Gray else Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteTask(task)
                                                onTriggerNotification("Task Deleted 🗑️", "Task '${task.title}' has been deleted.")
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFFF5252).copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
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

        // Segment 2: Organized Glassmorphic Workspace Grid
        item {
            AnimatedVisibility(
                visible = visible3,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
                exit = fadeOut()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Workspace & Planner",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        TextButton(onClick = onNavigateToCalendar) {
                            Text("Calendar View", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4A8BFF))
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Container A: Glassmorphic Scribbles & Notes (Full Width)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111).copy(alpha = 0.65f)),
                            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color(0xFF4A8BFF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Canvas Notes & Scribbles",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                if (notes.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Empty scribble pad.\nCreate a note using the '+' below.",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.heightIn(max = 160.dp)
                                    ) {
                                        items(notes.take(4)) { note ->
                                            var showNoteReader by remember { mutableStateOf(false) }
                                            if (showNoteReader) {
                                                MarkdownNoteEditorDialog(
                                                    note = note,
                                                    viewModel = viewModel,
                                                    onDismiss = { showNoteReader = false },
                                                    onTriggerNotification = onTriggerNotification
                                                )
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.White.copy(alpha = 0.05f))
                                                    .clickable { showNoteReader = true }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = note.title,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = note.content,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.Gray,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { 
                                                        viewModel.deleteNote(note) 
                                                        onTriggerNotification("Note Purged 🗑️", "Note '${note.title}' has been deleted from personal workspace.")
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = Color.White.copy(alpha = 0.4f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                                   // Container B: Birthdays Card (Home Screen Quick-View Card)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBirthdaysPopup = true },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F14)),
                            border = BorderStroke(1.5.dp, SoftPink.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SoftPink.copy(alpha = 0.15f))
                                            .border(1.dp, SoftPink.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cake,
                                            contentDescription = "Birthdays Logo",
                                            tint = SoftPink,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Birthdays",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Tap to view all upcoming celebrations.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Open",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Container C: Spacious Upcoming Events (Cleaned from unwanted texts & details for grand space)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0F)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "Upcoming Timeline & Events",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                    val count = events.size
                                    Text(
                                        text = "$count Total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }

                                val sortedEvents = remember(events) {
                                    events.sortedBy { it.timestamp }
                                }

                                if (sortedEvents.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.EventNote,
                                                contentDescription = null,
                                                tint = Color.Gray.copy(alpha = 0.5f),
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Timeline clear. Add events in the Calendar tab.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        sortedEvents.take(4).forEach { event ->
                                            val isBday = event.category.equals("birthdays", ignoreCase = true) || event.category.equals("birthday", ignoreCase = true)
                                            val badgeColor = if (isBday) SoftPink else safeColorFromLong(event.colorHex)

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF16161D)),
                                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(badgeColor.copy(alpha = 0.15f))
                                                            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(
                                                                text = "JUNE",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                                                color = badgeColor
                                                            )
                                                            Text(
                                                                text = event.timestamp.toString(),
                                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.width(14.dp))
                                                    
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = event.title,
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = Color.White
                                                        )
                                                        Text(
                                                            text = if (isBday) "Birthday Celebration 🎂" else "${event.category} Milestone",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Upcoming Birthdays Popup Window Dialog (Requirement 2)
                        if (showBirthdaysPopup) {
                            AlertDialog(
                                onDismissRequest = { showBirthdaysPopup = false },
                                title = {
                                    Text(
                                        text = "Upcoming Birthdays",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                },
                                text = {
                                    val bdaysList = remember(events) {
                                        events.filter { it.category.equals("birthdays", ignoreCase = true) || it.category.equals("birthday", ignoreCase = true) }
                                            .sortedBy { it.timestamp }
                                    }
                                    if (bdaysList.isEmpty()) {
                                        Text(
                                            text = "No upcoming birthdays found. Add them in the Calendar tab!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                                        ) {
                                            items(bdaysList) { event ->
                                                val dayWithSuffix = getDayWithSuffix(event.timestamp.toInt())
                                                val displayName = event.title.replace("🎂", "").trim()
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFF16161D), RoundedCornerShape(12.dp))
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(SoftPink.copy(alpha = 0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("🎂", fontSize = 16.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    val titleStr = event.title
                                                    val dateString = if (titleStr.contains("(") && titleStr.contains(")")) {
                                                        titleStr.substringAfter("(").substringBefore(")")
                                                    } else {
                                                        "June ${event.timestamp}"
                                                    }
                                                    val cleanName = titleStr
                                                        .replace("🎂", "")
                                                        .replace(Regex("\\(.*\\)"), "")
                                                        .trim()
                                                    Text(
                                                        text = "$cleanName — $dateString",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { showBirthdaysPopup = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(text = "Close View", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                },
                                containerColor = Color(0xFF0F0F14),
                                titleContentColor = Color.White,
                                textContentColor = Color.White
                            )
                        }                        }
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
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (item.isPaid) MintGreen.copy(alpha = 0.12f) else Color(0xFFFF4D4D).copy(alpha = 0.12f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (item.isPaid) "PAID ✅" else "UNPAID ⏳",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (item.isPaid) MintGreen else Color(0xFFFF4D4D)
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
    onTriggerNotification: (String, String) -> Unit,
    onAddClick: ((Int, Int?) -> Unit)? = null
) {
    var selectedDay by rememberSaveable { mutableStateOf<Int?>(1) }
    var currentYear by rememberSaveable { mutableStateOf(2026) }
    var currentMonth by rememberSaveable { mutableStateOf(5) } // June = 5 (0-indexed)
    var calendarViewMode by rememberSaveable { mutableStateOf(0) } // 0 = Month, 1 = Day

    val todayCalendar = remember { java.util.Calendar.getInstance() }
    val todayYear = todayCalendar.get(java.util.Calendar.YEAR)
    val todayMonth = todayCalendar.get(java.util.Calendar.MONTH) // 0-indexed
    val todayDay = todayCalendar.get(java.util.Calendar.DAY_OF_MONTH)

    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Calculate days count and offset dynamically
    val cal = remember(currentYear, currentMonth) {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, currentYear)
            set(java.util.Calendar.MONTH, currentMonth)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
    }
    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val firstDayOfWeekRaw = cal.get(java.util.Calendar.DAY_OF_WEEK) // Sunday = 1, Mon = 2 ...
    val firstDayOffset = when (firstDayOfWeekRaw) {
        java.util.Calendar.MONDAY -> 0
        java.util.Calendar.TUESDAY -> 1
        java.util.Calendar.WEDNESDAY -> 2
        java.util.Calendar.THURSDAY -> 3
        java.util.Calendar.FRIDAY -> 4
        java.util.Calendar.SATURDAY -> 5
        java.util.Calendar.SUNDAY -> 6
        else -> 0
    }

    LaunchedEffect(daysInMonth) {
        if (selectedDay != null && selectedDay!! > daysInMonth) {
            selectedDay = daysInMonth
        }
    }

    var showMonthYearPickerDialog by remember { mutableStateOf(false) }

    if (showMonthYearPickerDialog) {
        MonthYearPickerDialog(
            currentMonth = currentMonth,
            currentYear = currentYear,
            onDismiss = { showMonthYearPickerDialog = false },
            onSelect = { m, y ->
                currentMonth = m
                currentYear = y
                selectedDay = 1
                showMonthYearPickerDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Perfect, centered Arrow align structure
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentMonth == 0) {
                                currentMonth = 11
                                currentYear -= 1
                            } else {
                                currentMonth -= 1
                            }
                            selectedDay = 1
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF141414), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Clickable title dropdown capsule & sync button in center row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(Color(0xFF0F0F0F))
                                .border(1.2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(30.dp))
                                .clickable { showMonthYearPickerDialog = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${monthNames[currentMonth]} $currentYear",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Choose month",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Single-Button Sync Option
                        var isSyncingCal by remember { mutableStateOf(false) }
                        val scopeCal = rememberCoroutineScope()
                        IconButton(
                            onClick = {
                                isSyncingCal = true
                                scopeCal.launch {
                                    kotlinx.coroutines.delay(1200)
                                    viewModel.syncCalendarFromOthers { count ->
                                        isSyncingCal = false
                                        onTriggerNotification(
                                            "Calendar Synced 🛰",
                                            if (count > 0) "$count external items integrated successfully into database." else "No new events found. Up to date."
                                        )
                                    }
                                }
                            },
                            enabled = !isSyncingCal,
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF141414), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        ) {
                            if (isSyncingCal) {
                                CircularProgressIndicator(color = Color(0xFF00FFCC), modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync Calendar",
                                    tint = Color(0xFF00FFCC),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            if (currentMonth == 11) {
                                currentMonth = 0
                                currentYear += 1
                            } else {
                                currentMonth += 1
                            }
                            selectedDay = 1
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF141414), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Centered beautifully-styled Month/Day View selector Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF111111))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (calendarViewMode == 0) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { calendarViewMode = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Month View",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (calendarViewMode == 0) Color.White else Color.Gray
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (calendarViewMode == 1) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { calendarViewMode = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Day View",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (calendarViewMode == 1) Color.White else Color.Gray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Calendar Grid System
        item {
            AnimatedContent(
                targetState = calendarViewMode,
                transitionSpec = {
                    val slideDirection = if (targetState > initialState) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                    slideIntoContainer(
                        towards = slideDirection,
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                    ) togetherWith slideOutOfContainer(
                        towards = slideDirection,
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                    )
                },
                label = "calendar_view_slide_transition",
                modifier = Modifier.fillMaxWidth()
            ) { viewMode ->
                if (viewMode == 0) {
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
                                        color = if (isWeekend) Color(0xFFFFB399) else Color.Gray // Soft peach for weekend header
                                    )
                                }
                            }

                            // Days Grid Creator
                            val totalSlots = daysInMonth + firstDayOffset
                            val rowsCount = (totalSlots + 6) / 7

                            for (row in 0 until rowsCount) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (col in 0..6) {
                                        val slotIndex = row * 7 + col
                                        if (slotIndex >= firstDayOffset && slotIndex < totalSlots) {
                                            val day = slotIndex - firstDayOffset + 1
                                            val isSelected = selectedDay == day

                                            // Filter events/dues at exact integer index
                                            val dayEvents = events.filter { it.timestamp == day.toLong() }
                                            val dayFinances = finances.filter {
                                                it.dueDate == day.toString() || it.dueDate.contains("${monthNames[currentMonth]} $day") || (it.dueDate.filter { c -> c.isDigit() } == day.toString())
                                            }

                                            val hasEvent = dayEvents.isNotEmpty()
                                            val hasFinance = dayFinances.isNotEmpty()

                                            // Dynamic Color Mapping Rules
                                            val cellBgColor = when {
                                                isSelected -> Color.White.copy(alpha = 0.16f)
                                                hasEvent -> safeColorFromLong(dayEvents.first().colorHex).copy(alpha = 0.45f)
                                                hasFinance -> Color(0xFFFF9933).copy(alpha = 0.15f)
                                                else -> Color(0xFF141414)
                                            }

                                            val cellBorderColor = when {
                                                isSelected -> Color.White
                                                hasEvent -> safeColorFromLong(dayEvents.first().colorHex)
                                                else -> Color.White.copy(alpha = 0.04f)
                                            }

                                            val isWeekend = col >= 5
                                            val textWeight = if (isSelected || hasEvent) FontWeight.Bold else FontWeight.Normal
                                            val textColor = when {
                                                isSelected -> Color.White
                                                hasEvent -> safeColorFromLong(dayEvents.first().colorHex)
                                                isWeekend -> Color(0xFFFFB399)
                                                else -> Color.White
                                            }

                                            val isPresentDay = currentYear == todayYear && currentMonth == todayMonth && day == todayDay
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .padding(3.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(cellBgColor)
                                                    .then(
                                                        if (isPresentDay) {
                                                            Modifier.dashedBorder(
                                                                width = 1.5.dp,
                                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                                                cornerRadius = 12.dp
                                                            )
                                                        } else {
                                                            Modifier.border(
                                                                width = if (isSelected || hasEvent) 1.5.dp else 1.dp,
                                                                color = cellBorderColor,
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                        }
                                                    )
                                                    .clickable { selectedDay = if (selectedDay == day) null else day }
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
                                                                .background(Color(0xFFFF9933))
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
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                        border = BorderStroke(1.5.dp, Color(0xFFFF1E1E).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Timeline Day Stripe 🛰",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            val listState = rememberLazyListState()
                            LaunchedEffect(selectedDay) {
                                selectedDay?.let {
                                    if (it > 0 && it <= daysInMonth) {
                                        listState.animateScrollToItem(maxOf(0, it - 3))
                                    }
                                }
                            }

                            LazyRow(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items((1..daysInMonth).toList()) { day ->
                                    val isSelected = selectedDay == day
                                    val dayEvents = events.filter { it.timestamp == day.toLong() }
                                    val dayFinances = finances.filter {
                                        it.dueDate == day.toString() || it.dueDate.contains("${monthNames[currentMonth]} $day") || (it.dueDate.filter { c -> c.isDigit() } == day.toString())
                                    }
                                    val hasEvent = dayEvents.isNotEmpty()
                                    val hasFinance = dayFinances.isNotEmpty()

                                    val cellBgColor = when {
                                        isSelected -> Color.White.copy(alpha = 0.16f)
                                        hasEvent -> safeColorFromLong(dayEvents.first().colorHex).copy(alpha = 0.45f)
                                        hasFinance -> Color(0xFFFF9933).copy(alpha = 0.15f)
                                        else -> Color(0xFF141414)
                                    }

                                    val cellBorderColor = when {
                                        isSelected -> Color.White
                                        hasEvent -> safeColorFromLong(dayEvents.first().colorHex)
                                        else -> Color.White.copy(alpha = 0.08f)
                                    }

                                    val textWeight = if (isSelected || hasEvent) FontWeight.Bold else FontWeight.Normal
                                    val textColor = if (isSelected || hasEvent) Color.White else Color.Gray

                                    Box(
                                        modifier = Modifier
                                            .width(55.dp)
                                            .height(75.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(cellBgColor)
                                            .border(
                                                width = if (isSelected || hasEvent) 1.5.dp else 1.dp,
                                                color = cellBorderColor,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable { selectedDay = day }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "Day",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = day.toString(),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = textWeight),
                                                color = textColor
                                            )
                                            if (hasEvent || hasFinance) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(if (hasEvent) safeColorFromLong(dayEvents.first().colorHex) else Color(0xFFFF9933))
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
        }

        // Expanded Selected Day Schedule Itemizer
        item {
            val selectedDayEvents = if (selectedDay != null) events.filter { it.timestamp == selectedDay!!.toLong() } else emptyList()
            val selectedDayFinances = if (selectedDay != null) finances.filter {
                it.dueDate == selectedDay!!.toString() || it.dueDate.contains("${monthNames[currentMonth]} $selectedDay") || (it.dueDate.filter { c -> c.isDigit() } == selectedDay!!.toString())
            } else emptyList()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedDay != null) "Day Schedule: ${monthNames[currentMonth]} $selectedDay" else "Day Schedule: No Day Selected",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    color = Color.White
                )
                if (selectedDay != null && onAddClick != null) {
                    IconButton(
                        onClick = { onAddClick(0, selectedDay) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF141414), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Day Milestone",
                            tint = Color(0xFFFF1E1E)
                        )
                    }
                }
            }

            if (selectedDay == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Tap any day on the calendar above to view scheduling milestones & subscriptions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else if (selectedDayEvents.isEmpty() && selectedDayFinances.isEmpty()) {
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
                                    if (isBirthday) {
                                        if (!event.nakshatram.isNullOrBlank() || !event.rasi.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "✨ Star: ${event.nakshatram ?: "N/A"} • Rasi: ${event.rasi ?: "N/A"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF00FFCC)
                                            )
                                        }
                                        if (!event.timeOfBirth.isNullOrBlank() || !event.bornHospital.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "🏥 ${event.bornHospital ?: "N/A"} @ ${event.timeOfBirth ?: "N/A"}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Color.LightGray
                                            )
                                        }
                                    }
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
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (fin.isPaid) MintGreen.copy(alpha = 0.12f) else Color(0xFFFF3333).copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (fin.isPaid) "Paid ✅" else "Unpaid ⏳",
                                        color = if (fin.isPaid) MintGreen else Color(0xFFFF5252),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
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
    onTriggerNotification: (String, String) -> Unit,
    onAddFinanceClick: (() -> Unit)? = null
) {
    var filterSelected by remember { mutableStateOf("All") }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                }
                if (onAddFinanceClick != null) {
                    IconButton(
                        onClick = onAddFinanceClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF141414), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Bill / Debt",
                            tint = Color(0xFF4A8BFF)
                        )
                    }
                }
            }
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
                        text = "Monthly Balance Summary (IST) 🇮🇳",
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
                                if (isSelected) Color(0xFF4A8BFF)
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

        // Categorize order wise
        if (finances.isEmpty()) {
            item {
                EmptyListCard("No financial records stored.")
            }
        } else {
            val categoriesToRender = if (filterSelected == "All") {
                listOf("EMI", "Subscription", "Debt", "Friend Borrow")
            } else {
                listOf(filterSelected)
            }

            categoriesToRender.forEach { categoryName ->
                val categoryItems = finances.filter {
                    it.type.equals(categoryName, ignoreCase = true)
                }.sortedWith(
                    compareBy<FinancialItem> { it.isPaid } // Unpaid commitments first
                        .thenBy { it.dueDate.toIntOrNull() ?: 999 } // Chronological order
                )

                if (categoryItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "• ${categoryName.uppercase()} LISTING",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = when (categoryName) {
                                "EMI" -> Color(0xFFFFB300)
                                "Subscription" -> Color(0xFF4A8BFF)
                                "Debt" -> Color(0xFFFF5252)
                                else -> Color(0xFF00FFCC)
                            },
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(categoryItems) { item ->
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
                                val brandBgColor = when {
                                    isFriendBorrow -> Color(0xFFFB8C00).copy(alpha = 0.15f)
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
                                    else -> Color(0xFF4A8BFF)
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
                                            color = Color(0xFF4A8BFF)
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
                                    Text(
                                        text = "₹${String.format("%.0f", item.amount)}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    TextButton(
                                        onClick = {
                                            if (!isFriendBorrow || item.needsRepay) {
                                                viewModel.toggleFinancialPaidStatus(item)
                                                val nextState = if (item.isPaid) "outstanding" else "completed / paid"
                                                onTriggerNotification(
                                                    "Finance Commitment 🔄",
                                                    "Your commitment '${item.title}' has been marked as $nextState."
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
                                    onTriggerNotification("Bill Log Purged 💸", "Removed Finance Ledger: '${item.title}'.")
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF4D4D).copy(alpha = 0.5f))
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
// SCREEN 4: USER PROFILE SCREEN
// ==========================================
@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    events: List<CalendarEvent>,
    tasks: List<Task>,
    finances: List<FinancialItem>,
    onOpenSettings: () -> Unit,
    onTriggerNotification: (String, String) -> Unit
) {
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val progressPercent = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 100

    val unpaidAmount = finances.filter { !it.isPaid }.sumOf { it.amount }

    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val ownerEmail by viewModel.ownerEmail.collectAsStateWithLifecycle()
    val profileIcon by viewModel.profileIcon.collectAsStateWithLifecycle()

    var nameInput by remember { mutableStateOf(ownerName) }
    var emailInput by remember { mutableStateOf(ownerEmail) }
    var iconInput by remember { mutableStateOf(profileIcon) }

    LaunchedEffect(ownerName, ownerEmail, profileIcon) {
        nameInput = ownerName
        emailInput = ownerEmail
        iconInput = profileIcon
    }

    var showAvatarPickerDialog by remember { mutableStateOf(false) }

    val emojis = listOf("👨‍💻", "👩‍💻", "🧔", "🦊", "🚀", "💻", "🎨", "🌟", "⚡", "🧘", "🤖", "🦁", "🏆", "🦄", "🎯")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Decorative Neon Custom App Header & Cover Gallery
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1C2A4B), Color(0xFF0F172A), Color(0xFF3B1C4B))
                        )
                    )
            ) {
                // Background subtle patterns
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Icon(
                        imageVector = Icons.Default.Adjust,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.size(110.dp)
                    )
                }
                
                Column(modifier = Modifier.padding(18.dp).align(Alignment.BottomStart)) {
                    Text(
                        text = "CONTROL MATRIX",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF00FFCC)
                    )
                    Text(
                        text = "Secure Central Profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                }
            }
        }

        // Section: Clean integrated Profile Configuration form directly inline (acting as asymmetrical overlay)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().offset(y = (-10).dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0E14)), // Slate-dark base
                border = BorderStroke(1.dp, Color(0xFF4A8BFF).copy(alpha = 0.15f)) // subtle cyan glowing border
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { showAvatarPickerDialog = true }
                        ) {
                            // Render Premium Visual Image Avatar (or Emoji) preview directly inline
                            RenderAvatarImage(avatarId = iconInput, sizeDp = 68.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap inside to change",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color(0xFF00FFCC))
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Profile Details & Credentials",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF00FFCC), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "System Operator • India (IST)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Owner Name", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4A8BFF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.White.copy(alpha = 0.01f),
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Email Address", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4A8BFF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.White.copy(alpha = 0.01f),
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Choose Profile Icon Symbol Preset", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Button(
                            onClick = { showAvatarPickerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141822)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Open Premium Designer Avatar Library 🎨", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.updateOwnerName(nameInput)
                            viewModel.updateOwnerEmail(emailInput)
                            viewModel.updateProfileIcon(iconInput)
                            onTriggerNotification(
                                "Profile Updated 👤",
                                "Identity credentials modified successfully in Secure Prefs local storage."
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A8BFF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Profile Settings", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Dialog overlay for beautiful visual designer avatar presets selection (Requirement 3)
        item {
            if (showAvatarPickerDialog) {
                Dialog(onDismissRequest = { showAvatarPickerDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .shadow(12.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F131D)),
                        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Select Profile Image Avatar",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )

                            Text(
                                text = "Choose from premium designer avatar presets to customize your Operator identity.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            // Beautiful visual illustrations preset grid (represented by icons on radial colors)
                            val presetsList = listOf(
                                "avatar_hacker" to "Hacker",
                                "avatar_astronaut" to "Astronaut",
                                "avatar_artist" to "Artist",
                                "avatar_gamer" to "Gamer",
                                "avatar_zen" to "Zen",
                                "avatar_strategist" to "Finances",
                                "avatar_fox" to "Foxy",
                                "avatar_minimalist" to "Minimal"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                presetsList.chunked(4).forEach { rowPresets ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowPresets.forEach { (avatarId, name) ->
                                            val isSelected = iconInput == avatarId
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { iconInput = avatarId }
                                                    .background(
                                                        if (isSelected) Color(0xFF4A8BFF).copy(alpha = 0.15f) else Color.Transparent,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.dp,
                                                        color = if (isSelected) Color(0xFF00FFCC) else Color.Transparent,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(6.dp)
                                            ) {
                                                RenderAvatarImage(avatarId = avatarId, sizeDp = 48.dp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSelected) Color(0xFF00FFCC) else Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                            // Emojis fallback options
                            Text(
                                text = "Or select standard emoji symbol profile:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                items(emojis) { emoji ->
                                    val isSelected = iconInput == emoji
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFF4A8BFF) else Color(0xFF141822))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.1f),
                                                shape = CircleShape
                                            )
                                            .clickable { iconInput = emoji },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 17.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showAvatarPickerDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16161D)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Done Selection", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: System Stats & Achievements (glowing style)
        item {
            Text(
                text = "Live Account Telemetry",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = Color.LightGray,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Task Performance
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Task, contentDescription = null, tint = MintGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Completion Rate", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MintGreen
                        )
                        Text(
                            text = "$completedTasks of $totalTasks checked",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                // Financial Liability
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pending Liability", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "₹${String.format("%.0f", unpaidAmount)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFFFF5252)
                        )
                        Text(
                            text = "Outstanding payments",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Section 1: Account Management (Requirement 4)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10141D)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Account Management",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        color = Color(0xFF00FFCC),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Option 1.1: Personal Information
                    var showPersonalInfo by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPersonalInfo = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF4A8BFF).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF4A8BFF), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Personal Information", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                Text("Manage your account details and profile identity", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }

                    if (showPersonalInfo) {
                        AlertDialog(
                            onDismissRequest = { showPersonalInfo = false },
                            title = { Text("Personal Information", color = Color.White) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Display Identity details initialized securely in local memory:", color = Color.Gray)
                                    Text("• Operator Name: $ownerName", color = Color.White)
                                    Text("• Linked Email: $ownerEmail", color = Color.White)
                                    Text("• System Region: India (IST Standard)", color = Color.White)
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showPersonalInfo = false }) {
                                    Text("Close")
                                }
                            },
                            containerColor = Color(0xFF10141D)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Option 1.2: Security & Password
                    var showSecurityDialog by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSecurityDialog = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Security & Password", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                Text("Credential shield, passcode, and local security vaults", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }

                    if (showSecurityDialog) {
                        AlertDialog(
                            onDismissRequest = { showSecurityDialog = false },
                            title = { Text("Local Security Vault", color = Color.White) },
                            text = {
                                Text("All database logs and sessions are encrypted end-to-end with high entropy local-first keystores. Real-time security state is ACTIVE.", color = Color.Gray)
                            },
                            confirmButton = {
                                Button(onClick = { showSecurityDialog = false }) {
                                    Text("Acknowledged")
                                }
                            },
                            containerColor = Color(0xFF10141D)
                        )
                    }
                }
            }
        }

        // Section 2: Preferences (Requirement 4)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10141D)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Preferences",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        color = Color(0xFF00FFCC),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Option 2.1: Interface Theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTriggerNotification("Visual Theme Locked 🌌", "The AMOLED Cosmic Obsidian theme is dynamically customized.") }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF9C27B0).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Interface Theme", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                Text("Current: Space Cosmic Obsidian Dark Theme", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Option 2.2: Notification Settings
                    var showNotificationSettings by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNotificationSettings = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE91E63).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Notification Settings", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                Text("Manage reminder triggers and alert banners", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }

                    if (showNotificationSettings) {
                        AlertDialog(
                            onDismissRequest = { showNotificationSettings = false },
                            title = { Text("Notification Triggers", color = Color.White) },
                            text = {
                                Text("Banners and triggers are currently linked directly with the device's main system channels for instant local response.", color = Color.Gray)
                            },
                            confirmButton = {
                                Button(onClick = { showNotificationSettings = false }) {
                                    Text("Got It")
                                }
                            },
                            containerColor = Color(0xFF10141D)
                        )
                    }
                }
            }
        }

        // Section 3: Assistance (Requirement 4)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10141D)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Assistance",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        color = Color(0xFF00FFCC),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Option 3.1: Help & Support
                    var showHelpSupport by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHelpSupport = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF00E676).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Help, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Help & Support", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                Text("Browse user guide, system logs and contact dev portal", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }

                    if (showHelpSupport) {
                        AlertDialog(
                            onDismissRequest = { showHelpSupport = false },
                            title = { Text("Support Operator", color = Color.White) },
                            text = {
                                Text("Need help or want to export encrypted backups? Ping us directly at localfirst-support@example.com", color = Color.Gray)
                            },
                            confirmButton = {
                                Button(onClick = { showHelpSupport = false }) {
                                    Text("Close")
                                }
                            },
                            containerColor = Color(0xFF10141D)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Option 3.2: Sign Out
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTriggerNotification("Sign Out Handover 🔒", "All local sessions locked down. Enter master pass to unlock.") }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFF5252).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Sign Out", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                Text("Securely terminate current active Operator session", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
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
    onTriggerNotification: (String, String) -> Unit,
    initialTab: Int = 0,
    initialDay: Int? = null
) {
    var selectedTab by remember { mutableStateOf(initialTab) } // 0: Event, 1: Note, 2: Task, 3: Finance
    
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
    
    // States for optimized reminder date picker
    var reminderMonthDropdownExpanded by remember { mutableStateOf(false) }
    var reminderDateDropdownExpanded by remember { mutableStateOf(false) }
    var selectedReminderMonth by remember { mutableStateOf("June") }
    var selectedReminderDate by remember { mutableStateOf(initialDay ?: 1) }

    // Event configs
    var eventCategory by remember { mutableStateOf("Timeline") }
    var eventDay by remember { mutableStateOf(initialDay ?: 1) }
    var selectedColorLong by remember { mutableStateOf(PastelBlue.value.toLong()) }

    // Note configs
    var noteContent by remember { mutableStateOf("") }

    // Task configs
    var taskCategory by remember { mutableStateOf("Daily Task") }
    var taskDay by remember { mutableStateOf(initialDay ?: 1) }

    // Finance configs
    var financeAmountText by remember { mutableStateOf("") }
    var financeType by remember { mutableStateOf("EMI") }
    var financeDay by remember { mutableStateOf(initialDay ?: 1) }
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
                                                .background(if (isSel) (if (cat == "Birthdays") SoftPink else MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { 
                                                    eventCategory = cat 
                                                    errorMessage = null
                                                    if (cat == "Birthdays") {
                                                        selectedColorLong = SoftPink.value.toLong()
                                                    }
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
                                    
                                    val daysInBirthMonth = when (birthdayMonth) {
                                        "February" -> 28
                                        "April", "June", "September", "November" -> 30
                                        else -> 31
                                    }
                                    if (eventDay > daysInBirthMonth) {
                                        eventDay = daysInBirthMonth
                                    }
                                    Text("Select Exact Day: Day $eventDay", style = MaterialTheme.typography.titleSmall)
                                    CustomGridDatePicker(
                                        selectedDay = eventDay,
                                        maxDays = daysInBirthMonth,
                                        accentColor = SoftPink,
                                        onDaySelected = { 
                                            eventDay = it
                                            errorMessage = null 
                                        }
                                    )
                                } else if (eventCategory == "Reminder") { // Active reminder channel
                                    val monthsList = listOf(
                                        "January", "February", "March", "April", "May", "June",
                                        "July", "August", "September", "October", "November", "December"
                                    )
                                    val daysInReminderMonth = when (selectedReminderMonth) {
                                        "February" -> 28
                                        "April", "June", "September", "November" -> 30
                                        else -> 31
                                    }

                                    Column(
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
                                             .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                             .padding(14.dp),
                                         verticalArrangement = Arrangement.spacedBy(12.dp)
                                     ) {
                                                                 Text(
                                            text = "Add Reminder",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )

                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "Select Month",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )
                                            Box {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFF16161D), RoundedCornerShape(12.dp))
                                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                        .clickable { reminderMonthDropdownExpanded = true }
                                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = selectedReminderMonth, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.Gray)
                                                }
                                                DropdownMenu(
                                                    expanded = reminderMonthDropdownExpanded,
                                                    onDismissRequest = { reminderMonthDropdownExpanded = false },
                                                    modifier = Modifier.background(Color(0xFF16161D))
                                                ) {
                                                    monthsList.forEach { m ->
                                                        DropdownMenuItem(
                                                            text = { Text(text = m, color = Color.White) },
                                                            onClick = {
                                                                selectedReminderMonth = m
                                                                reminderMonthDropdownExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        val actualDaysInMonth = when (selectedReminderMonth) {
                                            "February" -> 28
                                            "April", "June", "September", "November" -> 30
                                            else -> 31
                                        }
                                        if (selectedReminderDate > actualDaysInMonth) {
                                            selectedReminderDate = actualDaysInMonth
                                        }

                                        Text("Select Date: Day $selectedReminderDate", style = MaterialTheme.typography.titleSmall)
                                        CustomGridDatePicker(
                                            selectedDay = selectedReminderDate,
                                            maxDays = actualDaysInMonth,
                                            accentColor = MaterialTheme.colorScheme.primary,
                                            onDaySelected = {
                                                selectedReminderDate = it
                                                errorMessage = null
                                            }
                                        )

                                        // Complete Time Selection Selector for reminders
                                        Text("Time Settings (IST Standard)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
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

                                        Button(
                                            onClick = {
                                                if (titleText.isBlank()) {
                                                    errorMessage = "⚠️ Please fill in the entry title."
                                                    return@Button
                                                }
                                                val trimmedTitle = titleText.trim()
                                                val finalTitle = if (!trimmedTitle.contains("⏰")) "$trimmedTitle ⏰" else trimmedTitle
                                                val fullTitle = "$finalTitle ($selectedReminderMonth $selectedReminderDate) at $reminderHour:$reminderMinute"

                                                val isDuplicate = events.any {
                                                    it.title.equals(fullTitle, ignoreCase = true) &&
                                                    it.category.equals("Reminder", ignoreCase = true) &&
                                                    it.timestamp == selectedReminderDate.toLong()
                                                }

                                                if (isDuplicate) {
                                                    errorMessage = "⚠️ [Duplicate Entry]: '$fullTitle' already recorded."
                                                    onTriggerNotification("Duplicate Entry Halted ⚠️", "An exact twin reminder title already exists.")
                                                    return@Button
                                                }

                                                viewModel.addEvent(fullTitle, selectedColorLong, "Reminder", selectedReminderDate.toLong())
                                                onTriggerNotification("Reminder Tracked 🔔", "Successfully logged Reminder: '$fullTitle'.")
                                                titleText = ""
                                                onDismiss()
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Save Event", color = Color.White, fontWeight = FontWeight.Bold)
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
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = noteContent,
                                        onValueChange = { noteContent = it; errorMessage = null },
                                        placeholder = { Text("Compile content parameters, details or links inside canvas...") },
                                        maxLines = 5,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(115.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                    
                                    if (noteContent.isNotBlank()) {
                                        Text(
                                            text = "Live Markdown Render Preview:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.04f))
                                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                                .verticalScroll(rememberScrollState())
                                                .padding(10.dp)
                                        ) {
                                            RenderMarkdown(markdown = noteContent)
                                        }
                                    }
                                }
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
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.12f)), // Glowing modern outline matching theme
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

                // CENTRAL "+" BUTTON - GENTLE STATIC SCALE FOR RELIABLE RESPONSE
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .scale(1.0f)
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF4A8BFF), Color(0xFF00FFCC)) // Modern neon futuristic gradients
                            )
                        )
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Entry",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
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

                // TAB 4: USER PROFILE
                TabIconCompact(
                    icon = Icons.Default.AccountCircle,
                    label = "Profile",
                    isSelected = currentTab == AppTab.PROFILE,
                    onClick = { onTabSelected(AppTab.PROFILE) },
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
        targetValue = if (isSelected) Color.White else Color(0xFF8E8E93),
        animationSpec = tween(durationMillis = 200),
        label = "tab_tint"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent,
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

// ==========================================
// SCREEN 5: SETTINGS & BACKUP OVERLAY DIALOG (M3 Switch + JSON Portability Blocks)
// ==========================================
@Composable
fun SettingsOverlayDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onTriggerNotification: (String, String) -> Unit
) {
    val events by viewModel.eventsUiState.collectAsStateWithLifecycle()
    val tasks by viewModel.tasksUiState.collectAsStateWithLifecycle()

    var googleCalendarSync by remember { mutableStateOf(true) }
    var notionSync by remember { mutableStateOf(false) }
    var notionDatabaseId by remember { mutableStateOf("db_9x7a2k") }
    var backupInputText by remember { mutableStateOf("") }
    var backupOutputText by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
                        .fillMaxHeight(0.85f)
                        .padding(12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                    border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color(0xFF4A8BFF),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Settings, Sync & Backup",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        // Google Calendar Sync
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Google Calendar Sync", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Text("Auto-exports birth dates & events", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Switch(
                                    checked = googleCalendarSync,
                                    onCheckedChange = { 
                                        googleCalendarSync = it
                                        onTriggerNotification(
                                            "Google Calendar Sync", 
                                            if (it) "Google Calendar Sync enabled." else "Google Calendar Sync disabled."
                                        )
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF4A8BFF), checkedTrackColor = Color(0xFF4A8BFF).copy(alpha = 0.3f))
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (googleCalendarSync) Color(0xFF00FFCC) else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (googleCalendarSync) "Status: Authenticated & Connected [Synced]" else "Status: Inactive/Disconnected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (googleCalendarSync) Color(0xFF00FFCC) else Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        // Notion Integration
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Notion Workspace Sync", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Text("Sinks tasks & entries to Notion canvas", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Switch(
                                    checked = notionSync,
                                    onCheckedChange = { 
                                        notionSync = it
                                        onTriggerNotification(
                                            "Notion Integration", 
                                            if (it) "Workspace sync connected." else "Workspace sync disconnected."
                                        )
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF4A8BFF), checkedTrackColor = Color(0xFF4A8BFF).copy(alpha = 0.3f))
                                )
                            }
                            if (notionSync) {
                                OutlinedTextField(
                                    value = notionDatabaseId,
                                    onValueChange = { notionDatabaseId = it },
                                    label = { Text("Notion Database ID", color = Color.Gray) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4A8BFF),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        // App Notification Controls UI block
                        val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
                        val essentialNotificationsOnly by viewModel.essentialNotificationsOnly.collectAsStateWithLifecycle()

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "App Notification Controls",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                color = Color.White
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("System Notifications", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Text("Show notifications for actions & timeline operations", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = { 
                                        viewModel.updateNotificationsEnabled(it)
                                        onTriggerNotification(
                                            "Notification Settings", 
                                            if (it) "Notifications fully enabled." else "Notifications muted."
                                        )
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF4A8BFF), checkedTrackColor = Color(0xFF4A8BFF).copy(alpha = 0.3f))
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Only Essential Milestones", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Text("Limits notifications strictly to crucial reminders", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Switch(
                                    checked = essentialNotificationsOnly,
                                    onCheckedChange = { 
                                        viewModel.updateEssentialNotificationsOnly(it)
                                        onTriggerNotification(
                                            "Notification Settings", 
                                            if (it) "Essential notification filter active." else "All notifications active."
                                        )
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF4A8BFF), checkedTrackColor = Color(0xFF4A8BFF).copy(alpha = 0.3f))
                                )
                            }
                        }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Backup Section
                Text(
                    text = "Backup & Timeline Portability",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            try {
                                val exportObj = org.json.JSONObject()
                                val eventsArr = org.json.JSONArray()
                                events.forEach { ev ->
                                    val o = org.json.JSONObject()
                                        .put("title", ev.title)
                                        .put("colorHex", ev.colorHex)
                                        .put("category", ev.category)
                                        .put("timestamp", ev.timestamp)
                                        .put("timeOfBirth", ev.timeOfBirth ?: "")
                                        .put("bornHospital", ev.bornHospital ?: "")
                                        .put("nakshatram", ev.nakshatram ?: "")
                                        .put("rasi", ev.rasi ?: "")
                                    eventsArr.put(o)
                                }
                                val tasksArr = org.json.JSONArray()
                                tasks.forEach { t ->
                                    val o = org.json.JSONObject()
                                        .put("title", t.title)
                                        .put("category", t.category)
                                        .put("dateString", t.dateString)
                                        .put("isCompleted", t.isCompleted)
                                    tasksArr.put(o)
                                }
                                exportObj.put("events", eventsArr)
                                exportObj.put("tasks", tasksArr)
                                backupOutputText = exportObj.toString(2)
                                onTriggerNotification("Backup Compiled 📦", "Generated portable system backup consisting of ${events.size} events and ${tasks.size} tasks!")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Text("Export", color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A8BFF)),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            if (backupInputText.isBlank()) {
                                onTriggerNotification("Validation Fault ⚠️", "Please paste a compiled JSON backup block in the terminal field.")
                                return@Button
                            }
                            try {
                                val cleanData = backupInputText.trim()
                                val parsed = org.json.JSONObject(cleanData)
                                val evArray = parsed.optJSONArray("events")
                                var importedEventsCount = 0
                                if (evArray != null) {
                                    for (i in 0 until evArray.length()) {
                                        val o = evArray.getJSONObject(i)
                                        val title = o.getString("title").replace(Regex("<[^>]*>"), "")
                                        val category = o.getString("category").replace(Regex("<[^>]*>"), "")
                                        val color = o.optLong("colorHex", PastelBlue.value.toLong())
                                        val timestamp = o.optLong("timestamp", 1)
                                        val tob = o.optString("timeOfBirth", "").filter { it.isLetterOrDigit() || it == ':' || it == ' ' }
                                        val bh = o.optString("bornHospital", "").replace(Regex("<[^>]*>"), "")
                                        val naks = o.optString("nakshatram", "").replace(Regex("<[^>]*>"), "")
                                        val rasi = o.optString("rasi", "").replace(Regex("<[^>]*>"), "")

                                        viewModel.addEvent(
                                            title = title,
                                            colorHex = color,
                                            category = category,
                                            day = timestamp,
                                            timeOfBirth = tob.ifBlank { null },
                                            bornHospital = bh.ifBlank { null },
                                            nakshatram = naks.ifBlank { null },
                                            rasi = rasi.ifBlank { null }
                                        )
                                        importedEventsCount++
                                    }
                                }

                                val tArray = parsed.optJSONArray("tasks")
                                var importedTasksCount = 0
                                if (tArray != null) {
                                    for (i in 0 until tArray.length()) {
                                        val o = tArray.getJSONObject(i)
                                        val title = o.getString("title").replace(Regex("<[^>]*>"), "")
                                        val category = o.getString("category").replace(Regex("<[^>]*>"), "")
                                        val dateStr = o.optString("dateString", "Daily")

                                        viewModel.addTask(
                                            title = title,
                                            category = category,
                                            dateString = dateStr
                                        )
                                        importedTasksCount++
                                    }
                                }

                                onTriggerNotification("Restore Completed 🎉", "Export complete! Synthesized $importedEventsCount events and $importedTasksCount tasks securely.")
                                onDismiss()
                            } catch (e: Exception) {
                                onTriggerNotification("Import Refused ⚠️", "Security validation failed. Structural string payload contains malformed parameters!")
                            }
                        }
                    ) {
                        Text("Import & Restore", color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                if (backupOutputText.isNotBlank()) {
                    Text("Export Terminal Output (Copy this text):", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    OutlinedTextField(
                        value = backupOutputText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FFCC),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }

                Text("Import Terminal Paste Field:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                OutlinedTextField(
                    value = backupInputText,
                    onValueChange = { backupInputText = it },
                    placeholder = { Text("Paste your JSON block dump here...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF1E1E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Done", color = Color.Black, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
}
}

// ==========================================
// CUSTOM MONTH YEAR PICKER DIALOG (M3 GRID)
// ==========================================
@Composable
fun MonthYearPickerDialog(
    currentMonth: Int,
    currentYear: Int,
    onDismiss: () -> Unit,
    onSelect: (Int, Int) -> Unit
) {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val years = (2024..2030).toList()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select Month & Year",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    color = Color.White
                )

                Text("Select Month", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(monthNames) { index, name ->
                        val isSel = index == currentMonth
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFF4A8BFF) else Color(0xFF141414))
                                .clickable { onSelect(index, currentYear) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.substring(0, 3),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSel) Color.White else Color.Gray
                             )
                        }
                    }
                }

                Text("Select Year", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(years) { year ->
                        val isSel = year == currentYear
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFF4A8BFF) else Color(0xFF141414))
                                .clickable { onSelect(currentMonth, year) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = year.toString(),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSel) Color.White else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// EDIT PROFILE DIALOG (M3 SHIFT & PREFERENCES ACCELERATION)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onTriggerNotification: (String, String) -> Unit
) {
    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val ownerEmail by viewModel.ownerEmail.collectAsStateWithLifecycle()
    val profileIcon by viewModel.profileIcon.collectAsStateWithLifecycle()

    var nameInput by remember { mutableStateOf(ownerName) }
    var emailInput by remember { mutableStateOf(ownerEmail) }
    var iconInput by remember { mutableStateOf(profileIcon) }

    val emojis = listOf("👨‍💻", "👩‍💻", "🧔", "🦊", "🚀", "💻", "🎨", "🌟", "⚡", "🧘", "🤖", "🦁", "🏆", "🦄", "🎯")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
                modifier = Modifier.clickable(enabled = false) {}
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                    border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Edit Profile Options",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = Color.White
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Owner Name", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF4A8BFF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Email Address", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF4A8BFF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Choose Profile Icon", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(emojis) { emoji ->
                                    val isSelected = iconInput == emoji
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFF4A8BFF) else Color(0xFF141414))
                                            .border(1.dp, if (isSelected) Color(0xFF4A8BFF) else Color.White.copy(alpha = 0.12f), CircleShape)
                                            .clickable { iconInput = emoji }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 20.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    viewModel.updateOwnerName(nameInput)
                                    viewModel.updateOwnerEmail(emailInput)
                                    viewModel.updateProfileIcon(iconInput)
                                    onTriggerNotification(
                                        "Profile Updated 👤",
                                        "Profile updated successfully."
                                    )
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A8BFF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getDayWithSuffix(day: Int): String {
    return when {
        day in 11..13 -> "${day}th"
        day % 10 == 1 -> "${day}st"
        day % 10 == 2 -> "${day}nd"
        day % 10 == 3 -> "${day}rd"
        else -> "${day}th"
    }
}

@Composable
fun RenderAvatarImage(avatarId: String, sizeDp: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val fontSize = (sizeDp.value * 0.45f).sp
        Text(
            text = if (avatarId.isBlank()) "👤" else avatarId,
            fontSize = fontSize
        )
    }
}

@Composable
fun CustomGridDatePicker(
    selectedDay: Int,
    maxDays: Int,
    accentColor: Color,
    onDaySelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val rows = (maxDays + 6) / 7
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (c in 0..6) {
                    val dayNum = r * 7 + c + 1
                    if (dayNum <= maxDays) {
                        val isSelected = selectedDay == dayNum
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.03f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onDaySelected(dayNum) }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNum.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) accentColor else Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
