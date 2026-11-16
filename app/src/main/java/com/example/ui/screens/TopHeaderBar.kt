package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TournamentAccount
import com.example.ui.theme.CricketPrimary
import com.example.ui.theme.CricketPrimaryLight
import com.example.ui.theme.OutfitFontFamily

@Composable
fun TopHeaderBar(
    currentUser: TournamentAccount?,
    isSyncing: Boolean = false,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onChangePassClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CricketPrimary,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(CricketPrimary, CricketPrimaryLight)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title and subtitle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "🏏 Rajdevi Mayor Cup 2083",
                        color = Color.White,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Player Dashboard · 9 Ward Teams",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Header actions (no settings option)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (currentUser != null) {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (currentUser.role == "admin") "Admin" else "Ward ${currentUser.ward}",
                                color = Color.White,
                                fontFamily = OutfitFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = onChangePassClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = "Change Password",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        IconButton(
                            onClick = onLogoutClick,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("logout_button")
                        ) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onLoginClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = CricketPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("login_button")
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "Login",
                                fontFamily = OutfitFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefreshClick,
                        enabled = !isSyncing,
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
