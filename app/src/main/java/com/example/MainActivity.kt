package com.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.ui.DashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  
  companion object {
    var globalErrorState by mutableStateOf<Throwable?>(null)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Catch any uncaught thread exceptions to prevent application exit or silent crashes,
    // and display a clean diagnostics overlay instead.
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      Handler(Looper.getMainLooper()).post {
        globalErrorState = throwable
      }
      throwable.printStackTrace()
    }

    setContent {
      MyApplicationTheme {
        Box(modifier = Modifier.fillMaxSize()) {
          val error = globalErrorState
          if (error != null) {
            // Visual Diagnosis / Self-Healing Console Recovery screen
            Scaffold(
              containerColor = Color(0xFF0F0F15),
              modifier = Modifier.fillMaxSize()
            ) { padding ->
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(padding)
                  .padding(24.dp)
                  .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                Text(
                  text = "🛰 Personal OS Crash Guard",
                  style = MaterialTheme.typography.headlineMedium,
                  color = Color(0xFFFF5252)
                )
                Text(
                  text = "An unexpected runtime exception was intercepted. The console caught the following status:",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color.White.copy(alpha = 0.8f)
                )
                
                Card(
                  colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2A)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                      text = "${error::class.java.simpleName}: ${error.message}",
                      style = MaterialTheme.typography.titleSmall,
                      color = Color(0xFFFFCC00),
                      fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                      text = error.stackTraceToString(),
                      style = MaterialTheme.typography.bodySmall,
                      color = Color.White.copy(alpha = 0.6f),
                      fontFamily = FontFamily.Monospace
                    )
                  }
                }

                Row(
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Button(
                    onClick = {
                      // Attempt to clear database and try recovering
                      try {
                        applicationContext.deleteDatabase("personal_os_database")
                      } catch (e: Exception) {
                        e.printStackTrace()
                      }
                      globalErrorState = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                  ) {
                    Text("Clear DB & Reset")
                  }

                  OutlinedButton(
                    onClick = {
                      globalErrorState = null
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                  ) {
                    Text("Ignore & Continue")
                  }
                }
              }
            }
          } else {
            DashboardScreen()
          }
        }
      }
    }
  }
}
