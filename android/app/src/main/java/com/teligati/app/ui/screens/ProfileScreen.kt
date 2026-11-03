package com.teligati.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teligati.app.model.Opportunity
import com.teligati.app.model.OpportunityCategory
import com.teligati.app.model.SampleData

@Composable
fun ProfileScreen() {
    val student = SampleData.studentPortfolio
    var showPostDialog by remember { mutableStateOf(false) }
    var postedSuccessMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Text("👤 Professional Portfolio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(student.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Text("${student.department} • Batch ${student.batch}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                            Text("Roll: ${student.roll} • CGPA: ${student.cgpa} / 4.00", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("🎓 Academic Standing: ${student.academicYear}")
                    Text("⏱️ Available for Assistant Work: ${student.availableHoursPerWeek} hrs/week", fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Clubs & Leadership:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    student.clubs.forEach { club ->
                        Text("• $club", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Core Technical Skills:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text(student.skills.joinToString(" • "), style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Achievements:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    student.achievements.forEach { ach ->
                        Text("🏆 $ach", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Professional Earnings & Escrow Ledger", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Available Balance", style = MaterialTheme.typography.labelSmall)
                            Text("৳${student.earningsAvailable.toInt()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("Pending in Escrow", style = MaterialTheme.typography.labelSmall)
                            Text("৳${student.earningsPendingEscrow.toInt()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {}, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Withdraw Earnings to Bank / Account")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Alumni & Teacher Quick Actions", fontWeight = FontWeight.Bold)
                    Text("Need a personal assistant, research collaborator, or hiring for your company?", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showPostDialog = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Post a Task / Opportunity")
                    }
                }
            }

            if (postedSuccessMsg != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(postedSuccessMsg!!, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Service Transaction History (No Repo Buying/Selling)", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(SampleData.transactions) { tx ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(tx.title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                        Text("${tx.category} • ${tx.date}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "+ ৳${tx.amount.toInt()}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showPostDialog) {
        var postTitle by remember { mutableStateOf("") }
        var postComp by remember { mutableStateOf("৳10,000 / month") }
        var postCategory by remember { mutableStateOf(OpportunityCategory.ASSISTANT_WORK) }

        AlertDialog(
            onDismissRequest = { showPostDialog = false },
            title = { Text("Publish Opportunity for KUETians") },
            text = {
                Column {
                    OutlinedTextField(
                        value = postTitle,
                        onValueChange = { postTitle = it },
                        label = { Text("Title (e.g. Research Assistant, Automation Gig)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = postComp,
                        onValueChange = { postComp = it },
                        label = { Text("Compensation (e.g. ৳12,000 / month or ৳500)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (postTitle.isNotBlank()) {
                            SampleData.opportunities.add(
                                0,
                                Opportunity(
                                    id = System.currentTimeMillis(),
                                    title = postTitle,
                                    category = postCategory,
                                    organization = "KUET Verified Network",
                                    location = "Remote",
                                    postedByName = student.name,
                                    posterRole = "KUET Member",
                                    compensation = postComp,
                                    duration = "Flexible",
                                    description = "Newly created opportunity published for the KUET community.",
                                    skillsRequired = listOf("Python", "Problem Solving"),
                                    minCgpa = 3.0
                                )
                            )
                            postedSuccessMsg = "Successfully published '$postTitle'! Visible now in Opportunities tab."
                            showPostDialog = false
                        }
                    }
                ) {
                    Text("Publish Opportunity")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPostDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
