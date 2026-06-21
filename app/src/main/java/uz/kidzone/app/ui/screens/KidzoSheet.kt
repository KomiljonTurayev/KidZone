package uz.kidzone.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.kidzone.app.kidzo.ContentCard
import uz.kidzone.app.ui.viewmodel.KidzoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidzoSheet(
    viewModel: KidzoViewModel,
    onContentSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()) {

            Text("🐥 Kidzo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Content cards
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cards) { card ->
                    KidzoCardItem(card = card, onClick = {
                        onContentSelected(card.contentId)
                        onDismiss()
                    })
                }
            }

            Spacer(Modifier.height(12.dp))

            // Chat messages (last 3)
            messages.takeLast(3).forEach { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Chat input
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Savol bering...") },
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                }) {
                    Text("→")
                }
            }
        }
    }
}

@Composable
private fun KidzoCardItem(card: ContentCard, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(120.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(card.emoji, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = card.displayText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
    }
}
