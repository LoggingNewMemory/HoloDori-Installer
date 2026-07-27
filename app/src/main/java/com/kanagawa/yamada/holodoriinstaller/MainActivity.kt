package com.kanagawa.yamada.holodoriinstaller

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kanagawa.yamada.holodoriinstaller.ui.theme.HoloDoriInstallerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Keep screen on while the app is visible
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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

    val installStatus by viewModel.installStatus.collectAsState()
    val isInstalling by viewModel.isInstalling.collectAsState()
    val useRoot by viewModel.useRoot.collectAsState()
    val shizukuAvailable by viewModel.shizukuAvailable.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.installLocalFile(uri)
        }
    }

    Scaffold(
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
                actions = {
                    IconButton(onClick = { viewModel.checkShizuku() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh Shizuku status")
                    }
                }
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
                onDownload = { viewModel.startApkPureDownload() },
                onPause = { viewModel.pauseDownload() },
                onResume = { viewModel.resumeDownload() },
                onCancel = { viewModel.cancelDownload() },
                onInstall = { viewModel.installDownloadedFile() },
            )

            // ── Local File Section ──
            LocalFileCard(
                isInstalling = isInstalling,
                onPick = { filePickerLauncher.launch("*/*") },
            )

            // ── Installation Status ──
            AnimatedVisibility(
                visible = installStatus.isNotEmpty() || isInstalling,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                InstallStatusCard(
                    installStatus = installStatus,
                    isInstalling = isInstalling,
                )
            }
        }
    }
}

// ─── Install Method Card ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallMethodCard(
    useRoot: Boolean,
    shizukuAvailable: Boolean,
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
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Download from APKPure",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Always fetches the latest version",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            val isIdle = downloadState == DownloadState.IDLE ||
                         downloadState == DownloadState.ERROR ||
                         downloadState == DownloadState.CANCELLED

            if (isIdle) {
                // Big download button
                FilledTonalButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Download & Update", fontWeight = FontWeight.SemiBold)
                }

                // Error message
                AnimatedVisibility(visible = downloadState == DownloadState.ERROR) {
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
            } else {
                // Active download view
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
    }
}

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
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "progress",
    )
    val percentText = "${(progress * 100).toInt()}%"
    val dlMb = downloadedBytes / 1024 / 1024
    val totalMb = if (totalBytes > 0) totalBytes / 1024 / 1024 else null
    val sizeText = if (totalMb != null) "${dlMb}MB / ${totalMb}MB" else "${dlMb}MB / ?"

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
        Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
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
    installStatus: String,
    isInstalling: Boolean,
) {
    val isSuccess = installStatus.contains("success", ignoreCase = true) ||
                    installStatus.contains("complete", ignoreCase = true)
    val isError = installStatus.contains("fail", ignoreCase = true) ||
                  installStatus.contains("error", ignoreCase = true)

    val containerColor = when {
        isSuccess -> MaterialTheme.colorScheme.primaryContainer
        isError -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
        isError -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val statusIcon = when {
        isSuccess -> Icons.Filled.CheckCircle
        isError -> Icons.Filled.ErrorOutline
        else -> Icons.Filled.InstallMobile
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
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

            Spacer(Modifier.height(8.dp))

            Text(
                installStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        }
    }
}