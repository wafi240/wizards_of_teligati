package com.teligati.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {
    var likedPosts by remember { mutableStateOf(setOf<Int>()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🧙 Wizards of Teligati",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Connect. Collaborate. Grow.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("KUET 3-Pillar Collaboration Network", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("🎓 Students: Replace tuition with flexible Alumni Assistant work & Research.", style = MaterialTheme.typography.bodySmall)
                    Text("💼 Alumni: Hire trusted KUET juniors & advertise global tech roles.", style = MaterialTheme.typography.bodySmall)
                    Text("🏛️ Teachers: Recruit students for PhD research & conference papers.", style = MaterialTheme.typography.bodySmall)
                }
            }

            Text("Professional Feed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        val feedItems = listOf(
            Triple(
                "Engr. Tanvir Ahmed • CSE '12 (BMW Group, Munich)",
                "📢 Hiring 2 KUET undergraduates as Technical Assistants! If you are in 2nd/3rd year and know Python & basic automation, check the Opportunities tab. We pay ৳15,000/month with flexible hours so your studies come first.",
                "Yesterday • Munich, Germany"
            ),
            Triple(
                "Prof. Dr. K. M. Azharul Hasan • CSE Dept",
                "🔬 Our Big Data Lab is accepting student co-authors for a real-time object detection research paper. Students with good academic standing and Python skills can apply under Research Collaborations.",
                "3 hours ago • KUET Campus"
            ),
            Triple(
                "Samun Sadab Wafi • CSE 2K23",
                "🚀 Published the 'Wizards of Teligati' Android application architecture built with Jetpack Compose & Clean Architecture. Check the Spellbook tab to contribute!",
                "5 hours ago • KUET Campus"
            ),
            Triple(
                "Nusrat Jahan • CSE '17 (Amazon Vancouver)",
                "Delighted to mentor KUET students aiming for Canadian permanent residency and tech careers. Book a guidance slot through the Explore tab!",
                "2 days ago • Vancouver, Canada"
            )
        )

        items(feedItems.size) { index ->
            val (author, text, meta) = feedItems[index]
            val isLiked = likedPosts.contains(index)

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(author, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                likedPosts = if (isLiked) likedPosts - index else likedPosts + index
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isLiked) "Liked" else "Like", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        TextButton(onClick = {}) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
    }
}
