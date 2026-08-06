package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AiDifficulty
import com.example.model.GameState
import com.example.model.PlayerColor
import com.example.model.PlayerType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    savedGame: GameState?,
    onQuickPlay: () -> Unit,
    onCustomSetup: () -> Unit,
    onNavigateWifiMultiplayer: () -> Unit,
    onResumeGame: (GameState) -> Unit,
    onNavigateStats: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateRules: () -> Unit,
    onNavigateAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ludo Master",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Graphic Banner
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFE53935)))
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF43A047)))
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFFDD835)))
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF1E88E5)))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "LUDO MASTER",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFB300),
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Classic Board Game • Offline PvP & Smart AI",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Resume Game Button (If saved state exists)
            if (savedGame != null) {
                Button(
                    onClick = { onResumeGame(savedGame) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF43A047),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .testTag("resume_game_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Resume Game",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "RESUME SAVED GAME",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Quick Play Button (vs 3 AI players)
            Button(
                onClick = onQuickPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .testTag("quick_play_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Quick Play",
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "QUICK PLAY (VS AI)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Custom Game Setup Button
            OutlinedButton(
                onClick = onCustomSetup,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("custom_setup_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Custom Setup",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "CUSTOM GAME SETUP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // Local Wi-Fi / Hotspot Multiplayer Button
            Button(
                onClick = onNavigateWifiMultiplayer,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .testTag("wifi_multiplayer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.WifiTethering,
                    contentDescription = "Wi-Fi Multiplayer",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "LOCAL WI-FI MULTIPLAYER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Quick Actions Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Statistics
                Card(
                    onClick = onNavigateStats,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .testTag("stats_button")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Statistics",
                            tint = Color(0xFF1E88E5)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Statistics",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Rules
                Card(
                    onClick = onNavigateRules,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .testTag("rules_button")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Rules",
                            tint = Color(0xFF43A047)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rules Guide",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // About
                Card(
                    onClick = onNavigateAbout,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .testTag("about_button")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = Color(0xFFE53935)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "About",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
