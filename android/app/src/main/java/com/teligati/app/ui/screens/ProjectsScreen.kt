package com.teligati.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teligati.app.model.ProjectItem
import com.teligati.app.model.SampleData

@Composable
fun ProjectsScreen() {
    var showDialog by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf<ProjectItem?>(null) }
    var contributeSuccessMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Text(
                "📖 Spellbook (Projects & Research)",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Showcase student innovations, club inventions, and research papers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "🛡️ Core Rule: No project buying or selling. Projects exist for learning, showcasing, and hiring contributors for real work.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (contributeSuccessMsg != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        contributeSuccessMsg!!,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        items(SampleData.projectsList) { p ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            p.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(p.type, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(p.description, style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tech & Tools: ${p.technologies.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Text("Lead: ${p.ownerName} (${p.ownerDept}) • ${p.contributorsCount} Collaborators", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (p.paperDoi.isNotEmpty()) {
                        Text("DOI: ${p.paperDoi}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row {
                        if (p.isOpenForContribution) {
                            Button(
                                onClick = {
                                    selectedProject = p
                                    showDialog = true
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Request to Contribute / Co-Author")
                            }
                        } else {
                            OutlinedButton(onClick = {}, enabled = false) {
                                Text("Team Complete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog && selectedProject != null) {
        var proposalText by remember {
            mutableStateOf("I would like to contribute my skills to this project / paper. My portfolio includes relevant work in ${selectedProject?.technologies?.firstOrNull() ?: "the field"}.")
        }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Collaborate on ${selectedProject?.name}") },
            text = {
                Column {
                    Text("Explain how you can contribute to this project or research:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = proposalText,
                        onValueChange = { proposalText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        contributeSuccessMsg = "Collaboration proposal sent to ${selectedProject?.ownerName}!"
                        showDialog = false
                    }
                ) {
                    Text("Send Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
