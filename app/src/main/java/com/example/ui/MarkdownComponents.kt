package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Note
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// PURE KOTLIN INLINE MARKDOWN PARSER ENGINE
// ==========================================
fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Bold (**text** or __text__)
                text.startsWith("**", i) && text.indexOf("**", i + 2) != -1 -> {
                    val next = text.indexOf("**", i + 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
                    append(text.substring(i + 2, next))
                    pop()
                    i = next + 2
                }
                text.startsWith("__", i) && text.indexOf("__", i + 2) != -1 -> {
                    val next = text.indexOf("__", i + 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
                    append(text.substring(i + 2, next))
                    pop()
                    i = next + 2
                }
                // Italic (*text* or _text_)
                text.startsWith("*", i) && text.indexOf("*", i + 1) != -1 -> {
                    val next = text.indexOf("*", i + 1)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(text.substring(i + 1, next))
                    pop()
                    i = next + 1
                }
                text.startsWith("_", i) && text.indexOf("_", i + 1) != -1 -> {
                    val next = text.indexOf("_", i + 1)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(text.substring(i + 1, next))
                    pop()
                    i = next + 1
                }
                // Inline Code (`code`)
                text.startsWith("`", i) && text.indexOf("`", i + 1) != -1 -> {
                    val next = text.indexOf("`", i + 1)
                    pushStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color.White.copy(alpha = 0.12f),
                        color = Color(0xFF00FFCC),
                        fontWeight = FontWeight.SemiBold
                    ))
                    append(text.substring(i + 1, next))
                    pop()
                    i = next + 1
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

// ==========================================
// MARKDOWN RENDERING BLOCK COMPOSABLES
// ==========================================
@Composable
fun MarkdownHeader(text: String, level: Int) {
    val style = when (level) {
        1 -> MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.ExtraBold, 
            fontSize = 21.sp, 
            color = Color.White
        )
        2 -> MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold, 
            fontSize = 17.sp, 
            color = Color(0xFF4A8BFF)
        )
        else -> MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold, 
            fontSize = 14.sp, 
            color = Color(0xFF00FFCC)
        )
    }
    Text(
        text = text,
        style = style,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

@Composable
fun MarkdownCodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF08080C))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color(0xFFA1EAC6),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MarkdownListItem(text: String) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("• ", color = Color(0xFF4A8BFF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            text = parseInlineMarkdown(text),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray
        )
    }
}

@Composable
fun MarkdownBlockQuote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
            .drawBehind {
                drawLine(
                    color = Color(0xFF4A8BFF),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(0f, size.height),
                    strokeWidth = 10f
                )
            }
            .padding(start = 14.dp)
    ) {
        Text(
            text = parseInlineMarkdown(text),
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = Color.LightGray.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun MarkdownCheckboxItem(text: String, checked: Boolean) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (checked) Color(0xFF00FFCC) else Color.Gray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = parseInlineMarkdown(text),
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = if (checked) Color.Gray else Color.LightGray
        )
    }
}

@Composable
fun MarkdownParagraph(text: String) {
    Text(
        text = parseInlineMarkdown(text),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.LightGray,
        modifier = Modifier.fillMaxWidth()
    )
}

// ==========================================
// REAL-TIME MARKDOWN CANVAS PREVIEW LAYER
// ==========================================
@Composable
fun RenderMarkdown(markdown: String, modifier: Modifier = Modifier) {
    val lines = markdown.split("\n")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var inCodeBlock = false
        val codeBlockLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()

            // Code block parsing
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    MarkdownCodeBlock(codeBlockLines.joinToString("\n"))
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockLines.add(line)
                continue
            }

            // Headings
            if (trimmed.startsWith("# ")) {
                MarkdownHeader(trimmed.removePrefix("# "), level = 1)
                continue
            }
            if (trimmed.startsWith("## ")) {
                MarkdownHeader(trimmed.removePrefix("## "), level = 2)
                continue
            }
            if (trimmed.startsWith("### ")) {
                MarkdownHeader(trimmed.removePrefix("### "), level = 3)
                continue
            }

            // Horizontal Rule
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )
                continue
            }

            // Blockquote
            if (trimmed.startsWith(">")) {
                MarkdownBlockQuote(trimmed.removePrefix(">").trim())
                continue
            }

            // Checkboxes
            if (trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ")) {
                val checked = trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ")
                val text = trimmed.substring(6)
                MarkdownCheckboxItem(text, checked)
                continue
            }

            // Unordered Lists
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                val contentText = if (trimmed.startsWith("- ")) trimmed.substring(2) else trimmed.substring(2)
                MarkdownListItem(contentText)
                continue
            }

            // Paragraphs
            if (trimmed.isNotEmpty()) {
                MarkdownParagraph(line)
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            MarkdownCodeBlock(codeBlockLines.joinToString("\n"))
        }
    }
}

// ==========================================================
// SECURE, GLASSMORPHIC LOCAL-FIRST MARKDOWN NOTE EDITOR DIALOG
// ==========================================================
@Composable
fun MarkdownNoteEditorDialog(
    note: Note,
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onTriggerNotification: (String, String) -> Unit
) {
    var titleText by remember { mutableStateOf(note.title) }
    var contentText by remember { mutableStateOf(note.content) }
    
    // UI state configurations
    var selectedViewMode by remember { mutableStateOf(0) } // 0: Editor, 1: Live Preview
    var isSaving by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    
    // Tracking local unsynced edits state in-memory
    var isLocalModified by remember {
        mutableStateOf(note.title != titleText || note.content != contentText || !note.isSynced)
    }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Sync helpers
    val isCurrentlySynced = note.isSynced && !isLocalModified && titleText == note.title && contentText == note.content
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFB0D111A)), // Deep premium dark translucent
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.15f)), // Glass border
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4A8BFF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF4A8BFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Local Markdown Canvas",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            // Live Sync Status Flag label with glass effect
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrentlySynced) Color(0xFF00FFCC) else Color(0xFFFF9900))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCurrentlySynced) "Synced with Personal-Cloud 🌍" else "Local Unsaved Edits (Offline-First) 🗄️",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrentlySynced) Color(0xFF00FFCC) else Color(0xFFFF9900)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action controls / sync controller widget
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Editor Mode Buttons
                            Button(
                                onClick = { selectedViewMode = 0 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedViewMode == 0) Color(0xFF4A8BFF) else Color.Transparent
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Write", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = { selectedViewMode = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedViewMode == 1) Color(0xFF00FFCC).copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = if (selectedViewMode == 1) BorderStroke(1.dp, Color(0xFF00FFCC)) else null,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = if (selectedViewMode == 1) Color(0xFF00FFCC) else Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview", color = if (selectedViewMode == 1) Color(0xFF00FFCC) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Sync trigger option
                        Button(
                            onClick = {
                                isSyncing = true
                                scope.launch {
                                    delay(1000)
                                    viewModel.syncAllAppData { nSynced, eSynced ->
                                        isSyncing = false
                                        onTriggerNotification(
                                            "Sync Handshake Succeeded! 🌍",
                                            "Room Secure Database synchronized securely. Synced $nSynced notes and $eSynced events."
                                        )
                                        isLocalModified = false
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.08f),
                                disabledContainerColor = Color.White.copy(alpha = 0.02f)
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = Color(0xFF00FFCC), modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title Input Field (Plain glassmorphic layout)
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { 
                        titleText = it
                        isLocalModified = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Note Title...", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4A8BFF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                        focusedContainerColor = Color.White.copy(alpha = 0.02f),
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Markdown Quick Helper CheatSheet bar
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    val syntaxHelpers = listOf(
                        "H1" to "# ",
                        "H2" to "## ",
                        "Bold" to "**bold**",
                        "Italic" to "*italic*",
                        "Quote" to "> ",
                        "Bullet" to "- ",
                        "Todo" to "- [ ] ",
                        "Code" to "```\ncode\n```"
                    )
                    items(syntaxHelpers) { (label, code) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .clickable {
                                    contentText += " " + code
                                    isLocalModified = true
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(label, color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Workspace Center (Active view mode toggle layout)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (selectedViewMode == 0) {
                        // Regular Rich Text Editor Area
                        OutlinedTextField(
                            value = contentText,
                            onValueChange = { 
                                contentText = it
                                isLocalModified = true
                            },
                            modifier = Modifier.fillMaxSize(),
                            placeholder = { Text("Write content here using Markdown syntax...\n\nUse # for headers\nUse ** for bold text\nUse - [ ] for checklists\nUse ``` for code blocks", color = Color.Gray, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4A8BFF).copy(alpha = 0.6f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                                focusedContainerColor = Color.White.copy(alpha = 0.02f),
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else {
                        // Live Markdown Rendering Panel
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            if (contentText.isBlank()) {
                                Text(
                                    "No outline logged inside notebook. Use the 'Write' editor tab to format.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    modifier = Modifier.align(Alignment.Center),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                RenderMarkdown(markdown = contentText)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom CTA controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left action: Delete
                    IconButton(
                        onClick = {
                            viewModel.deleteNote(note)
                            onDismiss()
                            onTriggerNotification("Scribble Deleted 🗑️", "Note titled '${note.title}' has been successfully purged.")
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Purge note", tint = Color(0xFFFF5252))
                    }

                    // Right action: Close/Save
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                        ) {
                            Text("Discard", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                isSaving = true
                                scope.launch {
                                    delay(500)
                                    // Save changes locally in Room database
                                    viewModel.updateNote(
                                        id = note.id,
                                        title = titleText.ifBlank { "Untitled Note" },
                                        content = contentText,
                                        isSynced = false // Mark unsynced on save to encourage syncing
                                    )
                                    isSaving = false
                                    isLocalModified = false
                                    onTriggerNotification("Local Note Saved 🗄️", "Markdown file content saved locally to database encrypted vault.")
                                    onDismiss()
                                }
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A8BFF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Draft", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
