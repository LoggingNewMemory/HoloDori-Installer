package com.kanagawa.yamada.holodoriinstaller

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kanagawa.yamada.holodoriinstaller.ui.theme.HoloDoriInstallerTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
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
            TopAppBar(
                title = { Text("HoloDori Installer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Installation Method", style = MaterialTheme.typography.titleMedium)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = !useRoot,
                    onClick = { viewModel.setUseRoot(false) }
                )
                Text("Shizuku (Status: ${if (shizukuAvailable) "Available" else "Not Running"})")
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = useRoot,
                    onClick = { viewModel.setUseRoot(true) }
                )
                Text("Root (Requires su)")
            }

            HorizontalDivider()
            
            Text("Download from APKPure", style = MaterialTheme.typography.titleMedium)
            
            if (downloadState == DownloadState.IDLE || downloadState == DownloadState.ERROR || downloadState == DownloadState.CANCELLED) {
                Button(
                    onClick = { viewModel.startApkPureDownload() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Download & Update (Latest Version)")
                }
                if (downloadState == DownloadState.ERROR) {
                    Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Download Status: $downloadState")
                        
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        
                        Text("${(progress * 100).toInt()}% (${downloadedBytes / 1024 / 1024}MB / ${if (totalBytes > 0) totalBytes / 1024 / 1024 else "?"}MB)")
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (downloadState == DownloadState.DOWNLOADING) {
                                Button(onClick = { viewModel.pauseDownload() }, modifier = Modifier.weight(1f)) {
                                    Text("Pause")
                                }
                            } else if (downloadState == DownloadState.PAUSED) {
                                Button(onClick = { viewModel.resumeDownload() }, modifier = Modifier.weight(1f)) {
                                    Text("Resume")
                                }
                            }
                            
                            Button(onClick = { viewModel.cancelDownload() }, modifier = Modifier.weight(1f)) {
                                Text("Cancel")
                            }
                        }
                        
                        if (downloadState == DownloadState.FINISHED) {
                            Button(
                                onClick = { viewModel.installDownloadedFile() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isInstalling
                            ) {
                                Text("Install Downloaded APK")
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Text("Install from Local File", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isInstalling
            ) {
                Text("Select .apk or .xapk File")
            }

            if (installStatus.isNotEmpty() || isInstalling) {
                HorizontalDivider()
                Text("Installation Status", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (isInstalling) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(bottom = 8.dp))
                        }
                        Text(installStatus)
                    }
                }
            }
        }
    }
}