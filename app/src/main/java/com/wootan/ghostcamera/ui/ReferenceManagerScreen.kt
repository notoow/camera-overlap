package com.wootan.ghostcamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wootan.ghostcamera.data.ReferencePhoto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceManagerScreen(
    references: List<ReferencePhoto>,
    loading: Boolean,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onSelect: (Int) -> Unit,
    onDelete: (ReferencePhoto) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<ReferencePhoto?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraBlack),
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text("기준 사진")
                        Text(
                            text = "${references.size}장",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "카메라로 돌아가기",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF111719),
                    navigationIconContentColor = SoftWhite,
                    titleContentColor = SoftWhite,
                ),
            )

            if (references.isEmpty()) {
                EmptyReferenceState(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding(),
                    onAdd = onAdd,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 16.dp,
                        bottom = 104.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(
                        items = references,
                        key = { _, photo -> photo.id },
                    ) { index, photo ->
                        ReferenceTile(
                            photo = photo,
                            number = index + 1,
                            canMovePrevious = index > 0,
                            canMoveNext = index < references.lastIndex,
                            onClick = { onSelect(index) },
                            onDelete = { pendingDelete = photo },
                            onMovePrevious = { onMove(index, index - 1) },
                            onMoveNext = { onMove(index, index + 1) },
                        )
                    }
                }
            }
        }

        if (references.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(24.dp),
                containerColor = GhostTeal,
                contentColor = Color(0xFF00201C),
                icon = {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                },
                text = { Text("사진 추가") },
            )
        }

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = GhostTeal)
            }
        }
    }

    pendingDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("기준 사진 삭제") },
            text = { Text("이 사진을 기준 목록에서 삭제할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(photo)
                    },
                ) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun EmptyReferenceState(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "등록된 기준 사진이 없습니다",
            style = MaterialTheme.typography.titleMedium,
            color = SoftWhite,
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("사진 추가")
        }
    }
}

@Composable
private fun ReferenceTile(
    photo: ReferencePhoto,
    number: Int,
    canMovePrevious: Boolean,
    canMoveNext: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMovePrevious: () -> Unit,
    onMoveNext: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF171D1F),
            contentColor = SoftWhite,
        ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
            ) {
                AsyncImage(
                    model = photo.file,
                    contentDescription = "$number 번 기준 사진",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = CircleShape,
                    color = Color(0xD9000000),
                ) {
                    Text(
                        text = number.toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 14.sp,
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color(0xB3000000), CircleShape),
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "$number 번 사진 삭제",
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onMovePrevious,
                    enabled = canMovePrevious,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "앞 순서로 이동",
                    )
                }
                Text(
                    text = "$number 번",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
                IconButton(
                    onClick = onMoveNext,
                    enabled = canMoveNext,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "뒤 순서로 이동",
                    )
                }
            }
        }
    }
}
