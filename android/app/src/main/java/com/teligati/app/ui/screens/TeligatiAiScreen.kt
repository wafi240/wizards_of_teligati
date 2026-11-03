package com.teligati.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ChatMessage(val sender: String, val text: String, val isUser: Boolean)

@Composable
fun TeligatiAiScreen() {
    var prompt by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    "Teligati AI",
                    "🪄 Welcome Samun! I am Teligati AI. I can match your CGPA & skills with open Alumni Assistant tasks, locate research professors, and scan your submissions to ensure no credentials or API keys leak.",
                    false
                )
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("🪄 Teligati AI Career Advisor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        Text("Smart opportunity matching & security scanner.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Secret Scanner Active: Prompts and submissions are automatically monitored to prevent API key leaks.", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(msg.sender, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SuggestionChip(
                onClick = { prompt = "Recommend assistant tasks for 2nd year CSE with Python" },
                label = { Text("Find Assistant Gigs", style = MaterialTheme.typography.labelSmall) }
            )
            SuggestionChip(
                onClick = { prompt = "Which KUET professors have open research papers?" },
                label = { Text("Research Papers", style = MaterialTheme.typography.labelSmall) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Teligati AI...") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (prompt.isNotBlank()) {
                        val userText = prompt
                        val aiReply = when {
                            userText.contains("key", ignoreCase = true) || userText.contains("secret", ignoreCase = true) ->
                                "⚠️ Security Warning: Teligati AI detected potential credential/secret references in your input. Please keep your personal API keys confidential."
                            userText.contains("research", ignoreCase = true) ->
                                "🔬 Research Match: Prof. Dr. K. M. Azharul Hasan (CSE Intelligent Systems Lab) has 2 open student positions for real-time computer vision papers. Your CGPA (3.84) qualifies!"
                            userText.contains("assistant", ignoreCase = true) ->
                                "🤝 Assistant Work Match: 92% Match with Engr. Tanvir Ahmed's Python Automation Assistant (BMW Munich Alumni). Replaces tuition with ৳15,000/month."
                            else ->
                                "💡 Based on your CSE 2K23 background, I recommend exploring the Alumni Directory in Germany (Munich chapter) and requesting mentorship for graduate studies!"
                        }
                        messages = messages + ChatMessage("You", userText, true) + ChatMessage("Teligati AI", aiReply, false)
                        prompt = ""
                    }
                }
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
