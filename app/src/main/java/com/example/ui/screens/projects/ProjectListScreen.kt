package com.example.ui.screens.projects

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.data.model.PropertyType
import com.example.ui.MainViewModel
import com.example.ui.theme.NexaGoldDark
import com.example.ui.theme.NexaNavyMedium
import com.example.ui.theme.NexaTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: MainViewModel,
    onProjectSelected: (Project) -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsState()
    val canManageProjects = viewModel.authManager.canManageMasterProjects()

    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<PropertyType?>(null) }
    var showAddProjectDialog by remember { mutableStateOf(false) }

    val filteredProjects = projects.filter { proj ->
        val matchesQuery = searchQuery.isBlank() ||
                proj.name.contains(searchQuery, ignoreCase = true) ||
                proj.location.contains(searchQuery, ignoreCase = true) ||
                proj.developer.contains(searchQuery, ignoreCase = true) ||
                proj.configurations.contains(searchQuery, ignoreCase = true)

        val matchesType = selectedType == null || proj.projectType == selectedType

        matchesQuery && matchesType
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (canManageProjects) {
                ExtendedFloatingActionButton(
                    onClick = { showAddProjectDialog = true },
                    icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) },
                    text = { Text("Add Project") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_project_fab")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Master Projects & Inventory",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredProjects.size} Verified RERA Developments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!canManageProjects) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "View Only",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by project name, developer, location...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Property Type Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text("All Types (${projects.size})", fontSize = 12.sp) }
                )
                PropertyType.values().forEach { pt ->
                    FilterChip(
                        selected = selectedType == pt,
                        onClick = { selectedType = if (selectedType == pt) null else pt },
                        label = { Text(pt.label, fontSize = 12.sp) }
                    )
                }
            }

            // Projects List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProjects, key = { it.id }) { project ->
                    ProjectMasterCard(
                        project = project,
                        onClick = { onProjectSelected(project) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (showAddProjectDialog) {
        var pName by remember { mutableStateOf("") }
        var pDev by remember { mutableStateOf("City Nexa Infrastructure") }
        var pLoc by remember { mutableStateOf("Hoskote") }
        var pRera by remember { mutableStateOf("PRM/KA/RERA/1251/310/PR/240901/008812") }
        var pPriceMin by remember { mutableStateOf("80") }
        var pPriceMax by remember { mutableStateOf("160") }
        var pUnits by remember { mutableStateOf("200") }
        var pAvail by remember { mutableStateOf("45") }
        var pConfigs by remember { mutableStateOf("2, 3 & 4 BHK") }

        AlertDialog(
            onDismissRequest = { showAddProjectDialog = false },
            title = { Text("Add Master Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("Project Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pDev, onValueChange = { pDev = it }, label = { Text("Developer") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pLoc, onValueChange = { pLoc = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pRera, onValueChange = { pRera = it }, label = { Text("RERA Number") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = pPriceMin, onValueChange = { pPriceMin = it }, label = { Text("Min Price (₹L)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pPriceMax, onValueChange = { pPriceMax = it }, label = { Text("Max Price (₹L)") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = pConfigs, onValueChange = { pConfigs = it }, label = { Text("Configurations") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pName.isNotBlank()) {
                            viewModel.saveProject(
                                Project(
                                    id = "NX-PRJ-0${(5..99).random()}",
                                    name = pName.trim(),
                                    developer = pDev.trim(),
                                    location = pLoc.trim(),
                                    address = "$pLoc, Bengaluru",
                                    reraNumber = pRera.trim(),
                                    projectType = PropertyType.APARTMENT,
                                    priceMinLakhs = pPriceMin.toDoubleOrNull() ?: 80.0,
                                    priceMaxLakhs = pPriceMax.toDoubleOrNull() ?: 160.0,
                                    totalUnits = pUnits.toIntOrNull() ?: 200,
                                    availableUnits = pAvail.toIntOrNull() ?: 45,
                                    configurations = pConfigs,
                                    sizeRangeSqFt = "1200 - 2400 sq.ft",
                                    amenities = "Clubhouse, Swimming Pool, Gym, Children Play Area"
                                )
                            )
                            showAddProjectDialog = false
                        }
                    }
                ) {
                    Text("Save Project")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProjectDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProjectMasterCard(
    project: Project,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("project_card_${project.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (project.isResale || project.projectType == PropertyType.RESALE) {
                        Surface(
                            color = Color(0xFF003B95).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Autorenew, contentDescription = null, tint = Color(0xFF003B95), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "RESALE",
                                    color = Color(0xFF003B95),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Surface(
                        color = Color(0xFF059669).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "RERA Approved",
                                color = Color(0xFF059669),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = project.projectType.label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${project.developer} • ${project.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (project.isResale && project.resaleOwnerName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF003B95), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Seller: ${project.resaleOwnerName} (${project.propertyAgeYears} yrs old property)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF003B95),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${project.priceMinLakhs}L — ₹${project.priceMaxLakhs}L",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "${project.availableUnits} / ${project.totalUnits} Units Available",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (project.availableUnits > 10) Color(0xFF059669) else Color(0xFFDC2626),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Config: ${project.configurations} (${project.sizeRangeSqFt})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Possession: ${project.possessionDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "View Units & Inventory →",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
