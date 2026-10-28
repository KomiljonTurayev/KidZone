package uz.kidzone.app.ui.screens

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import uz.kidzone.app.auth.AuthGraph
import uz.kidzone.app.auth.isSignedIn
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.ui.viewmodel.LoginViewModel
import uz.kidzone.app.ui.viewmodel.OnboardingStep
import uz.kidzone.app.ui.viewmodel.OnboardingViewModel
import uz.kidzone.app.ui.viewmodel.isAdultBirthYear
import java.util.Calendar

private val OnbBackground = Color(0xFFFFF8F0)
private val OnbText = Color(0xFF2B2B2B)
private val OnbBlue = Color(0xFF2196F3)
private val OnbOrange = Color(0xFFFF9F1C)
private val OnbGreen = Color(0xFF4CAF50)

/** Name the default profile gets before a parent names it (see KidZoneApplication). */
private const val DEFAULT_PROFILE_NAME = "Asosiy"

/**
 * First launch, parent-first: language → birth-year gate → sign in (or "later") → child profile
 * → hand the phone to the child. See [OnboardingViewModel].
 *
 * [profiles] lets the child step notice profiles restored from the cloud after a sign-in;
 * [onSaveChild] names the child's profile.
 */
@Composable
fun OnboardingScreen(
    prefs: SharedPreferences,
    profiles: List<ProfileEntity>,
    onSaveChild: (name: String, lang: String) -> Unit,
    onDone: () -> Unit,
    vm: OnboardingViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val t = onboardingTexts(state.lang)

    AnimatedContent(targetState = state.step, label = "onboarding_step") { step ->
        when (step) {
            OnboardingStep.LANGUAGE -> LangStep(onSelect = { lang ->
                prefs.edit().putString("kz_lang", lang).apply()
                vm.setLang(lang)
            })
            OnboardingStep.PARENT_GATE -> ParentGateStep(
                t = t,
                onVerified = {
                    val signedIn = runCatching { AuthGraph.auth.state.value.isSignedIn }.getOrDefault(false)
                    vm.parentVerified(alreadySignedIn = signedIn)
                },
                onBack = vm::back,
            )
            OnboardingStep.SIGN_IN -> {
                val loginViewModel: LoginViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { LoginViewModel(AuthGraph.auth, AuthGraph.telegramLogin, AuthGraph.googleSignIn) }
                    },
                )
                LoginScreen(
                    viewModel = loginViewModel,
                    title = t.signInTitle,
                    message = t.signInMessage,
                    skipLabel = t.later,
                    onSignedIn = vm::signInFinished,
                    onSkip = vm::signInFinished,
                    lang = state.lang,
                )
            }
            OnboardingStep.CHILD -> {
                val restored = profiles.filter { it.name != DEFAULT_PROFILE_NAME }
                if (restored.isNotEmpty()) {
                    RestoredProfilesStep(t = t, names = restored.map { it.name }, onContinue = {
                        vm.submitChild(restored.first().name, state.age)
                    })
                } else {
                    ChildStep(
                        t = t,
                        onSubmit = { name, age ->
                            prefs.edit().putString("kz_age", age).apply()
                            onSaveChild(name.trim(), state.lang)
                            vm.submitChild(name, age)
                        },
                        onBack = vm::back,
                    )
                }
            }
            OnboardingStep.HANDOFF -> HandoffStep(t = t, childName = state.childName, onDone = {
                prefs.edit().putBoolean("kz_onboarding_done", true).apply()
                onDone()
            })
        }
    }
}

@Composable
private fun StepColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OnbBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

@Composable
private fun BigButton(label: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(vertical = 4.dp),
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BackButton(label: String, onBack: () -> Unit) {
    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(label, fontSize = 16.sp, color = Color.Gray)
    }
}

@Composable
private fun LangStep(onSelect: (String) -> Unit) {
    StepColumn {
        Text("🌍", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "Tilni tanlang / Выберите язык / Choose language",
            style = MaterialTheme.typography.titleMedium,
            color = OnbText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        listOf(
            "uz" to "Oʻzbek 🇺🇿",
            "ru" to "Русский 🇷🇺",
            "en" to "English 🇬🇧",
        ).forEach { (code, label) ->
            BigButton(label, OnbBlue) { onSelect(code) }
        }
    }
}

@Composable
private fun ParentGateStep(t: OnboardingTexts, onVerified: () -> Unit, onBack: () -> Unit) {
    var year by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    StepColumn {
        Text("👨‍👩‍👧", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(t.parentTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            color = OnbText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(t.parentMessage, style = MaterialTheme.typography.bodyMedium, color = OnbText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = year,
            onValueChange = { v -> if (v.length <= 4 && v.all(Char::isDigit)) { year = v; failed = false } },
            label = { Text(t.birthYear) },
            isError = failed,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        if (failed) {
            Spacer(Modifier.height(8.dp))
            Text(t.askParent, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        BigButton(t.next, OnbOrange, enabled = year.length == 4) {
            if (isAdultBirthYear(year.toIntOrNull(), currentYear)) onVerified() else failed = true
        }
        BackButton(t.back, onBack)
    }
}

@Composable
private fun ChildStep(t: OnboardingTexts, onSubmit: (name: String, age: String) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf<String?>(null) }

    StepColumn {
        Text("🧒", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(t.childTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            color = OnbText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 24) name = it },
            label = { Text(t.childName) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Text(t.childAge, style = MaterialTheme.typography.titleSmall, color = OnbText)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("2-4" to "2–4 🐣", "5-7" to "5–7 🐤", "8+" to "8+ 🐦").forEach { (code, label) ->
                val selected = age == code
                Button(
                    onClick = { age = code },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) OnbOrange else Color.White,
                        contentColor = if (selected) Color.White else OnbText,
                    ),
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Text(label, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(24.dp))
        BigButton(t.next, OnbGreen, enabled = name.isNotBlank() && age != null) {
            onSubmit(name, age ?: return@BigButton)
        }
        BackButton(t.back, onBack)
    }
}

@Composable
private fun RestoredProfilesStep(t: OnboardingTexts, names: List<String>, onContinue: () -> Unit) {
    StepColumn {
        Text("☁️", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(t.restoredTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            color = OnbText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(names.joinToString(", "), style = MaterialTheme.typography.titleMedium, color = OnbText,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        BigButton(t.next, OnbGreen, onClick = onContinue)
    }
}

@Composable
private fun HandoffStep(t: OnboardingTexts, childName: String, onDone: () -> Unit) {
    StepColumn {
        Text("🎉", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(t.handoffTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            color = OnbText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(t.handoffMessage(childName), style = MaterialTheme.typography.bodyLarge, color = OnbText,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        BigButton(t.start, OnbGreen, onClick = onDone)
    }
}

private class OnboardingTexts(
    val parentTitle: String,
    val parentMessage: String,
    val birthYear: String,
    val askParent: String,
    val signInTitle: String,
    val signInMessage: String,
    val later: String,
    val childTitle: String,
    val childName: String,
    val childAge: String,
    val restoredTitle: String,
    val handoffTitle: String,
    val handoffMessage: (String) -> String,
    val start: String,
    val next: String,
    val back: String,
)

private fun onboardingTexts(lang: String): OnboardingTexts = when (lang) {
    "ru" -> OnboardingTexts(
        parentTitle = "Эту часть заполняет родитель",
        parentMessage = "Настройка займёт минуту. Потом передадите телефон ребёнку.",
        birthYear = "Ваш год рождения",
        askParent = "Пожалуйста, позовите маму или папу 🙂",
        signInTitle = "Аккаунт родителя",
        signInMessage = "Профили, прогресс и Premium сохранятся в аккаунте — даже если вы смените телефон.",
        later = "Позже",
        childTitle = "Кто будет играть?",
        childName = "Имя ребёнка",
        childAge = "Возраст",
        restoredTitle = "Профили восстановлены",
        handoffTitle = "Всё готово!",
        handoffMessage = { name -> if (name.isBlank()) "Передайте телефон ребёнку." else "Передайте телефон: $name" },
        start = "Начать 🚀",
        next = "Далее →",
        back = "← Назад",
    )
    "en" -> OnboardingTexts(
        parentTitle = "This part is for parents",
        parentMessage = "Setup takes a minute. Then hand the phone to your child.",
        birthYear = "Your year of birth",
        askParent = "Please ask a grown-up for help 🙂",
        signInTitle = "Parent account",
        signInMessage = "Profiles, progress and Premium are saved to your account — even if you change phones.",
        later = "Later",
        childTitle = "Who is going to play?",
        childName = "Child's name",
        childAge = "Age",
        restoredTitle = "Profiles restored",
        handoffTitle = "All set!",
        handoffMessage = { name -> if (name.isBlank()) "Hand the phone to your child." else "Hand the phone to $name." },
        start = "Let's go 🚀",
        next = "Next →",
        back = "← Back",
    )
    else -> OnboardingTexts(
        parentTitle = "Bu qismni ota-ona to'ldiradi",
        parentMessage = "Sozlash bir daqiqa oladi. Keyin telefonni bolangizga berasiz.",
        birthYear = "Tug'ilgan yilingiz",
        askParent = "Iltimos, ota-onangizni chaqiring 🙂",
        signInTitle = "Ota-ona hisobi",
        signInMessage = "Profillar, natijalar va Premium hisobingizda saqlanadi — telefon almashsa ham yo'qolmaydi.",
        later = "Keyinroq",
        childTitle = "Kim o'ynaydi?",
        childName = "Bolaning ismi",
        childAge = "Yoshi",
        restoredTitle = "Profillar tiklandi",
        handoffTitle = "Hammasi tayyor!",
        handoffMessage = { name -> if (name.isBlank()) "Telefonni bolangizga bering." else "Telefonni $name'ga bering." },
        start = "Boshlash 🚀",
        next = "Davom etish →",
        back = "← Orqaga",
    )
}
