package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AuthUser
import com.example.data.CustomerProfile
import com.example.data.LeadSequenceState
import com.example.data.PropertyMappingResult
import com.example.data.ServiceQuote
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.ui.theme.*
import com.example.viewmodel.ServiceViewModel

@Composable
fun MainLayout(viewModel: ServiceViewModel = viewModel()) {
    val isAdminMode by viewModel.isAdminMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var showAuthDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CharcoalBg,
        topBar = {
            ApexTopBar(
                isAdmin = isAdminMode,
                currentUser = currentUser,
                onModeToggle = { viewModel.isAdminMode.value = it },
                onAuthClick = { showAuthDialog = true },
                onSignOutClick = { viewModel.signOut() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CharcoalBg)
        ) {
            // Ambient gold background glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x15D4AF37), Color.Transparent),
                                center = Offset(size.width * 0.8f, size.height * 0.2f),
                                radius = size.width * 0.8f
                            )
                        )
                    }
            )

            if (isAdminMode) {
                // Secure Admin View: requires being signed in as admin
                if (currentUser == null || !currentUser!!.isAdmin) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AuthScreen(
                            viewModel = viewModel,
                            onAuthSuccess = {
                                // Handled via StateFlow observation
                            }
                        )
                    }
                } else {
                    AdminBackendScreen(viewModel = viewModel)
                }
            } else {
                ClientPortalScreen(viewModel = viewModel)
            }

            // Auth Dialog
            if (showAuthDialog) {
                Dialog(onDismissRequest = { showAuthDialog = false }) {
                    AuthScreen(
                        viewModel = viewModel,
                        onAuthSuccess = { showAuthDialog = false },
                        onDismiss = { showAuthDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun ApexTopBar(
    isAdmin: Boolean,
    currentUser: AuthUser?,
    onModeToggle: (Boolean) -> Unit,
    onAuthClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
                shape = RoundedCornerShape(0.dp)
            ),
        color = CharcoalSurface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GoldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "E",
                        color = CharcoalBg,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }

                Text(
                    text = "ESTATE GOLD",
                    color = GoldPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            // Auth Control and App View Switcher
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // User Badge / Sign In Button
                if (currentUser != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .background(Color(0x0DFFFFFF), RoundedCornerShape(20.dp))
                            .clickable { onSignOutClick() }
                            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.displayName.take(1).uppercase(),
                                color = CharcoalBg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column {
                            Text(
                                text = currentUser.displayName.substringBefore(" "),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentUser.isAdmin) "Staff" else "Client",
                                color = if (currentUser.isAdmin) GoldPrimary else OnCharcoal.copy(alpha = 0.5f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = OnCharcoal.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onAuthClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AD4AF37), contentColor = GoldPrimary),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Sign In",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sign In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Client vs Admin Mode Toggles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(CharcoalBg, RoundedCornerShape(20.dp))
                        .padding(3.dp)
                ) {
                    val buttonShape = RoundedCornerShape(16.dp)
                    
                    Surface(
                        onClick = { onModeToggle(false) },
                        shape = buttonShape,
                        color = if (!isAdmin) GoldPrimary else Color.Transparent,
                        contentColor = if (!isAdmin) CharcoalBg else OnCharcoal,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text("Client", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        onClick = { onModeToggle(true) },
                        shape = buttonShape,
                        color = if (isAdmin) GoldPrimary else Color.Transparent,
                        contentColor = if (isAdmin) CharcoalBg else OnCharcoal,
                        modifier = Modifier.height(28.dp).testTag("admin_toggle_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text("Admin", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientPortalScreen(viewModel: ServiceViewModel) {
    val scrollState = rememberScrollState()
    val activeProfile by viewModel.activeCustomerProfile.collectAsState()
    val activeQuote by viewModel.activeServiceQuote.collectAsState()
    val activeLeadState by viewModel.activeLeadState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var showAuthDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Cloud Connection Status Banner
        if (currentUser != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CharcoalSurface,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Cloud Synced",
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text("SaaS Cloud Session Active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Synced with Firestore as ${currentUser!!.displayName}", color = OnCharcoal.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                    
                    TextButton(
                        onClick = { viewModel.signOut() },
                        colors = ButtonDefaults.textButtonColors(contentColor = OnCharcoal.copy(alpha = 0.6f))
                    ) {
                        Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CharcoalSurface,
                border = BorderStroke(1.dp, Color(0x0DFFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x0FFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "Cloud Setup",
                                tint = OnCharcoal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text("Cloud Database Persistence", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Sign in to secure satellite quotes with Firestore.", color = OnCharcoal.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                    
                    Button(
                        onClick = { showAuthDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = CharcoalBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("Sign In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (activeProfile == null) {
            HeroSection()

            // Form Experience Switcher
            var selectedFormTab by remember { mutableStateOf("Tailwind") }

            TabRow(
                selectedTabIndex = if (selectedFormTab == "Tailwind") 0 else 1,
                containerColor = CharcoalSurface,
                contentColor = GoldPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (selectedFormTab == "Tailwind") 0 else 1]),
                        color = GoldPrimary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedFormTab == "Tailwind",
                    onClick = { selectedFormTab = "Tailwind" },
                    text = { Text("SaaS Tailwind Form", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedFormTab == "Native",
                    onClick = { selectedFormTab = "Native" },
                    text = { Text("Native Compose Form", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (selectedFormTab == "Tailwind") {
                TailwindQuoteFormWebView(
                    viewModel = viewModel,
                    onQuoteSubmitted = {
                        // Handled via activeProfile updates
                    }
                )
            } else {
                ServiceSelectionSection(viewModel = viewModel)
                PackagePlansSection(viewModel = viewModel)
                QuoteFormSection(viewModel = viewModel)
            }
        } else {
            CustomerPortalDashboard(
                profile = activeProfile!!,
                quote = activeQuote,
                leadState = activeLeadState,
                viewModel = viewModel
            )
        }

        if (activeProfile != null) {
            SimulatedInboxTimeline(viewModel = viewModel)
        }
    }

    if (showAuthDialog) {
        Dialog(onDismissRequest = { showAuthDialog = false }) {
            AuthScreen(
                viewModel = viewModel,
                onAuthSuccess = { showAuthDialog = false },
                onDismiss = { showAuthDialog = false }
            )
        }
    }
}

@Composable
fun HeroSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CharcoalSurface)
            .border(
                BorderStroke(1.dp, Color(0x11FFFFFF)),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Color(0x1AD4AF37), RoundedCornerShape(50.dp))
                .border(BorderStroke(1.dp, GoldPrimary), RoundedCornerShape(50.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "GOLD STANDARDS IN ESTATE CARE",
                    color = GoldPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Text(
            text = buildAnnotatedString {
                append("Your Property, \n")
                withStyle(style = SpanStyle(color = GoldPrimary)) {
                    append("Perfected All Year.")
                }
            },
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 36.sp,
            color = Color.White
        )

        Text(
            text = "From summer lawns to winter snow. Precision landscaping & maintenance powered by satellite mapping.",
            color = OnCharcoal.copy(alpha = 0.8f),
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        HorizontalDivider(color = Color(0x0DFFFFFF), modifier = Modifier.padding(vertical = 4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("100% Virtual Quotes", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Analyzed via satellite imagery", color = OnCharcoal.copy(alpha = 0.7f), fontSize = 11.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("All-Season Service", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Grass in summer, snow in winter", color = OnCharcoal.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ServiceSelectionSection(viewModel: ServiceViewModel) {
    val selectedServices by viewModel.selectedServices.collectAsState()

    val services = listOf(
        ServiceItem("Grass Cutting", "Manicured lawn cuts, lawn trimming, blowing.", Icons.Default.Check),
        ServiceItem("Landscaping", "Custom design layouts, flowerbeds, and mulch.", Icons.Default.Build),
        ServiceItem("Fence Building", "Cedar or composite fencing built to last.", Icons.Default.Home),
        ServiceItem("Deck Building", "Premium custom decks, entertainment patios.", Icons.Default.Home),
        ServiceItem("Window Washing", "Deep streak-free pane wash and solar care.", Icons.Default.Build),
        ServiceItem("Pressure Washing", "Blast grease and dirt from your driveway.", Icons.Default.Refresh),
        ServiceItem("Snow Removal", "Prompt winter plow, salting, and pathway care.", Icons.Default.Star)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Select Our Premium Services",
            color = GoldPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // Clean grid representation using Rows to ensure total stability
        val rowsCount = (services.size + 1) / 2
        for (i in 0 until rowsCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (j in 0..1) {
                    val index = i * 2 + j
                    if (index < services.size) {
                        val service = services[index]
                        val isSelected = selectedServices.contains(service.name)

                        Surface(
                            onClick = { viewModel.toggleService(service.name) },
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) GoldBackgroundMuted else CharcoalSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) GoldPrimary else Color(0x0DFFFFFF)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = service.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) GoldPrimary else OnCharcoal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = service.name,
                                    color = if (isSelected) GoldPrimary else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )

                                Text(
                                    text = service.desc,
                                    color = OnCharcoal,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun PackagePlansSection(viewModel: ServiceViewModel) {
    val selectedPkg by viewModel.selectedPackage.collectAsState()

    val packages = listOf(
        PackageItem(
            name = "A la Carte",
            price = "Standard Rates",
            desc = "Choose individual services. Autocalculated via satellite mapping.",
            perks = listOf("Customized selection", "Flexible schedules", "Standard flat rates")
        ),
        PackageItem(
            name = "Apex Standard Package",
            price = "Save 10%",
            desc = "Lawn Grooming + Window Wash + Winter Plowing. Autorecurring billing.",
            perks = listOf("Lawn, snow & window core care", "Priority scheduling slot", "Recurring automated payments")
        ),
        PackageItem(
            name = "Apex Elite Subscription",
            price = "Save 20% (Elite Care)",
            desc = "Worry-free annual estate upkeep. Lawn, plowing, washes, custom deck & fence care.",
            perks = listOf("All 7 estate services managed", "Free seasonal deck/fence inspections", "24/7 priority emergency dispatch")
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Select a Plan Option",
            color = GoldPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        packages.forEach { pkg ->
            val isSelected = selectedPkg == pkg.name
            
            Surface(
                onClick = { viewModel.setPackage(pkg.name) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) GoldBackgroundMuted else CharcoalSurface,
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) GoldPrimary else Color(0x0DFFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pkg.name,
                            color = if (isSelected) GoldPrimary else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Text(
                            text = pkg.price,
                            color = GoldAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = pkg.desc,
                        color = OnCharcoal,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        pkg.perks.forEach { perk ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(perk, color = OnCharcoal, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuoteFormSection(viewModel: ServiceViewModel) {
    val name by viewModel.nameInput.collectAsState()
    val email by viewModel.emailInput.collectAsState()
    val phone by viewModel.phoneInput.collectAsState()
    val address by viewModel.addressInput.collectAsState()
    val bookingDate by viewModel.bookingDateInput.collectAsState()
    val method by viewModel.selectedMappingMethod.collectAsState()
    val notes by viewModel.notesInput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedServices by viewModel.selectedServices.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CharcoalSurfaceLight,
        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Generate Free Virtual Quote",
                color = GoldPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            val customTextFieldColors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CharcoalBg,
                unfocusedContainerColor = CharcoalBg,
                focusedBorderColor = GoldPrimary,
                focusedLabelColor = GoldPrimary,
                unfocusedBorderColor = Color(0x1AFFFFFF),
                unfocusedLabelColor = OnCharcoal.copy(alpha = 0.5f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )

            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.nameInput.value = it },
                label = { Text("Your Contact Name") },
                colors = customTextFieldColors,
                modifier = Modifier.fillMaxWidth().testTag("name_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.emailInput.value = it },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = customTextFieldColors,
                modifier = Modifier.fillMaxWidth().testTag("email_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { viewModel.phoneInput.value = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = customTextFieldColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = address,
                onValueChange = { viewModel.addressInput.value = it },
                label = { Text("Property Address") },
                colors = customTextFieldColors,
                modifier = Modifier.fillMaxWidth().testTag("address_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = bookingDate,
                onValueChange = { viewModel.bookingDateInput.value = it },
                label = { Text("Preferred Start Date (e.g. 2026-07-02)") },
                colors = customTextFieldColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Select Survey Method:", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val styles = listOf("Auto Map", "Phone Map", "In-Person")
                    styles.forEach { item ->
                        val active = method == item
                        Surface(
                            onClick = { viewModel.selectedMappingMethod.value = item },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) GoldBackgroundMuted else CharcoalBg,
                            border = BorderStroke(1.dp, if (active) GoldPrimary else Color(0x0DFFFFFF)),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = item,
                                    fontSize = 11.sp,
                                    color = if (active) GoldPrimary else OnCharcoal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.notesInput.value = it },
                label = { Text("Property obstacles or instructions") },
                colors = customTextFieldColors,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.submitQuoteRequest()
                },
                enabled = name.isNotBlank() && email.isNotBlank() && address.isNotBlank() && selectedServices.isNotEmpty() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = CharcoalBg,
                    disabledContainerColor = CharcoalSurfaceLight,
                    disabledContentColor = OnCharcoal.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_quote_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = CharcoalBg, modifier = Modifier.size(24.dp))
                } else {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedServices.isEmpty()) "Select a Service First" else "Generate My Quote Now",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerPortalDashboard(
    profile: CustomerProfile,
    quote: ServiceQuote?,
    leadState: LeadSequenceState?,
    viewModel: ServiceViewModel
) {
    var signatureInput by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CharcoalSurface,
            border = BorderStroke(1.dp, GoldPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "APEX CUSTOMER PORTAL",
                        color = GoldPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    val stageLabel = when (leadState?.currentStage) {
                        1 -> "Stage 1: Mapping Property"
                        2 -> "Stage 2: Quote Prepared"
                        3 -> "Stage 3: Follow-Up Offer"
                        4 -> "Contract Completed"
                        else -> "Processing..."
                    }
                    Box(
                        modifier = Modifier
                            .background(GoldBackgroundMuted, RoundedCornerShape(50.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(stageLabel, color = GoldLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Welcome, ${profile.name}!",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Your property at ${profile.address} is active in our database. We have booked your service sequence for: ${profile.bookingDate.ifBlank { "July 2nd, 2026" }}.",
                    color = OnCharcoal,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.resetForm() },
                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurfaceLight, contentColor = OnCharcoal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("New Quote", fontSize = 12.sp)
                    }

                    if (leadState != null && leadState.currentStage < 4 && quote != null && !quote.isContractSigned) {
                        Button(
                            onClick = { viewModel.fastForwardSimulation() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent.copy(alpha = 0.2f), contentColor = GoldLight),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccent),
                            modifier = Modifier.weight(1.5f).height(38.dp).testTag("fast_forward_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fast Forward 1 Day", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        SatelliteMapVisualizer(
            address = profile.address,
            method = profile.mappingType,
            isScanned = leadState != null && leadState.currentStage >= 2
        )

        if (leadState != null && leadState.currentStage >= 2 && quote != null) {
            QuoteInvoiceBreakdown(
                quote = quote,
                viewModel = viewModel,
                signatureInput = signatureInput,
                onSignatureChange = { signatureInput = it }
            )
        } else {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CharcoalSurface,
                border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = GoldPrimary, strokeWidth = 2.dp)
                    Text("Satellite Survey Analysis Pending", color = GoldLight, fontWeight = FontWeight.Bold)
                    Text(
                        "Our virtual scanners are currently preparing estimates for ${profile.address}. Press 'Fast Forward 1 Day' above to instantly skip calculations and see your quoted prices!",
                        color = OnCharcoal,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SatelliteMapVisualizer(
    address: String,
    method: String,
    isScanned: Boolean
) {
    var scanLineOffset by remember { mutableStateOf(0f) }
    val infiniteTransition = rememberInfiniteTransition()
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CharcoalSurface,
        border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "VIRTUAL PROPERTY SCANNER",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(if (isScanned) Color(0x222ECC71) else Color(0x22E74C3C), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isScanned) "SCAN MAPPED" else "ANALYZING SURVEY",
                        color = if (isScanned) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CharcoalBg)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw green lawn boundaries
                    val lawnPath = Path().apply {
                        moveTo(w * 0.15f, h * 0.15f)
                        lineTo(w * 0.85f, h * 0.1f)
                        lineTo(w * 0.9f, h * 0.85f)
                        lineTo(w * 0.2f, h * 0.9f)
                        close()
                    }
                    drawPath(
                        path = lawnPath,
                        color = Color(0x222ECC71)
                    )
                    drawPath(
                        path = lawnPath,
                        color = Color(0xFF2ECC71),
                        style = Stroke(width = 2f)
                    )

                    // Draw property structure outline (House)
                    val housePath = Path().apply {
                        moveTo(w * 0.35f, h * 0.3f)
                        lineTo(w * 0.65f, h * 0.3f)
                        lineTo(w * 0.65f, h * 0.7f)
                        lineTo(w * 0.35f, h * 0.7f)
                        close()
                    }
                    drawPath(
                        path = housePath,
                        color = Color(0x33D4AF37)
                    )
                    drawPath(
                        path = housePath,
                        color = GoldPrimary,
                        style = Stroke(width = 4f)
                    )

                    // Draw Deck extension
                    val deckPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.7f)
                        lineTo(w * 0.62f, h * 0.7f)
                        lineTo(w * 0.62f, h * 0.82f)
                        lineTo(w * 0.5f, h * 0.82f)
                        close()
                    }
                    drawPath(
                        path = deckPath,
                        color = Color(0x33FF9800)
                    )
                    drawPath(
                        path = deckPath,
                        color = Color(0xFFFF9800),
                        style = Stroke(width = 2f)
                    )

                    // Scanline animator
                    if (!isScanned) {
                        val scanY = h * animatedOffset
                        drawLine(
                            color = GoldLight,
                            start = Offset(0f, scanY),
                            end = Offset(w, scanY),
                            strokeWidth = 2f
                        )
                    }

                    if (isScanned) {
                        drawCircle(color = GoldPrimary, center = Offset(w * 0.35f, h * 0.3f), radius = 4f)
                        drawCircle(color = GoldPrimary, center = Offset(w * 0.65f, h * 0.7f), radius = 4f)
                    }
                }

                // Digital HUD details
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TELEMETRY: ACTIVE", color = GoldPrimary.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("3D LAYOUT SCAN", color = GoldPrimary.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }

                    if (!isScanned) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .background(CharcoalSurface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "MAPPING BOUNDARIES...",
                                color = GoldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ADDR: $address", color = OnCharcoal.copy(alpha = 0.7f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("SURVEY: $method", color = OnCharcoal.copy(alpha = 0.7f), fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun QuoteInvoiceBreakdown(
    quote: ServiceQuote,
    viewModel: ServiceViewModel,
    signatureInput: String,
    onSignatureChange: (String) -> Unit
) {
    val leadState by viewModel.activeLeadState.collectAsState()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CharcoalSurface,
        border = BorderStroke(1.dp, GoldPrimary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Itemized Service Invoice",
                color = GoldPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("INVOICE STATUS", color = OnCharcoal, fontSize = 10.sp)
                    Text(
                        text = if (quote.invoiceStatus == "Paid") "FULLY PAID" else "PENDING AUTHORIZATION",
                        color = if (quote.invoiceStatus == "Paid") Color(0xFF2ECC71) else GoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = viewModel.formatCurrency(quote.quotedPrice),
                    color = GoldLight,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }

            HorizontalDivider(color = Color(0x0DFFFFFF))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SCAN MEASUREMENTS COMPLETED:", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estimated Turf Size:", color = OnCharcoal, fontSize = 12.sp)
                    Text("${quote.propertySizeSqFt} sq ft", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Text("SELECTED SERVICES BREAKDOWN:", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                val services = quote.servicesSelected.split(", ")
                services.forEach { serviceName ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• $serviceName", color = OnCharcoal, fontSize = 12.sp)
                        Text("Included in Program", color = GoldLight, fontSize = 11.sp, fontStyle = FontStyle.Italic)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Program Choice:", color = OnCharcoal, fontSize = 12.sp)
                    Text(quote.packageSelected, color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Color(0x0DFFFFFF))

            if (leadState?.currentStage == 3 && !quote.isContractSigned) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GoldAccent.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, GoldAccent), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                            Text("10% Follow-Up Promo Applied!", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text("We've adjusted your total quote to reflect the follow-up discount offer sent in Email 3.", color = OnCharcoal, fontSize = 11.sp)
                    }
                }
            }

            if (!quote.isContractSigned) {
                HorizontalDivider(color = Color(0x0DFFFFFF))

                Text(
                    text = "STEP 1: Sign Digital Contract",
                    color = GoldLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "By signing below, you authorize Apex Services to start scheduled estate maintenance on ${quote.scheduledStartDate ?: "July 2nd, 2026"} under the agreed standard terms.",
                    color = OnCharcoal,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = signatureInput,
                    onValueChange = onSignatureChange,
                    label = { Text("Type your full name to sign contract") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CharcoalBg,
                        unfocusedContainerColor = CharcoalBg,
                        focusedBorderColor = GoldPrimary,
                        focusedLabelColor = GoldPrimary,
                        unfocusedBorderColor = Color(0x1AFFFFFF),
                        unfocusedLabelColor = OnCharcoal.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("contract_signature_input"),
                    singleLine = true
                )

                Button(
                    onClick = { viewModel.signContract(signatureInput) },
                    enabled = signatureInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = CharcoalBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("sign_contract_button")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign & Authorize Contract", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalBg, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CONTRACT ACTIVE & SIGNED", color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2ECC71), modifier = Modifier.size(20.dp))
                    }
                    Text("Signatory: ${quote.contractSignature}", color = OnCharcoal, fontSize = 12.sp)
                    Text("Scheduled Start: ${quote.scheduledStartDate}", color = OnCharcoal, fontSize = 12.sp)

                    if (quote.invoiceStatus == "Unpaid") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.payInvoice() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = CharcoalBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("pay_invoice_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate Invoice Payment", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x112ECC71), RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, Color(0xFF2ECC71)), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                "Success! Your estate booking sequence is active. A technician will contact you shortly and schedule start for ${quote.scheduledStartDate}.",
                                color = Color(0xFF2ECC71),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatedInboxTimeline(viewModel: ServiceViewModel) {
    val emails by viewModel.simulatedEmails.collectAsState()
    var selectedEmailId by remember { mutableStateOf<Int?>(null) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CharcoalSurface,
        border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "LEAD AUTOMATION SEQUENCER",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Tap to preview mail",
                    color = OnCharcoal.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                emails.forEach { email ->
                    val isActive = selectedEmailId == email.id
                    
                    Surface(
                        onClick = {
                            selectedEmailId = if (isActive) null else email.id
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (email.isSent) CharcoalBg else CharcoalSurfaceLight.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            if (email.isSent) GoldPrimary.copy(alpha = 0.3f) else Color(0x0DFFFFFF)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (email.isSent) Icons.Default.Email else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (email.isSent) GoldPrimary else OnCharcoal.copy(alpha = 0.4f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = email.subject,
                                        color = if (email.isSent) Color.White else OnCharcoal.copy(alpha = 0.4f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth(0.7f)
                                    )
                                }

                                Text(
                                    text = email.sentAtFormatted,
                                    color = if (email.isSent) GoldAccent else OnCharcoal.copy(alpha = 0.3f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = email.preheader,
                                color = OnCharcoal,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            AnimatedVisibility(
                                visible = isActive,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .background(CharcoalSurface, RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = email.body,
                                        color = OnCharcoal,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                    if (!email.isSent) {
                                        Text(
                                            text = "⚠️ Sent automatically if client does not book and simulation moves forward.",
                                            color = GoldAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminBackendScreen(viewModel: ServiceViewModel) {
    val customers by viewModel.allCustomerProfiles.collectAsState()
    val selectedCustomer by viewModel.selectedAdminCustomer.collectAsState()
    val selectedQuote by viewModel.selectedAdminQuote.collectAsState()
    val selectedLeadState by viewModel.selectedAdminLeadState.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CharcoalSurface,
            border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "APEX CRM DATABASE",
                    color = GoldPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (customers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No leads currently stored.\nSubmit a quote on the client screen!",
                            color = OnCharcoal,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(customers) { customer ->
                            val isActive = selectedCustomer?.id == customer.id
                            
                            Surface(
                                onClick = { viewModel.selectAdminCustomer(customer) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isActive) GoldBackgroundMuted else CharcoalBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isActive) GoldPrimary else Color(0x0DFFFFFF)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = customer.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = customer.email,
                                            color = OnCharcoal,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CharcoalSurface,
            border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
            modifier = Modifier
                .weight(1.8f)
                .fillMaxHeight()
        ) {
            if (selectedCustomer == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Select a lead to inspect mapping metrics,\ninvoice values, and email status.",
                        color = OnCharcoal,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CLIENT BACKEND METRICS",
                            color = GoldPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { viewModel.deleteCustomer(selectedCustomer!!.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE74C3C))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CharcoalBg,
                        border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Name: ${selectedCustomer!!.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Email: ${selectedCustomer!!.email}", color = OnCharcoal, fontSize = 12.sp)
                            Text("Phone: ${selectedCustomer!!.phone}", color = OnCharcoal, fontSize = 12.sp)
                            Text("Address: ${selectedCustomer!!.address}", color = OnCharcoal, fontSize = 12.sp)
                            Text("Start Date: ${selectedCustomer!!.bookingDate}", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Survey Mode: ${selectedCustomer!!.mappingType}", color = OnCharcoal, fontSize = 12.sp)
                        }
                    }

                    if (selectedLeadState != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Trigger Custom Sequence Stage:", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                (1..4).forEach { stage ->
                                    val active = selectedLeadState!!.currentStage == stage
                                    val label = when (stage) {
                                        1 -> "Email 1"
                                        2 -> "Email 2"
                                        3 -> "Email 3"
                                        else -> "Booked"
                                    }
                                    Surface(
                                        onClick = { viewModel.setLeadStageAdmin(stage) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (active) GoldPrimary else CharcoalBg,
                                        contentColor = if (active) CharcoalBg else OnCharcoal,
                                        border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
                                        modifier = Modifier.weight(1f).height(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedQuote != null) {
                        HorizontalDivider(color = Color(0x0DFFFFFF))
                        Text("Invoice Adjustment Control:", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("Services: ${selectedQuote!!.servicesSelected}", color = OnCharcoal, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Program: ${selectedQuote!!.packageSelected}", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = viewModel.formatCurrency(selectedQuote!!.quotedPrice),
                                color = GoldLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.updateAdminQuotePrice(selectedQuote!!.quotedPrice - 50.0) },
                                colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurfaceLight),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Text("Apply -${'$'}50", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { viewModel.updateAdminQuotePrice(selectedQuote!!.quotedPrice + 50.0) },
                                colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurfaceLight),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Text("Add +${'$'}50", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ServiceItem(
    val name: String,
    val desc: String,
    val icon: ImageVector
)

data class PackageItem(
    val name: String,
    val price: String,
    val desc: String,
    val perks: List<String>
)
