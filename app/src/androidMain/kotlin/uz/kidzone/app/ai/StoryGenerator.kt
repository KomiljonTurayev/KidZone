package uz.kidzone.app.ai

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeneratedStory(val title: String, val text: String)

/**
 * Generates children's stories for KidZone.
 *
 * Generation priority:
 * 1. KidZone backend's /ai/story endpoint (backend holds the Gemini key server-side —
 *    never embed it in the client, it's trivially extractable from the APK).
 * 2. High-quality Offline PersonalizedStoryEngine that dynamically crafts stories in Uzbek, Russian, and English
 *    incorporating the child's name and specified scenario.
 */
object StoryGenerator {

    private const val TAG = "StoryGenerator"
    private val BACKEND_URL = "${uz.kidzone.app.BackendConfig.BASE_URL}/ai/story"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    suspend fun generate(
        lang: String,
        ageRange: String,
        childName: String = "",
        scenario: String = "",
    ): GeneratedStory {
        val safeName = childName.trim().take(30)
        val safeScenario = scenario.trim().take(200)

        val backendStory = generateViaBackend(lang, ageRange, safeName, safeScenario)
        if (backendStory != null) {
            Log.d(TAG, "Story successfully generated via backend")
            return backendStory
        }

        // Robust PersonalizedStoryEngine (always succeeds and respects childName & scenario)
        Log.d(TAG, "Generating story via PersonalizedStoryEngine for name='$safeName', scenario='$safeScenario'")
        return PersonalizedStoryEngine.generate(lang, ageRange, safeName, safeScenario)
    }

    private fun generateViaBackend(
        lang: String,
        ageRange: String,
        childName: String,
        scenario: String,
    ): GeneratedStory? {
        return try {
            val idToken = currentIdToken() ?: return null

            val requestJson = JSONObject().apply {
                put("lang", lang)
                put("ageRange", ageRange)
                put("childName", childName)
                put("scenario", scenario)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(BACKEND_URL)
                .addHeader("Authorization", "Bearer $idToken")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Backend story HTTP failed: ${response.code}")
                    return null
                }
                val respBody = response.body?.string() ?: return null
                val root = JSONObject(respBody)
                val title = root.optString("title").trim()
                val text = root.optString("text").trim()
                if (title.isNotEmpty() && text.isNotEmpty()) GeneratedStory(title, text) else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend story call failed: ${e.message}")
            null
        }
    }

    private fun currentIdToken(): String? = try {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        Tasks.await(user.getIdToken(false)).token
    } catch (e: Exception) {
        Log.w(TAG, "getIdToken failed: ${e.message}")
        null
    }

}

/**
 * High-quality procedural children's story engine that guarantees personal stories
 * matching childName and scenario even when offline or without cloud credentials.
 */
object PersonalizedStoryEngine {

    private var lastVariantIndex = -1

    fun generate(lang: String, ageRange: String, childName: String, scenario: String): GeneratedStory {
        val nextVariant = getNextVariantIndex()
        return when (lang) {
            "ru" -> generateRu(childName, scenario, nextVariant)
            "en" -> generateEn(childName, scenario, nextVariant)
            else -> generateUz(childName, scenario, nextVariant)
        }
    }

    private fun getNextVariantIndex(): Int {
        var idx = kotlin.random.Random.nextInt(5)
        if (idx == lastVariantIndex) {
            idx = (idx + 1) % 5
        }
        lastVariantIndex = idx
        return idx
    }

    private fun cleanTitle(scenario: String): String {
        val cleaned = scenario.replace(Regex("[.,!?;:]"), "").trim()
        val words = cleaned.split(" ").filter { it.isNotBlank() }
        return if (words.isEmpty()) {
            "Ajoyib Sarguzasht"
        } else {
            words.take(4).joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
        }
    }

    private fun generateUz(childName: String, scenario: String, variant: Int): GeneratedStory {
        val name = childName.ifBlank { "Jasur" }
        val sc = scenario.trim()
        val scClean = cleanTitle(scenario)

        return when (variant % 5) {
            0 -> {
                // 1. Sirli Xazina va Topqirlik
                val title = if (sc.isNotBlank()) "$name va $scClean: Sirli Topilma" else "$name va Oltin Kalit Sirlari"
                val p1 = "Kunlarning birida $name o'zining shinam uyida kitob varaqlab o'tirib, g'aroyib bir xaritaga ko'zi tushdi. $name har doim sarguzashtlarni yaxshi ko'rgani uchun, darhol bu sirni o'rganishga qaror qildi."
                val p2 = if (sc.isNotBlank()) {
                    "Xaritadagi qadimiy belgilar to'g'ridan-to'g'ri \"$sc\" tomon yetaklardi. $name sumkachasiga zarur narsalarni solib, yo'lga otlandi. Har bir qadamda yangi hayratlanarli mo'jizalar uni kutib turardi."
                } else {
                    "Xarita qalin o'rmon qa'ridagi sirli yashil darvozani ko'rsatardi. $name sumkachasini olib, dadil qadamlar bilan mo'jizalar olami sari yo'l oldi."
                }
                val p3 = "Yo'lda ${name}ga chigal jumboq duch keldi. Lekin bizning aqlli qahramonimiz shoshilmadi — u diqqat bilan o'ylab, topqirlik qildi va sirli eshikning qulfini ochish yo'lini topdi. Uning sabr-toqati va zehni barchani lol qoldirdi."
                val p4 = if (sc.isNotBlank()) {
                    "Eshik ortida esa haqiqiy mo'jiza yashiringan ekan! $name \"$sc\" orqali ko'zlangan buyuk maqsadiga erishdi. U yerda kutib turgan do'stlari $name sharafiga bayramona quvonch ulashdilar."
                } else {
                    "Eshik ochilgach, ichkarida minglab porloq kitoblar va bilim javohirlari bilan to'la sirli kutubxona paydo bo'ldi. Bu yer bilimdonlar maskani edi."
                }
                val p5 = "Uyga qaytgan ${name}ning qalbi cheksiz quvonchga to'ldi. U tushundiki, dunyodagi eng katta xazina — bu oltin yoki boylik emas, balki topqirlik, teran aql va yangi bilimlardir!"
                GeneratedStory(title, "$p1\n\n$p2\n\n$p3\n\n$p4\n\n$p5")
            }
            1 -> {
                // 2. Do'stlik va Jamoaviy Sarguzasht
                val title = if (sc.isNotBlank()) "$name va Do'stlar: $scClean" else "$name va Quvnoq Jamoa"
                val p1 = "Ertalabki quyosh nurlari zaminni yoritganda, $name quvnoq kayfiyatda ko'chaga chiqdi. Bugun odatiy kun emas, balki haqiqiy do'stlik sinovidan o'tadigan ajoyib sarguzasht kuni edi."
                val p2 = if (sc.isNotBlank()) {
                    "Do'stlari bilan birga to'plangan $name ularga o'zining ajoyib rejasi — \"$sc\" haqida so'zlab berdi. Barcha do'stlar bir ovozdan bu qiziqarli ishga qo'shilishga qaror qilishdi va qo'l berib ahdlashdilar."
                } else {
                    "Bog'da sayr qilib yurgan $name daraxt tepasida yig'lab o'tirgan mittivoy sincobchani ko'rib qoldi. Do'stlari bilan maslahatlashib, unga yordam berishga oshiqdilar."
                }
                val p3 = "Sayohat davomida kutilmagan to'siq chiqdi: keng daryodan o'tish lozim edi. $name darhol jamoani birlashtirdi — biri novdalarni ushladi, boshqasi yo'l ko'rsatdi, $name esa ularni ruhlantirib, xavfsiz o'tish yo'lini barpo qildi."
                val p4 = if (sc.isNotBlank()) {
                    "Bir yoqadan bosh chiqarib harakat qilgan ahil jamoa \"$sc\" orzusini to'liq ro'yobga chiqardi! Do'stlar bir-birlarini quchoqlab, quvonchdan sakrashdi. ${name}ning yetakchiligi hamma uchun katta ilhom bo'ldi."
                } else {
                    "Birgalikdagi sa'y-harakat tufayli mittivoy sincobcha xavfsiz yerga tushirildi va o'z oilasiga yetkazildi. Sincobchaning onasi $name va do'stlariga eng shirin yong'oqlarni ulashdi."
                }
                val p5 = "Kechki salqinda uyga qaytgan $name shuni angladiki: yolg'iz qilingan ish qiyin bo'lishi mumkin, ammo sadoqatli do'stlar bilan har qanday qiyinchilik zavqli g'alabaga aylanadi!"
                GeneratedStory(title, "$p1\n\n$p2\n\n$p3\n\n$p4\n\n$p5")
            }
            2 -> {
                // 3. Zukko Ixtirochi va Yangi Kashfiyot
                val title = if (sc.isNotBlank()) "$name: $scClean Kashfiyoti" else "$name va Sehrli Ixtiro"
                val p1 = "$name bolaligidanoq buyumlar qanday tuzilganini bilishga qiziqardi. Uning xonasi turli chizmalar, qiziqarli modellar va rang-barang asboblar bilan to'la edi."
                val p2 = if (sc.isNotBlank()) {
                    "Bir kuni ${name}ning xayoliga ajoyib fikr keldi. U \"$sc\" g'oyasini amalga oshirish uchun maxsus mo'jizakor asbob yasashga kirishdi. U chizdi, o'lchadi va astoydil mehnat qildi."
                } else {
                    "Bir kuni $name shamol kuchi bilan yuradigan va qushlar tilini tushunadigan kichkina quvnoq robot yasashga qaror qildi."
                }
                val p3 = "Boshida hamma narsa o'xshayvermadi: ba'zi qismlar uzildi, chiroqlar yonmay qoldi. Lekin $name taslim bo'ladiganlardan emasdi! U xatolarini qunt bilan to'g'riladi, yangi usullarni sinab ko'rdi va oxir-oqibat o'z asbobini ishga tushirdi."
                val p4 = if (sc.isNotBlank()) {
                    "Mo'jizakor ixtiro charaqlovchi nurlar taratib, \"$sc\" jarayonini misli ko'rilmagan darajada oson va hayratlanarli qildi! Atrofdagilar ${name}ning mahoratiga qoyil qolib, unga tasannolar aytishdi."
                } else {
                    "Mitti robot yaltirab ishga tushdi va qushlar bilan xushmuomala suhbatlasha boshladi. Butun bog' ahli ${name}ning kashfiyotini olqishladi."
                }
                val p5 = "$name faxr va baxt tuyg'usiga to'ldi. U bildiki, qiyinchiliklar — bu faqat o'sish imkoniyati, mehnatsevarlik va sabr esa har qanday orzuni haqiqatga aylantiradi!"
                GeneratedStory(title, "$p1\n\n$p2\n\n$p3\n\n$p4\n\n$p5")
            }
            3 -> {
                // 4. Koinot va Sehrli Parvoz
                val title = if (sc.isNotBlank()) "$name va Koinotdagi $scClean" else "$name va Yulduzlar Oroli"
                val p1 = "Kechqurun osmonga qarab, miltillagan yulduzlarni sanashni $name juda sevardi. Har bir yulduz go'yo unga sirli sarguzashtlardan ertak aytayotgandek tuyulardi."
                val p2 = if (sc.isNotBlank()) {
                    "Shu kecha ${name}ning derazasiga kumush rang nurlar tushdi va unga orzuidagi \"$sc\" sayohatini boshlash uchun sehrli imkoniyat taqdim etildi. $name hecham ikkilanmay, quvonch bilan bu taklifni qabul qildi."
                } else {
                    "Shu kecha uning derazasiga kamalak rangida tovlanuvchi mitti parivash kelib, yulduzlar oroliga sayohat qilishni taklif etdi."
                }
                val p3 = "Baland-baland ko'klarga ko'tarilib, bulutlar ustida sayr qilar ekan, $name koinotning cheksiz go'zalligiga guvoh bo'ldi. U yo'lda adashib qolgan mittivoy yulduzchaga yo'l ko'rsatib, uning qayta charaqlashiga yordam berdi."
                val p4 = if (sc.isNotBlank()) {
                    "Nihoyat, yulduzlar shukuhida \"$sc\" rejasining eng qiziq nuqtasiga yetib kelindi. $name o'zining ezgu niyatlari bilan bu sarguzashtni unutilmas mo'jizaga aylantirdi va butun olamga iliqlik ulashdi."
                } else {
                    "Yulduzlar oroliga yetgach, $name ezgu orzular daraxtiga o'z tilagini bildirdi va daraxt yorqin nurlar sochib, barcha bolalarga shodlik taratdi."
                }
                val p5 = "Ertalab qushlar sayrog'i bilan uyg'ongan ${name}ning yuzida tabassum porlardi. U tushundiki, eng go'zal orzular yaxshi niyat bilan boshlanadi va haqiqiy mo'jiza bizning qalbimizda yashaydi!"
                GeneratedStory(title, "$p1\n\n$p2\n\n$p3\n\n$p4\n\n$p5")
            }
            else -> {
                // 5. Tabiat va Mehribonlik Qal'asi
                val title = if (sc.isNotBlank()) "$name va Mo'jizaviy $scClean" else "$name va Tabiat Saxovati"
                val p1 = "Bahor tongida butun tabiat uyg'onib, gullar xushbo'y ifor taratayotgan edi. $name tabiat bag'rida sayr qilishni, shabada shivirlashini va daryo oqishini tinglashni yaxshi ko'rardi."
                val p2 = if (sc.isNotBlank()) {
                    "Bu safargi sayr esa ajoyib bir maqsad bilan boshlandi: $name \"$sc\" haqidagi ezgu rejani amalga oshirish uchun eng chiroyli vodiy sari yo'l oldi."
                } else {
                    "Sayr chog'ida $name chanqab qolgan kichkina gullarni ko'rib, buloqdan suv keltirib ularni sug'orishga kirishdi."
                }
                val p3 = "Yo'lda u turli jonzotlarni uchratdi. $name har biriga samimiy munosabatda bo'ldi, o'zining mehrini ayamadi. Qiyin dovonlarda tabiat unga go'yo yordam bergandek, mayin shabada yo'l ko'rsatib turdi."
                val p4 = if (sc.isNotBlank()) {
                    "Mehr-oqibat va ezgulik tufayli \"$sc\" sarguzashti kutilgandanda yuksak darajada amalga oshdi! Jonajon o'lka gullab-yashnadi, barcha qushlar va hayvonlar ${name}ni do'st deb e'tirof etdilar."
                } else {
                    "Gullar minnatdor bo'lib yaproqlarini yozdi va ulardan xushbo'y kamalak nurlari taraldi. Butun bog' yorishib ketdi."
                }
                val p5 = "Kechqurun ufqqa botayotgan quyoshga qarab, $name juda muhim haqiqatni his etdi: ezgulik hech qachon yo'qolmaydi, uning aksi doim o'zimizga ikki hissa bo'lib qaytadi!"
                GeneratedStory(title, "$p1\n\n$p2\n\n$p3\n\n$p4\n\n$p5")
            }
        }
    }

    private fun generateRu(childName: String, scenario: String, variant: Int): GeneratedStory {
        val name = childName.ifBlank { "Максим" }
        val sc = scenario.trim()
        val scClean = cleanTitle(scenario)

        return when (variant % 5) {
            0 -> {
                val title = if (sc.isNotBlank()) "$name и Тайна $scClean" else "$name и Золотой Ключик"
                val p1 = "В один прекрасный день $name нашёл старинную карту в любимой книге сказок. Любознательный $name сразу понял: впереди ждёт настоящее приключение!"
                val p2 = if (sc.isNotBlank()) {
                    "Стрелки на карте указывали путь к цели — \"$sc\". С горящими глазами $name собрал походный рюкзачок и сделал первый решительный шаг."
                } else {
                    "Карта вела к таинственной изумрудной двери на опушке леса. $name храбро зашагал навстречу неизведанному."
                }
                val p3 = "На пути возникла сложная загадка. Но $name не растерялся — проявив сообразительность и терпение, он сумел разгадать древний секрет. Его ум и смекалка помогли открыть заветную дверь."
                val p4 = if (sc.isNotBlank()) {
                    "За дверью открылось удивительное зрелище! Задумка \"$sc\" исполнилась наилучшим образом. Все вокруг аплодировали находчивости $name."
                } else {
                    "Внутри оказалась волшебная библиотека знаний, где каждая книга сияла мягким светом мудрости."
                }
                val p5 = "$name вернулся домой с радостным сердцем. Он понял, что самое ценное сокровище на свете — это знания, ум и верность своим добрым мечтам!"
                GeneratedStory(title, "$p1\n\n$p2\n\n$p3\n\n$p4\n\n$p5")
            }
            1 -> {
                val title = if (sc.isNotBlank()) "$name и Дружная Команда: $scClean" else "$name и Верные Друзья"
                val p1 = "Утреннее солнце ласково заглянуло в окно, и $name вышел во двор с отличным настроением. Сегодня предстоял особенный день — день настоящей дружбы!"
                val p2 = if (sc.isNotBlank()) {
                    "Собрав своих верных друзей, $name поделился грандиозной идеей — \"$sc\". Ребята дружно поддержали план и взялись за дело с задором."
                } else {
                    "В парке $name заметил грустного птенца, который выпал из гнезда. Вместе с друзьями они сразу решили помочь малышу."
                }
                val p3 = "Вскоре путь преградила бурная река. $name организовал общую работу: кто-то держал ветки, кто-то строил надёжную переправу. Вместе они преодолели преграду."
                val p4 = if (sc.isNotBlank()) {
                    "Благодаря сплоченности команды цель \"$sc\" была с триумфом достигнута! Радостный смех и улыбки озарили лица всех участников."
                } else {
                    "Птенец был бережно возвращен в теплое гнездо к маме. Мама-птица весело запела в знак глубокой благодарности."
                }
                val p5 = "Вечером $name понял самое главное: в одиночку путь бывает труден, но с верными друзьями любая вершина покоряется легко и весело!"
                GeneratedStory(title, "$p1\n\n$p2\n\n$p3\n\n$p4\n\n$p5")
            }
            else -> {
                val title = if (sc.isNotBlank()) "$name и Чудесное $scClean" else "$name и Волшебное Приключение"
                val p1 = "$name с детства обожал мечтать и узнавать, как устроен окружающий мир. Его доброе сердце всегда искало возможности сделать что-то полезное."
                val p2 = if (sc.isNotBlank()) {
                    "И вот представился идеальный случай: $name решил осуществить задуманное приключение — \"$sc\"."
                } else {
                    "Гуляя по цветущему саду, $name встретил сказочную бабочку, крылья которой сияли цветами радуги."
                }
                val p3 = "Несмотря на неожиданные испытания на пути, $name сохранял оптимизм и доброту, поддерживая каждого, кому требовалась помощь."
                val p4 = if (sc.isNotBlank()) {
                    "Смелость и упорство принесли долгожданный успех! Приключение \"$sc\" завершилось ярким триумфом."
                } else {
                    "Бабочка привела его к волшебному роднику доброты, дарящему радость всем лесным жителям."
                }
                val p5 = "Этот день подарил $name важный урок: доброта и смелость открывают двери к самым невероятным чудесам!"
                GeneratedStory(title, "$p1\n\n$p2\n\n$p3\n\n$p4\n\n$p5")
            }
        }
    }

    private fun generateEn(childName: String, scenario: String, variant: Int): GeneratedStory {
        val name = childName.ifBlank { "Leo" }
        val sc = scenario.trim()
        val scClean = cleanTitle(scenario)

        return when (variant % 5) {
            0 -> {
                val title = if (sc.isNotBlank()) "$name and the Mystery of $scClean" else "$name and the Golden Compass"
                val p1 = "One morning, while exploring an old book of wonders, $name discovered a fascinating treasure map. With excitement bubbling in their heart, $name set out on an incredible journey."
                val p2 = if (sc.isNotBlank()) {
                    "The shimmering compass pointed toward \"$sc\". Without hesitation, $name packed a small bag and took the first brave step."
                } else {
                    "The map led toward an enchanted green archway deep inside the whispering woods."
                }
                val p3 = "A clever riddle stood in the path. Instead of rushing, $name thought carefully, used great patience, and solved the mystery with a bright smile."
                val p4 = if (sc.isNotBlank()) {
                    "Beyond the ancient archway lay the grand result! The goal of \"$sc\" was achieved with flying colors, celebrated by cheering companions."
                } else {
                    "The door swung open to reveal a dazzling library of starlight wisdom and endless stories."
                }
                val p5 = "Returning home under the twilight sky, $name learned that the greatest treasure is not gold, but wisdom, courage, and a curious mind!"
                GeneratedStory(title, "$p1\n\n$p2\n\n$p3\n\n$p4\n\n$p5")
            }
            1 -> {
                val title = if (sc.isNotBlank()) "$name & Friends: The $scClean Quest" else "$name and the Team of Champions"
                val p1 = "The golden sun rose over the hills, and $name greeted the new day full of positive energy, ready for a test of true teamwork."
                val p2 = if (sc.isNotBlank()) {
                    "Gathering the best friends together, $name shared a thrilling plan: \"$sc\". Everyone cheered and promised to support each other."
                } else {
                    "Walking through the park, $name noticed a little bird that needed help reaching its cozy nest."
                }
                val p3 = "When an obstacle blocked their way, $name united the group. Working side by side, they turned a hard challenge into an easy, fun team effort."
                val p4 = if (sc.isNotBlank()) {
                    "With everyone contributing their unique strengths, the mission of \"$sc\" succeeded beyond expectations!"
                } else {
                    "Together, they safely returned the little bird to its family, greeted by cheerful songs of gratitude."
                }
                val p5 = "Looking back on this amazing day, $name knew that together with good friends, there is no hurdle that cannot be overcome with joy!"
                GeneratedStory(title, "$p1\n\n$p2\n\n$p3\n\n$p4\n\n$p5")
            }
            else -> {
                val title = if (sc.isNotBlank()) "$name's Great $scClean Adventure" else "$name and the Starlight Miracle"
                val p1 = "Kind-hearted and inventive, $name was always looking for ways to bring joy and wonder into the world."
                val p2 = if (sc.isNotBlank()) {
                    "Today marked the beginning of a special dream: \"$sc\". $name stepped forward with determination and curiosity."
                } else {
                    "A gentle breeze carried a magical whispering leaf right to $name's open hand."
                }
                val p3 = "Through clever ideas and unfailing kindness, $name overcame every twist and turn in the road."
                val p4 = if (sc.isNotBlank()) {
                    "The adventure of \"$sc\" blossomed into a magnificent celebration that inspired everyone around."
                } else {
                    "It revealed a hidden spring of living flowers glowing in peaceful rainbow colors."
                }
                val p5 = "$name smiled warmly, knowing that real magic happens whenever you share your kindness and never stop believing!"
                GeneratedStory(title, "$p1\n\n$p2\n\n$p3\n\n$p4\n\n$p5")
            }
        }
    }
}
