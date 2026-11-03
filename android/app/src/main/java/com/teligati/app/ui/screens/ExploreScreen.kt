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
import com.teligati.app.model.AlumniProfile
import com.teligati.app.model.FacultyProfile
import com.teligati.app.model.SampleData

@Composable
fun ExploreScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var guidanceRequestedName by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Text(
                "🌍 KUET Directory & Network",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Connect with Alumni across 40+ countries and Faculty Supervisors.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Alumni Network") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Teachers & Labs") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Around the World") }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        if (selectedTab == 0) "Search by company (Google, Tesla), country (Germany)..."
                        else if (selectedTab == 1) "Search by research field (AI, Robotics, Cloud)..."
                        else "Search by country..."
                    )
                },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (guidanceRequestedName != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Career guidance request sent to $guidanceRequestedName! You will receive a direct notification.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> {
                val filteredAlumni = SampleData.alumniList.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.currentCompany.contains(searchQuery, ignoreCase = true) ||
                    it.country.contains(searchQuery, ignoreCase = true) ||
                    it.department.contains(searchQuery, ignoreCase = true) ||
                    it.batch.contains(searchQuery, ignoreCase = true)
                }
                items(filteredAlumni) { alumni ->
                    AlumniCard(
                        alumni = alumni,
                        onRequestGuidance = { guidanceRequestedName = alumni.name }
                    )
                }
            }
            1 -> {
                val filteredFaculty = SampleData.facultyList.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.researchLab.contains(searchQuery, ignoreCase = true) ||
                    it.researchAreas.any { area -> area.contains(searchQuery, ignoreCase = true) }
                }
                items(filteredFaculty) { faculty ->
                    FacultyCard(
                        faculty = faculty,
                        onRequestResearch = { guidanceRequestedName = faculty.name }
                    )
                }
            }
            2 -> {
                item {
                    WorldBreakdownCard()
                }
            }
        }
    }
}

@Composable
fun AlumniCard(alumni: AlumniProfile, onRequestGuidance: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(alumni.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                if (alumni.isVerified) {
                    Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(" KUET Verified", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text("${alumni.department} • ${alumni.batch}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${alumni.currentPosition} @ ${alumni.currentCompany}", fontWeight = FontWeight.Medium)
            Text("📍 ${alumni.city}, ${alumni.country}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(alumni.bio, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (alumni.isAvailableForMentorship) {
                    Button(onClick = onRequestGuidance, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Request Guidance")
                    }
                }
                if (alumni.isActivelyHiring) {
                    OutlinedButton(onClick = onRequestGuidance, shape = RoundedCornerShape(8.dp)) {
                        Text("Apply as Assistant")
                    }
                }
            }
        }
    }
}

@Composable
fun FacultyCard(faculty: FacultyProfile, onRequestResearch: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(faculty.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("${faculty.designation} • ${faculty.department} (${faculty.university})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("🔬 Lab: ${faculty.researchLab}", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
            Text("📚 Publications: ${faculty.publicationsCount}+ papers", style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.height(8.dp))
            Text("Research Focus: ${faculty.researchAreas.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "${faculty.openAssistantPositions} Open Research Positions",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(onClick = onRequestResearch, shape = RoundedCornerShape(8.dp)) {
                    Text("Request to Join Lab")
                }
            }
        }
    }
}

@Composable
fun WorldBreakdownCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🌍 KUETians Around the World", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Aggregated alumni presence across countries & industries.", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(14.dp))

            val countries = listOf(
                Triple("🇧🇩 Bangladesh", "8,420 Alumni & Students", "Dhaka, Chittagong, Khulna Tech Hubs"),
                Triple("🇺🇸 United States", "1,245 Alumni", "Silicon Valley, Texas, Washington"),
                Triple("🇨🇦 Canada", "382 Alumni", "Toronto, Vancouver, Montreal"),
                Triple("🇩🇪 Germany", "214 Alumni", "Munich, Berlin, Frankfurt"),
                Triple("🇬🇧 United Kingdom", "301 Alumni", "London, Cambridge, Manchester"),
                Triple("🇦🇺 Australia", "176 Alumni", "Sydney, Melbourne"),
                Triple("🇯🇵 Japan", "143 Alumni", "Tokyo, Osaka, Nagoya")
            )

            countries.forEach { (country, count, hubs) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(country, fontWeight = FontWeight.Bold)
                        Text(hubs, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(count, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}
