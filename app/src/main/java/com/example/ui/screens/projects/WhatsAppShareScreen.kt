package com.example.ui.screens.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lead
import com.example.data.model.Project
import com.example.data.model.WhatsAppTemplate
import com.example.ui.MainViewModel
import com.example.ui.theme.NexaTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppShareScreen(
    lead: Lead,
    project: Project,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val templates by viewModel.whatsAppTemplates.collectAsState()

    var selectedTemplate by remember { mutableStateOf(templates.firstOrNull()) }

    LaunchedEffect(templates) {
        if (selectedTemplate == null) {
            selectedTemplate = templates.firstOrNull()
        }
    }

    // Attachment checkboxes
    var includeMessage by remember { mutableStateOf(true) }
    var includeBrochurePdf by remember { mutableStateOf(true) }
    var includePriceSheetPdf by remember { mutableStateOf(true) }
    var includeImages by remember { mutableStateOf(true) }
    var includeVideoLink by remember { mutableStateOf(true) }

    // Replace template variables
    val renderedMessage = remember(selectedTemplate, lead, project, currentUser, includeBrochurePdf, includePriceSheetPdf, includeVideoLink) {
        val rawTemplate = selectedTemplate?.messageTemplate ?: "Hello {{customer_name}}, please find details for {{project_name}} attached."
        var msg = rawTemplate
            .replace("{{customer_name}}", lead.name)
            .replace("{{salesperson_name}}", currentUser?.name ?: "Sales Executive")
            .replace("{{project_name}}", project.name)
            .replace("{{location}}", project.location)
            .replace("{{budget}}", "${lead.budgetMin}L - ${lead.budgetMax}L")
            .replace("{{salesperson_phone}}", currentUser?.phone ?: "9880112266")

        val attachmentsList = mutableListOf<String>()
        if (includeBrochurePdf) attachmentsList.add("📄 Digital Brochure PDF: ${project.brochurePdfName}")
        if (includePriceSheetPdf) attachmentsList.add("📊 Verified Price Matrix: ${project.priceSheetPdfName}")
        if (includeVideoLink) attachmentsList.add("🎥 4K Architectural Walkthrough: ${project.videoUrl}")
        if (includeImages) attachmentsList.add("🖼️ High-Res Elevation & Master Plan Images")

        if (attachmentsList.isNotEmpty()) {
            msg += "\n\n📦 Shared Attachments & Assets:\n" + attachmentsList.joinToString("\n")
        }
        msg
    }

    var editableMessage by remember { mutableStateOf(renderedMessage) }
    LaunchedEffect(renderedMessage) {
        editableMessage = renderedMessage
    }

    var shareSuccessBanner by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WhatsApp Project Share", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${lead.name} • ${project.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recipient & Project Header
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Customer Recipient", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${lead.name} (${lead.whatsapp})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("RERA Number", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(project.reraNumber.take(16) + "...", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF059669))
                        }
                    }
                }
            }

            // WhatsApp Master Template Selector
            Text("Select Approved WhatsApp Template", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            var templateExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = templateExpanded,
                onExpandedChange = { templateExpanded = !templateExpanded }
            ) {
                OutlinedTextField(
                    value = selectedTemplate?.title ?: "Select Template",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = templateExpanded,
                    onDismissRequest = { templateExpanded = false }
                ) {
                    templates.forEach { tmpl ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(tmpl.title, fontWeight = FontWeight.SemiBold)
                                    Text(tmpl.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                selectedTemplate = tmpl
                                templateExpanded = false
                            }
                        )
                    }
                }
            }

            // Media Assets & Collateral Selection
            Text("Collateral & Attachments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    CheckboxItem(label = "Official Digital Brochure PDF (${project.brochurePdfName})", checked = includeBrochurePdf, onCheckedChange = { includeBrochurePdf = it })
                    CheckboxItem(label = "Cost Sheet Matrix & Floor Plan PDF (${project.priceSheetPdfName})", checked = includePriceSheetPdf, onCheckedChange = { includePriceSheetPdf = it })
                    CheckboxItem(label = "High-Res Render Images & Master Plan", checked = includeImages, onCheckedChange = { includeImages = it })
                    CheckboxItem(label = "Architectural Video Walkthrough Link", checked = includeVideoLink, onCheckedChange = { includeVideoLink = it })
                }
            }

            // Message Preview & Edit
            Text("Message Preview (Live Replace)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = editableMessage,
                onValueChange = { editableMessage = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .testTag("whatsapp_message_preview"),
                textStyle = MaterialTheme.typography.bodySmall
            )

            // Send on WhatsApp Button
            Button(
                onClick = {
                    val filesList = mutableListOf<String>()
                    if (includeBrochurePdf) filesList.add(project.brochurePdfName)
                    if (includePriceSheetPdf) filesList.add(project.priceSheetPdfName)
                    if (includeImages) filesList.add("Project_Images")
                    if (includeVideoLink) filesList.add("Video_Link")

                    viewModel.shareProjectOnWhatsApp(
                        context = context,
                        lead = lead,
                        project = project,
                        message = editableMessage,
                        selectedFiles = filesList
                    )
                    shareSuccessBanner = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("send_on_whatsapp_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NexaTeal)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SEND ON WHATSAPP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            if (shareSuccessBanner) {
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WhatsApp share initiated and activity history recorded in CRM timeline!",
                            fontSize = 12.sp,
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun CheckboxItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
