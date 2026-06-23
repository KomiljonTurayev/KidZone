package uz.kidzone.shared.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.kidzone.shared.SettingsProvider
import uz.kidzone.shared.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    settings: SettingsProvider,
    onDone: () -> Unit,
) {
    val vm = remember { OnboardingViewModel() }
    val state by vm.state.collectAsState()

    AnimatedContent(targetState = state.step, label = "onboarding_step") { step ->
        when (step) {
            0 -> LangStep(onSelect = { lang ->
                settings.setString("kz_lang", lang)
                vm.setLang(lang)
            })
            1 -> AgeStep(
                lang = state.lang,
                onSelect = { age ->
                    settings.setString("kz_age", age)
                    vm.setAge(age)
                    vm.nextStep()
                },
                onBack = { vm.prevStep() },
            )
            2 -> WelcomeStep(lang = state.lang, onDone = {
                settings.setBoolean("kz_onboarding_done", true)
                onDone()
            })
        }
    }
}

@Composable
private fun LangStep(onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🌍", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "Tilni tanlang / Выберите язык / Choose language",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(24.dp))
        listOf(
            "uz" to "Oʻzbek 🇺🇿",
            "ru" to "Русский 🇷🇺",
            "en" to "English 🇬🇧",
        ).forEach { (code, label) ->
            Button(
                onClick = { onSelect(code) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun AgeStep(lang: String, onSelect: (String) -> Unit, onBack: () -> Unit) {
    val title = when (lang) {
        "ru" -> "Выберите возраст"
        "en" -> "Select age group"
        else -> "Yosh guruhini tanlang"
    }
    val backLabel = when (lang) {
        "ru" -> "← Назад"
        "en" -> "← Back"
        else -> "← Orqaga"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("👶", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        listOf(
            "2-4" to "2–4 yosh 🐣",
            "5-7" to "5–7 yosh 🐤",
            "8+" to "8+ yosh 🐦",
        ).forEach { (code, label) ->
            Button(
                onClick = { onSelect(code) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Text(label)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(backLabel)
        }
    }
}

@Composable
private fun WelcomeStep(lang: String, onDone: () -> Unit) {
    val greeting = when (lang) {
        "ru" -> "Привет! Я Кидзо 🐥\nДобро пожаловать в KidZone!"
        "en" -> "Hi! I’m Kidzo 🐥\nWelcome to KidZone!"
        else -> "Salom! Men Kidzo 🐥\nKidZone'ga xush kelibsiz!"
    }
    val startLabel = when (lang) {
        "ru" -> "Начать 🚀"
        "en" -> "Let’s go 🚀"
        else -> "Boshlash 🚀"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(greeting, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(startLabel)
        }
    }
}
