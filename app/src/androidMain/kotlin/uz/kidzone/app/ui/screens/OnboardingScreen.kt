package uz.kidzone.app.ui.screens

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import uz.kidzone.app.auth.AuthGraph
import uz.kidzone.app.auth.isSignedIn
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.ui.components.Kid
import uz.kidzone.app.ui.components.KidButton
import uz.kidzone.app.ui.components.DigitBoxes
import uz.kidzone.app.ui.components.KidCard
import uz.kidzone.app.ui.components.KidError
import uz.kidzone.app.ui.components.KidNumberPad
import uz.kidzone.app.ui.components.KidPage
import uz.kidzone.app.ui.components.KidTextField
import uz.kidzone.app.ui.components.KidSubtitle
import uz.kidzone.app.ui.components.KidTitle
import uz.kidzone.app.ui.components.MascotBubble
import uz.kidzone.app.ui.components.StepHeader
import uz.kidzone.app.ui.viewmodel.LoginViewModel
import uz.kidzone.app.ui.viewmodel.OnboardingStep
import uz.kidzone.app.ui.viewmodel.OnboardingViewModel
import uz.kidzone.app.ui.viewmodel.isAdultBirthYear
import java.util.Calendar

/** Name the default profile gets before a parent names it (see KidZoneApplication). */
private const val DEFAULT_PROFILE_NAME = "Asosiy"

/** Steps after the language picker, shown as progress dots. */
private const val PARENT_STEPS = 4

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

    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            (slideInHorizontally { w -> if (forward) w / 3 else -w / 3 } + fadeIn()) togetherWith
                (slideOutHorizontally { w -> if (forward) -w / 3 else w / 3 } + fadeOut())
        },
        label = "onboarding_step",
    ) { step ->
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
                    emoji = "👨‍👩‍👧",
                    benefits = t.benefits,
                    header = { StepHeader(current = 1, total = PARENT_STEPS, onBack = vm::back) },
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
private fun LangStep(onSelect: (String) -> Unit) {
    KidPage {
        MascotBubble("🐥")
        Spacer(Modifier.height(24.dp))
        KidTitle("Salom! · Привет! · Hi!")
        Spacer(Modifier.height(6.dp))
        KidSubtitle("Tilni tanlang · Выберите язык · Choose a language")
        Spacer(Modifier.height(28.dp))
        listOf(
            Triple("uz", "🇺🇿", "Oʻzbekcha"),
            Triple("ru", "🇷🇺", "Русский"),
            Triple("en", "🇬🇧", "English"),
        ).forEach { (code, flag, label) ->
            KidCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                onClick = { onSelect(code) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(Kid.Peach),
                        contentAlignment = Alignment.Center,
                    ) { Text(flag, fontSize = 28.sp) }
                    Spacer(Modifier.width(16.dp))
                    Text(label, style = MaterialTheme.typography.titleLarge, color = Kid.Ink, modifier = Modifier.weight(1f))
                    Text("›", fontSize = 30.sp, color = Kid.Orange)
                }
            }
        }
    }
}

@Composable
private fun ParentGateStep(t: OnboardingTexts, onVerified: () -> Unit, onBack: () -> Unit) {
    var year by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    KidPage(top = { StepHeader(current = 0, total = PARENT_STEPS, onBack = onBack) }) {
        MascotBubble("🔒", size = 108.dp)
        Spacer(Modifier.height(20.dp))
        KidTitle(t.parentTitle)
        Spacer(Modifier.height(6.dp))
        KidSubtitle(t.parentMessage)
        Spacer(Modifier.height(24.dp))
        Text(t.birthYear, style = MaterialTheme.typography.titleMedium, color = Kid.InkSoft)
        Spacer(Modifier.height(10.dp))
        DigitBoxes(year, length = 4, error = failed)
        if (failed) {
            Spacer(Modifier.height(10.dp))
            KidError(t.askParent)
        }
        Spacer(Modifier.height(20.dp))
        KidNumberPad(
            onDigit = { d -> if (year.length < 4) { year += d; failed = false } },
            onDelete = { year = year.dropLast(1); failed = false },
        )
        Spacer(Modifier.height(20.dp))
        KidButton(t.next, enabled = year.length == 4) {
            if (isAdultBirthYear(year.toIntOrNull(), currentYear)) onVerified() else { failed = true; year = "" }
        }
    }
}

@Composable
private fun ChildStep(t: OnboardingTexts, onSubmit: (name: String, age: String) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf<String?>(null) }
    val ages = listOf(
        Triple("2-4", "🐣", t.ages[0]),
        Triple("5-7", "🐤", t.ages[1]),
        Triple("8+", "🦅", t.ages[2]),
    )

    KidPage(top = { StepHeader(current = 2, total = PARENT_STEPS, onBack = onBack) }) {
        MascotBubble(ages.firstOrNull { it.first == age }?.second ?: "🧒", size = 108.dp)
        Spacer(Modifier.height(20.dp))
        KidTitle(t.childTitle)
        Spacer(Modifier.height(24.dp))
        KidTextField(
            value = name,
            onValueChange = { if (it.length <= 24) name = it },
            placeholder = t.childName,
        )
        Spacer(Modifier.height(24.dp))
        Text(t.childAge, style = MaterialTheme.typography.titleMedium, color = Kid.InkSoft)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ages.forEach { (code, emoji, label) ->
                KidCard(
                    modifier = Modifier.weight(1f).height(120.dp),
                    selected = age == code,
                    onClick = { age = code },
                ) {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(emoji, fontSize = 40.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(label, style = MaterialTheme.typography.titleMedium,
                            color = if (age == code) Kid.Orange else Kid.Ink)
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        KidButton(t.next, brush = Kid.Meadow, enabled = name.isNotBlank() && age != null) {
            onSubmit(name, age ?: return@KidButton)
        }
    }
}

@Composable
private fun RestoredProfilesStep(t: OnboardingTexts, names: List<String>, onContinue: () -> Unit) {
    KidPage(top = { StepHeader(current = 2, total = PARENT_STEPS, onBack = null) }) {
        MascotBubble("☁️")
        Spacer(Modifier.height(24.dp))
        KidTitle(t.restoredTitle)
        Spacer(Modifier.height(16.dp))
        names.forEach { n ->
            KidCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Kid.Peach),
                        contentAlignment = Alignment.Center,
                    ) { Text(n.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = Kid.Orange) }
                    Spacer(Modifier.width(14.dp))
                    Text(n, style = MaterialTheme.typography.titleLarge, color = Kid.Ink)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        KidButton(t.next, brush = Kid.Meadow, onClick = onContinue)
    }
}

@Composable
private fun HandoffStep(t: OnboardingTexts, childName: String, onDone: () -> Unit) {
    KidPage(top = { StepHeader(current = 3, total = PARENT_STEPS, onBack = null) }) {
        Confetti()
        MascotBubble("🎉", size = 148.dp)
        Spacer(Modifier.height(28.dp))
        KidTitle(t.handoffTitle)
        Spacer(Modifier.height(8.dp))
        KidSubtitle(t.handoffMessage(childName))
        Spacer(Modifier.height(36.dp))
        KidButton(t.start, brush = Kid.Meadow, onClick = onDone)
    }
}

/** A row of party emojis bouncing out of phase. */
@Composable
private fun Confetti() {
    val party = rememberInfiniteTransition(label = "confetti")
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 20.dp)) {
        listOf("🎈", "⭐", "🎨", "🧸", "🌈").forEachIndexed { i, e ->
            val dy by party.animateFloat(
                initialValue = 0f, targetValue = -14f,
                animationSpec = infiniteRepeatable(tween(700 + i * 130, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "confetti_$i",
            )
            Text(e, fontSize = 30.sp, modifier = Modifier.offset(y = dy.dp))
        }
    }
}

private class OnboardingTexts(
    val parentTitle: String,
    val parentMessage: String,
    val birthYear: String,
    val askParent: String,
    val signInTitle: String,
    val signInMessage: String,
    val benefits: List<String>,
    val later: String,
    val childTitle: String,
    val childName: String,
    val childAge: String,
    val ages: List<String>,
    val restoredTitle: String,
    val handoffTitle: String,
    val handoffMessage: (String) -> String,
    val start: String,
    val next: String,
)

private fun onboardingTexts(lang: String): OnboardingTexts = when (lang) {
    "ru" -> OnboardingTexts(
        parentTitle = "Эту часть заполняет родитель",
        parentMessage = "Настройка займёт минуту — потом передадите телефон ребёнку.",
        birthYear = "Ваш год рождения",
        askParent = "Пожалуйста, позовите маму или папу 🙂",
        signInTitle = "Аккаунт родителя",
        signInMessage = "Всё важное сохранится, даже если вы смените телефон.",
        benefits = listOf("Профили детей и прогресс", "Отчёты и лимиты времени", "Kidzo AI и Premium"),
        later = "Позже",
        childTitle = "Кто будет играть?",
        childName = "Имя ребёнка",
        childAge = "Сколько лет?",
        ages = listOf("2–4", "5–7", "8+"),
        restoredTitle = "Профили восстановлены",
        handoffTitle = "Всё готово!",
        handoffMessage = { name -> if (name.isBlank()) "Передайте телефон ребёнку." else "Передайте телефон: $name" },
        start = "Начать играть 🚀",
        next = "Далее",
    )
    "en" -> OnboardingTexts(
        parentTitle = "This part is for parents",
        parentMessage = "Setup takes a minute — then hand the phone to your child.",
        birthYear = "Your year of birth",
        askParent = "Please ask a grown-up for help 🙂",
        signInTitle = "Parent account",
        signInMessage = "Everything important is kept, even if you change phones.",
        benefits = listOf("Child profiles and progress", "Reports and time limits", "Kidzo AI and Premium"),
        later = "Maybe later",
        childTitle = "Who's going to play?",
        childName = "Child's name",
        childAge = "How old?",
        ages = listOf("2–4", "5–7", "8+"),
        restoredTitle = "Profiles restored",
        handoffTitle = "All set!",
        handoffMessage = { name -> if (name.isBlank()) "Hand the phone to your child." else "Hand the phone to $name." },
        start = "Let's play 🚀",
        next = "Next",
    )
    else -> OnboardingTexts(
        parentTitle = "Bu qismni ota-ona to'ldiradi",
        parentMessage = "Sozlash bir daqiqa oladi — keyin telefonni bolangizga berasiz.",
        birthYear = "Tug'ilgan yilingiz",
        askParent = "Iltimos, ota-onangizni chaqiring 🙂",
        signInTitle = "Ota-ona hisobi",
        signInMessage = "Telefon almashsa ham hammasi saqlanib qoladi.",
        benefits = listOf("Bola profillari va natijalari", "Hisobotlar va vaqt limiti", "Kidzo AI va Premium"),
        later = "Keyinroq",
        childTitle = "Kim o'ynaydi?",
        childName = "Bolaning ismi",
        childAge = "Necha yoshda?",
        ages = listOf("2–4", "5–7", "8+"),
        restoredTitle = "Profillar tiklandi",
        handoffTitle = "Hammasi tayyor!",
        handoffMessage = { name -> if (name.isBlank()) "Telefonni bolangizga bering." else "Telefonni $name'ga bering." },
        start = "O'ynashni boshlash 🚀",
        next = "Davom etish",
    )
}
