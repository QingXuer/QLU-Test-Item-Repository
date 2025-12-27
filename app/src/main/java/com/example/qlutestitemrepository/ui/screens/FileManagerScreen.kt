package com.example.qlutestitemrepository.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.qlutestitemrepository.R
import com.example.qlutestitemrepository.util.FileUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    navController: NavController
) {
    var currentPath by remember { mutableStateOf("") }
    var fileList by remember { mutableStateOf(FileUtils.listFiles(currentPath)) }
    val context = LocalContext.current

    // Refresh list when path changes
    LaunchedEffect(currentPath) {
        fileList = FileUtils.listFiles(currentPath)
    }

    // Handle Back Press
    BackHandler(enabled = currentPath.isNotEmpty()) {
        val parent = File(currentPath).parent
        currentPath = if (parent == null || parent == ".") "" else parent
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件管理") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Path Breadcrumb (Optional, but helpful)
            Text(
                text = if (currentPath.isEmpty()) "Root" else currentPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Back to parent directory item
                if (currentPath.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clickable {
                                    val parent = File(currentPath).parent
                                    currentPath = if (parent == null || parent == ".") "" else parent
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = "返回上一级", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                if (fileList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("空文件夹")
                        }
                    }
                }

                items(fileList) { file ->
                    FileItemRow(
                        file = file,
                        onFolderClick = { 
                             val newPath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                             currentPath = newPath
                        },
                        onDelete = {
                            if (FileUtils.deleteFile(file)) {
                                Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                                fileList = FileUtils.listFiles(currentPath) // Refresh
                            } else {
                                Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenOther = { FileUtils.openWithOther(context, file) },
                        onPreview = { 
                            // Reuse FileUtils.openFile for internal preview if possible, or generic open
                            // FileUtils.openFile takes remoteUrl, which we don't have here. 
                            // But looking at FileUtils.openFile, it calls getTestsFile(name) and tries to open uri.
                            // We can use a simpler version here since we HAVE the file object.
                            // Let's use openWithOther as fallback or create a new internal open helper?
                            // Actually, let's just use openWithOther for now, or replicate the intent logic.
                            // Requirement: "App内预览".
                            // FileUtils.openFile does: Intent(ACTION_VIEW).
                            // Let's call that logic directly.
                            try {
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension) ?: "*/*")
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法预览: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onExport = { FileUtils.exportFile(context, file) }
                    )
                }
            }
        }
    }
}

@Composable
fun FileItemRow(
    file: File,
    onFolderClick: () -> Unit,
    onDelete: () -> Unit,
    onOpenOther: () -> Unit,
    onPreview: () -> Unit,
    onExport: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable {
                if (file.isDirectory) {
                    onFolderClick()
                } else {
                    onPreview()
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(FileUtils.formatDate(file.lastModified()))
                        if (!file.isDirectory) {
                            append("  |  ")
                            append(FileUtils.formatSize(file.length()))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (file.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("删除", color = Color.Red) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.Red) },
                            onClick = {
                                expanded = false
                                onDelete()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("用其他应用打开") },
                            leadingIcon = { Icon(Icons.Filled.OpenInNew, contentDescription = null) },
                            onClick = {
                                expanded = false
                                onOpenOther()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("App内预览") },
                            leadingIcon = { Icon(Icons.Filled.Visibility, contentDescription = null) },
                            onClick = {
                                expanded = false
                                onPreview()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("分享") },
                            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                            onClick = {
                                expanded = false
                                onExport() // Reuse export logic which is essentially share
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导出") },
                            leadingIcon = { Icon(Icons.Filled.Output, contentDescription = null) },
                            onClick = {
                                expanded = false
                                onExport()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = Color.Red) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.Red) },
                            onClick = {
                                expanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
