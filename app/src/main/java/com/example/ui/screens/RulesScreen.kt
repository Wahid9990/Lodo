package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Official Rules Guide", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RuleCard(
                icon = Icons.Default.Casino,
                iconColor = Color(0xFFE53935),
                title = "1. Rolling a Six",
                description = "A token can leave the home yard only when you roll a 6. Rolling a 6 also grants an extra turn! (Rolling three 6s in a row forfeits your turn)."
            )

            RuleCard(
                icon = Icons.Default.Security,
                iconColor = Color(0xFF43A047),
                title = "2. Capturing Tokens",
                description = "Landing on an opponent token on a normal cell captures it and sends it back to its yard! Capturing grants you an immediate extra turn."
            )

            RuleCard(
                icon = Icons.Default.Star,
                iconColor = Color(0xFFFFB300),
                title = "3. Safe Zones",
                description = "Start tiles and Star tiles are safe zones! Tokens occupying safe tiles cannot be captured by opponents."
            )

            RuleCard(
                icon = Icons.Default.Refresh,
                iconColor = Color(0xFF1E88E5),
                title = "4. Home Stretch & Finish",
                description = "After completing a full circuit, your tokens enter your colored Home Stretch. An exact dice roll is required to enter the center Home finish (56)."
            )

            RuleCard(
                icon = Icons.Default.EmojiEvents,
                iconColor = Color(0xFFFF9800),
                title = "5. Winning",
                description = "The first player to bring all 4 of their tokens safely into the center Home triangle wins the game!"
            )
        }
    }
}

@Composable
private fun RuleCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.padding(6.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
