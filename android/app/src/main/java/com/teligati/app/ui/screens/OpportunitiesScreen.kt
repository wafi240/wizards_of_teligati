package com.teligati.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teligati.app.model.Opportunity
import com.teligati.app.model.OpportunityCategory
import com.teligati.app.model.SampleData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunitiesScreen() {
    var selectedCategory by remember { mutableStateOf(OpportunityCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var appliedJobTitle by remember { mutableStateOf<String?>(null) }
    var activeApplyingOpportunity by remember { mutableStateOf<Opportunity?>(null) }

    val filteredList = SampleData.opportunities.filter { opp ->
        val matchesCategory = (selectedCategory == OpportunityCategory.ALL || opp.category == selectedCategory)
        val matchesQuery = opp.title.contains(searchQuery, ignoreCase = true) ||
                opp.organization.contains(searchQuery, ignoreCase = true) ||
                opp.skillsRequired.any { it.contains(searchQuery, ignoreCase = true) }
        matchesCategory && matchesQuery
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Text(
                "💼 KUET Opportunities Hub",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Flexible Assistant Work, Research Papers with Professors, and Micro-tasks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by skill, research field, or alumni...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OpportunityCategory.values().forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.displayName) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (appliedJobTitle != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Application submitted for \"$appliedJobTitle\"! The KUET poster will review your portfolio.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        items(filteredList) { opp ->
            OpportunityCard(
                opp = opp,
                onApply = { activeApplyingOpportunity = opp }
            )
        }
    }

    if (activeApplyingOpportunity != null) {
        var applicationMessage by remember {
            mutableStateOf("Respected ${activeApplyingOpportunity?.postedByName}, I am a 2nd year KUET CSE student with relevant skills in ${activeApplyingOpportunity?.skillsRequired?.joinToString(", ")}. I would love to contribute!")
        }
        AlertDialog(
            onDismissRequest = { activeApplyingOpportunity = null },
            title = {
                Text(
                    "Apply: ${activeApplyingOpportunity?.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Posted By: ${activeApplyingOpportunity?.postedByName} (${activeApplyingOpportunity?.posterRole})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Compensation: ${activeApplyingOpportunity?.compensation}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Your Message / Portfolio Highlights:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = applicationMessage,
                        onValueChange = { applicationMessage = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeApplyingOpportunity?.isApplied = true
                        appliedJobTitle = activeApplyingOpportunity?.title
                        activeApplyingOpportunity = null
                    }
                ) {
                    Text("Submit Application")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeApplyingOpportunity = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun OpportunityCard(opp: Opportunity, onApply: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (opp.category) {
                        OpportunityCategory.ASSISTANT_WORK -> MaterialTheme.colorScheme.primaryContainer
                        OpportunityCategory.RESEARCH_COLLABORATION -> MaterialTheme.colorScheme.secondaryContainer
                        OpportunityCategory.MICRO_TASKS -> Color(0xFF10B981).copy(alpha = 0.2f)
                        OpportunityCategory.FULLTIME_JOBS -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        opp.category.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    opp.duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(opp.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${opp.organization} • 📍 ${opp.location}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("👤 Posted by: ${opp.postedByName} (${opp.posterRole})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(8.dp))
            Text(opp.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                opp.skillsRequired.forEach { skill ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(skill, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (opp.minCgpa != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Min CGPA: ${opp.minCgpa}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Payment / Stipend", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        opp.compensation,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (opp.isApplied) {
                    OutlinedButton(onClick = {}, enabled = false, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Applied")
                    }
                } else {
                    Button(onClick = onApply, shape = RoundedCornerShape(8.dp)) {
                        Text("Apply Now")
                    }
                }
            }
        }
    }
}
