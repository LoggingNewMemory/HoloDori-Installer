package com.kanagawa.yamada.holodoriinstaller

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kanagawa.yamada.holodoriinstaller.ui.theme.HoloDoriInstallerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            HoloDoriInstallerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkShizuku()
    }
}

// ─── Main Screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel) {
    val downloadState by viewModel.downloader.downloadState.collectAsState()
    val progress by viewModel.downloader.progress.collectAsState()
    val downloadedBytes by viewModel.downloader.downloadedBytes.collectAsState()
    val totalBytes by viewModel.downloader.totalBytes.collectAsState()
    val errorMessage by viewModel.downloader.errorMessage.collectAsState()

    val installLogs by viewModel.installLogs.collectAsState()
    val isInstalling by viewModel.isInstalling.collectAsState()
    val useRoot by viewModel.useRoot.collectAsState()
    val shizukuAvailable by viewModel.shizukuAvailable.collectAsState()
    val rootAvailable by viewModel.rootAvailable.collectAsState()

    val installedVersion by viewModel.installedVersion.collectAsState()
    val latestVersion by viewModel.latestVersion.collectAsState()
    val isCheckingVersion by viewModel.isCheckingVersion.collectAsState()
    val isUpdateCheckEnabled by viewModel.isUpdateCheckEnabled.collectAsState()

    // Keep screen on only while downloading or installing
    val keepOn = downloadState == DownloadState.DOWNLOADING || isInstalling
    val view = LocalView.current
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.installLocalFile(uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setUpdateCheckEnabled(true)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "HoloDori",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Installer",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                ),
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Install Method Card ──
            InstallMethodCard(
                useRoot = useRoot,
                shizukuAvailable = shizukuAvailable,
                rootAvailable = rootAvailable,
                onSelectShizuku = { viewModel.setUseRoot(false) },
                onSelectRoot = { viewModel.setUseRoot(true) },
            )

            // ── Download Section ──
            DownloadCard(
                downloadState = downloadState,
                progress = progress,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                errorMessage = errorMessage,
                isInstalling = isInstalling,
                installedVersion = installedVersion,
                latestVersion = latestVersion,
                isCheckingVersion = isCheckingVersion,
                isUpdateCheckEnabled = isUpdateCheckEnabled,
                onRefreshVersion = { viewModel.refreshVersionInfo() },
                onToggleUpdateCheck = { enabled ->
                    if (enabled) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setUpdateCheckEnabled(true)
                        }
                    } else {
                        viewModel.setUpdateCheckEnabled(false)
                    }
                },
                onDownload = { viewModel.startApkPureDownload() },
                onPause = { viewModel.pauseDownload() },
                onResume = { viewModel.resumeDownload() },
                onCancel = { viewModel.cancelDownload() },
                onInstall = { viewModel.installDownloadedFile() },
            )

            // ── Local File Section ──
            val isDownloadIdle = downloadState == DownloadState.IDLE ||
                    downloadState == DownloadState.ERROR ||
                    downloadState == DownloadState.CANCELLED

            AnimatedVisibility(
                visible = isDownloadIdle,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                LocalFileCard(
                    isInstalling = isInstalling,
                    onPick = { filePickerLauncher.launch("*/*") },
                )
            }

            // ── Installation Status ──
            AnimatedVisibility(
                visible = installLogs.isNotEmpty() || isInstalling,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                InstallStatusCard(
                    installLogs = installLogs,
                    isInstalling = isInstalling,
                )
            }

            // ── Footer Credit ──
            FooterCredit()
        }
    }
}

// ─── Install Method Card ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallMethodCard(
    useRoot: Boolean,
    shizukuAvailable: Boolean,
    rootAvailable: Boolean,
    onSelectShizuku: () -> Unit,
    onSelectRoot: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Install Method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(14.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !useRoot,
                    onClick = onSelectShizuku,
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = !useRoot) {
                            Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    },
                ) {
                    Text("Shizuku")
                }
                SegmentedButton(
                    selected = useRoot,
                    onClick = onSelectRoot,
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = useRoot) {
                            Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    },
                ) {
                    Text("Root")
                }
            }

            // Shizuku status — only shown when Shizuku is selected
            AnimatedVisibility(visible = !useRoot) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (shizukuAvailable) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (shizukuAvailable) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (shizukuAvailable) "Shizuku is running" else "Shizuku is not running",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Root status — only shown when Root is selected
            AnimatedVisibility(visible = useRoot) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (rootAvailable) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (rootAvailable) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (rootAvailable) "Root access granted" else "Root not available",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─── Download Card ──────────────────────────────────────────────────────────

@Composable
fun DownloadCard(
    downloadState: DownloadState,
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    errorMessage: String,
    isInstalling: Boolean,
    installedVersion: String?,
    latestVersion: String?,
    isCheckingVersion: Boolean,
    isUpdateCheckEnabled: Boolean,
    onRefreshVersion: () -> Unit,
    onToggleUpdateCheck: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Download from APKPure",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))

            // Version info row
            VersionInfoRow(
                installedVersion = installedVersion,
                latestVersion = latestVersion,
                isCheckingVersion = isCheckingVersion,
                onRefresh = onRefreshVersion,
            )

            Spacer(Modifier.height(16.dp))

            val isIdle = downloadState == DownloadState.IDLE ||
                         downloadState == DownloadState.ERROR ||
                         downloadState == DownloadState.CANCELLED

            AnimatedContent(
                targetState = isIdle,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically { it / 4 })
                        .togetherWith(fadeOut(tween(150)))
                },
                label = "downloadContent",
            ) { idle ->
                if (idle) {
                    Column {
                        // Download button
                        val hasUpdate = latestVersion != null && installedVersion != null && latestVersion != installedVersion
                        val notInstalled = installedVersion == null

                        FilledTonalButton(
                            onClick = onDownload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    notInstalled -> "Download & Install"
                                    hasUpdate -> "Download Update"
                                    else -> "Download & Reinstall"
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // Error
                        AnimatedVisibility(
                            visible = downloadState == DownloadState.ERROR,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    DownloadProgressSection(
                        downloadState = downloadState,
                        progress = progress,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes,
                        isInstalling = isInstalling,
                        onPause = onPause,
                        onResume = onResume,
                        onCancel = onCancel,
                        onInstall = onInstall,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Notify if Update Available",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Switch(
                    checked = isUpdateCheckEnabled,
                    onCheckedChange = onToggleUpdateCheck
                )
            }
        }
    }
}

// ─── Version Info Row ───────────────────────────────────────────────────────

@Composable
fun VersionInfoRow(
    installedVersion: String?,
    latestVersion: String?,
    isCheckingVersion: Boolean,
    onRefresh: () -> Unit,
) {
    // Spinning animation for the refresh icon
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spinAngle",
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Installed
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Installed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    installedVersion ?: "Not installed",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (installedVersion != null) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Arrow
            Icon(
                @Suppress("DEPRECATION")
                Icons.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.width(8.dp))

            // Latest
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Latest",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isCheckingVersion) {
                    Text(
                        "Checking…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        latestVersion ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Refresh button
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Refresh versions",
                    modifier = Modifier
                        .size(18.dp)
                        .then(if (isCheckingVersion) Modifier.rotate(spinAngle) else Modifier),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── Download Progress Section ──────────────────────────────────────────────

@Composable
fun DownloadProgressSection(
    downloadState: DownloadState,
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    isInstalling: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
) {
    Column {
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "progress",
        )
        val percentText = "${(progress * 100).toInt()}%"
        val dlMb = downloadedBytes / 1024 / 1024
        val totalMb = if (totalBytes > 0) totalBytes / 1024 / 1024 else null
        val sizeText = if (totalMb != null) "${dlMb}MB / ${totalMb}MB" else "${dlMb}MB / ?"

        // Pulsing alpha for the status icon while downloading
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseAlpha",
        )

        // Status row
        Row(verticalAlignment = Alignment.CenterVertically) {
            val statusIcon = when (downloadState) {
                DownloadState.DOWNLOADING -> Icons.Filled.Downloading
                DownloadState.PAUSED -> Icons.Filled.Pause
                DownloadState.FINISHED -> Icons.Filled.CheckCircle
                else -> Icons.Filled.HourglassEmpty
            }
            val statusColor = when (downloadState) {
                DownloadState.FINISHED -> MaterialTheme.colorScheme.primary
                DownloadState.PAUSED -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val iconAlpha = if (downloadState == DownloadState.DOWNLOADING) pulseAlpha else 1f

            Icon(
                statusIcon,
                contentDescription = null,
                tint = statusColor.copy(alpha = iconAlpha),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                when (downloadState) {
                    DownloadState.DOWNLOADING -> "Downloading…"
                    DownloadState.PAUSED -> "Paused"
                    DownloadState.FINISHED -> "Download complete"
                    else -> downloadState.name
                },
                style = MaterialTheme.typography.labelLarge,
                color = statusColor,
            )
            Spacer(Modifier.weight(1f))
            Text(
                percentText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(8.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            sizeText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            when (downloadState) {
                DownloadState.DOWNLOADING -> {
                    FilledTonalButton(
                        onClick = onPause,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pause")
                    }
                }
                DownloadState.PAUSED -> {
                    FilledTonalButton(
                        onClick = onResume,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Resume")
                    }
                }
                DownloadState.FINISHED -> {
                    Button(
                        onClick = onInstall,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isInstalling,
                    ) {
                        Icon(Icons.Filled.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Install")
                    }
                }
                else -> {}
            }

            if (downloadState != DownloadState.FINISHED) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = if (downloadState == DownloadState.DOWNLOADING || downloadState == DownloadState.PAUSED)
                        Modifier.weight(1f) else Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel")
                }
            }
        }
    }
}

// ─── Local File Card ────────────────────────────────────────────────────────

@Composable
fun LocalFileCard(
    isInstalling: Boolean,
    onPick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Install from Local File",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Pick your own .apk or .xapk file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onPick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isInstalling,
            ) {
                Icon(Icons.Outlined.FileOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Browse Files", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Install Status Card ────────────────────────────────────────────────────

@Composable
fun InstallStatusCard(
    installLogs: List<String>,
    isInstalling: Boolean,
) {
    val lastLog = installLogs.lastOrNull() ?: ""
    val isSuccess = lastLog.contains("success", ignoreCase = true) ||
                    lastLog.contains("complete", ignoreCase = true)
    val isError = lastLog.contains("fail", ignoreCase = true) ||
                  lastLog.contains("error", ignoreCase = true)

    val containerColor = when {
        isSuccess -> MaterialTheme.colorScheme.primaryContainer
        isError -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
        isError -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusIcon = when {
        isSuccess -> Icons.Filled.CheckCircle
        isError -> Icons.Filled.ErrorOutline
        else -> Icons.Filled.InstallMobile
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isInstalling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp,
                        color = contentColor,
                    )
                } else {
                    Icon(
                        statusIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    if (isInstalling) "Installing…" else if (isSuccess) "Success" else if (isError) "Error" else "Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Display logs in a terminal-like block
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    installLogs.forEach { log ->
                        Text(
                            text = "> $log",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = contentColor,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Footer Credit ──────────────────────────────────────────────────────────

@Composable
fun FooterCredit() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "By: Kanagawa Yamada",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Yamada's YouTube",
                style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.Underline),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@KanagawaYamada")))
                },
            )
            Text(
                "GitHub Source",
                style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.Underline),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/LoggingNewMemory/HoloDori-Installer")))
                },
            )
        }
    }
}