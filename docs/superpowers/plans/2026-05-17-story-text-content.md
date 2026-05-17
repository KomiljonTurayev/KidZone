# Story Text Content Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `text: {uz, ru, en}` field to all 20 stories in content.json so tapping a story card shows the full story text in the existing ai-viewer modal and reads it aloud.

**Architecture:** Single file change — `app/src/main/assets/www/content.json`. No code changes needed. `StoryManager._play()` already checks `item.text` and renders it in `#ai-viewer` with `_doSpeak()`.

**Tech Stack:** JSON, Android WebView, Gradle assembleDebug, adb install

---

## File Map

| Action | File |
|--------|------|
| Modify | `app/src/main/assets/www/content.json` — add `text:{uz,ru,en}` to stories 001–010 |
| Modify | `app/src/main/assets/www/content.json` — add `text:{uz,ru,en}` to stories 011–020 |

---

## Task 1: Add text to stories 001–010

**Files:**
- Modify: `app/src/main/assets/www/content.json`

- [ ] **Step 1: Read content.json and add `text` to story-001**

For story-001 (`"id":"story-001"`), add a `"text"` field after `"audio"`:

```json
"text":{
  "uz":"Bir kuni kuchli sher uxlab yotgan edi. Kichkina sichqon uning ustidan yugurib o'tib, sherni uyg'otib yubordi. Sher g'azablanib sichqonni ushladi.\n\n\"Meni qo'yib yubor! Men bir kun senga yordam beraman,\" dedi sichqon. Sher kuldi, lekin uni qo'yib yubordi.\n\nKo'p o'tmay sher ovchilarning to'riga tushib qoldi. U qattiq bo'kirdi. Sichqon ovozni eshitib yugurdi va arqonlarni kemirdi. Sher ozod bo'ldi.\n\n\"Rahmat, kichkina do'st,\" dedi sher. \"Kichkina bo'lsang ham — katta yurak borligini ko'rdim.\" Do'stlik kattayu kichikni tanimasligi shundan.",
  "ru":"Однажды могучий лев спал в лесу. Маленькая мышка пробежала прямо по его носу и разбудила его. Лев рассердился и схватил её.\n\n\"Отпусти меня! Однажды я помогу тебе,\" — пискнула мышка. Лев засмеялся, но всё же отпустил её.\n\nВскоре лев попал в сеть охотников и громко зарычал. Мышка услышала и прибежала. Она грызла верёвки, пока лев не освободился.\n\n\"Спасибо, маленький друг,\" — сказал лев. \"Я не знал, что в таком маленьком сердце столько храбрости.\" Дружба не знает ни больших, ни маленьких.",
  "en":"One day a mighty lion lay sleeping in the forest. A tiny mouse ran right across his nose and woke him up. The lion grabbed her in anger.\n\n\"Let me go! I will help you someday,\" squeaked the mouse. The lion laughed but set her free.\n\nSoon the lion was caught in a hunter's net and roared loudly. The mouse heard him and came running. She gnawed through the ropes until the lion was free.\n\n\"Thank you, little friend,\" said the lion. \"I never knew such a small heart could hold so much courage.\" True friendship knows no size."
}
```

- [ ] **Step 2: Add `text` to story-002**

```json
"text":{
  "uz":"Katta fil o'rmonda yurganida kichkina chumolini ko'rib kuldi: \"Sen juda kichkiksan, hech narsaga yaramaysan!\"\n\nChumoli jim tura olmadi: \"Kattayu kichik emas, aql va mehnat muhim.\"\n\nBir kuni fil botqoqqa botib qoldi. U qancha urinmasin, chiqa olmadi. Chumoli o'z do'stlarini chaqirdi. Yuzlab chumoli bir yoqadan bosh chiqarish bilan zig'irlar va o'tlarni tortib berdi. Fil shu bilan tayanib chiqib oldi.\n\n\"Kechirasiz, men noto'g'ri edim,\" dedi fil. \"Siz kichik, lekin kuchli ekansizlar.\" O'shandan beri fil chumoli bilan do'st bo'lib yashadi.",
  "ru":"Большой слон шёл по лесу и увидел маленького муравья. \"Ты такой крошечный, от тебя никакой пользы!\" — засмеялся слон.\n\nМуравей не промолчал: \"Важны не размеры, а ум и трудолюбие.\"\n\nОднажды слон застрял в болоте. Как он ни старался, выбраться не мог. Муравей позвал друзей. Сотни муравьёв дружно потянули ветки и траву. Слон ухватился за них и выбрался.\n\n\"Простите меня, я был неправ,\" — сказал слон. \"Вы маленькие, но такие сильные.\" С тех пор слон и муравей стали лучшими друзьями.",
  "en":"A big elephant was walking through the forest when he spotted a tiny ant and laughed. \"You are so small, you are useless!\"\n\nThe ant stood firm: \"Size does not matter — wisdom and hard work do.\"\n\nOne day the elephant got stuck in a muddy swamp and could not get out. The ant called his friends. Hundreds of ants pulled branches and vines together. The elephant grabbed them and climbed out.\n\n\"I'm sorry, I was wrong,\" said the elephant. \"You are small but so incredibly strong.\" From that day on, the elephant and the ant became the best of friends."
}
```

- [ ] **Step 3: Add `text` to story-003**

```json
"text":{
  "uz":"Quyon toshbaqani ko'rganida doim masxara qilardi: \"Sen shu qadar sekin yurasan!\"\n\nToshbaqa tabassum qilib dedi: \"Poyga qilib ko'raylik.\" Barcha hayvonlar tomosha qilish uchun keldi.\n\nStart berildi. Quyon shiddat bilan yo'lga tushdi. U o'rtada to'xtab, soya ostida uxlab qoldi. Toshbaqa esa to'xtamay, qadamba qadam yurdi.\n\nUzoq vaqt o'tgach, toshbaqa finishga birinchi yetib keldi. Quyon uyg'onib ko'rdi — kech edi.\n\n\"Matonat — tezlikdan muhim,\" dedi toshbaqa. Barcha hayvonlar unga qarsak chalishdi.",
  "ru":"Заяц всегда смеялся над черепахой: \"Ты такая медленная!\"\n\nЧерепаха улыбнулась: \"Давай устроим гонку.\" Все звери собрались посмотреть.\n\nГонка началась. Заяц помчался вперёд, а черепаха медленно, но уверенно шла за ним. На полпути заяц остановился отдохнуть в тени дерева и уснул. Черепаха шла не останавливаясь.\n\nКогда заяц проснулся — было уже поздно. Черепаха пересекла финишную черту первой.\n\n\"Настойчивость важнее скорости,\" — сказала черепаха. Все звери захлопали в ладоши.",
  "en":"The hare always mocked the tortoise: \"You are so slow!\"\n\nThe tortoise smiled. \"Let us have a race.\" All the animals gathered to watch.\n\nThe race began. The hare shot ahead while the tortoise walked slowly but steadily. Halfway through, the hare stopped to rest under a tree and fell asleep. The tortoise kept walking without stopping.\n\nWhen the hare woke up, it was too late. The tortoise crossed the finish line first.\n\n\"Persistence matters more than speed,\" said the tortoise. All the animals cheered."
}
```

- [ ] **Step 4: Add `text` to story-004**

```json
"text":{
  "uz":"Bir kuni och tulki tokzordan o'tib bordi. U shoxlarda osilib turgan to'q qizil uzumlarni ko'rdi. \"Qanday mazali bo'lsa kerak!\" deb o'yladi.\n\nTulki yuqoriga sakradi, lekin yetolmadi. Yana bir bor urindi — yana yetolmadi. Ko'p marta uringanidan so'ng, charchab ketdi.\n\nTulki boshini ko'tarib dedi: \"Bu uzumlar yashil va nordon, shuning uchun ularga keragi yo'q.\" So'ng u o'tib ketdi.\n\nLekin ichidan bilardi — uzumlar to'q va shirin edi. O'zini aldash — haqiqatni o'zgartirmaydi. Muvaffaqiyatsizlikni tan olish ham katta jasorat.",
  "ru":"Однажды голодная лиса проходила мимо виноградника. Она увидела на ветках крупные спелые гроздья. \"Какие, должно быть, вкусные!\" — подумала она.\n\nЛиса прыгнула вверх, но не достала. Попробовала ещё раз — снова ничего. После многих попыток она устала.\n\nЛиса подняла голову и сказала: \"Эти ягоды зелёные и кислые, они мне не нужны.\" Затем ушла прочь.\n\nНо в глубине души она знала — виноград был спелым и сладким. Обманывать себя не изменит правды. Признать неудачу — тоже большая смелость.",
  "en":"One day a hungry fox was walking past a vineyard. She saw large, ripe clusters of grapes hanging from the branches. \"How delicious they must be!\" she thought.\n\nThe fox jumped up but could not reach them. She tried again and again. After many attempts she grew tired.\n\nThe fox raised her head and said, \"Those grapes are green and sour — I do not want them anyway.\" Then she walked away.\n\nBut deep inside she knew the grapes were ripe and sweet. Fooling yourself does not change the truth. Admitting failure takes courage too."
}
```

- [ ] **Step 5: Add `text` to story-005**

```json
"text":{
  "uz":"O'rmonda yashil bargli baland bir daraxt o'sardi. Bolalar unga \"Sehrli Daraxt\" deb ism qo'yishgan edi. Chunki har kim uning ostida o'tirib, biror yaxshi ish qilsa — daraxt ularga meva berardi.\n\nBir kuni kichkina Aziz yo'lda yig'layotgan bolani ko'rib, unga o'yinchoq berdi. Keyin daraxt tagiga keldi. Daraxt shoxlarini silkitdi — oltin olma tushdi.\n\nAziz uyga qaytib onasiga berdi. Onasi tabassum qilib dedi: \"Bu meva mehnatingning in'omi.\"\n\nAziz tushundi: mehribonlik qilsang, hayot senga ham mehribon bo'ladi.",
  "ru":"В лесу росло высокое дерево с зелёными листьями. Дети назвали его \"Волшебным деревом\". Ведь каждый, кто сидел под ним и делал доброе дело, получал от него плоды.\n\nОднажды маленький Азиз увидел плачущего мальчика и отдал ему свою игрушку. Потом подошёл к дереву. Дерево встряхнуло ветвями — упало золотое яблоко.\n\nАзиз принёс его домой маме. Мама улыбнулась: \"Этот плод — награда за твою доброту.\"\n\nАзиз понял: если ты добр к другим, жизнь тоже будет добра к тебе.",
  "en":"Deep in the forest stood a tall tree with bright green leaves. The children called it the Magic Tree. Whoever sat beneath it and did a kind deed would receive its fruit as a gift.\n\nOne day little Aziz saw a crying boy and gave him his own toy. Then he went to the tree. The tree shook its branches and a golden apple fell down.\n\nAziz carried it home to his mother. She smiled: \"This fruit is the reward for your kindness.\"\n\nAziz understood — if you are kind to others, life will be kind to you too."
}
```

- [ ] **Step 6: Add `text` to story-006**

```json
"text":{
  "uz":"Yomg'ir to'xtaganidan so'ng kichkina Malika derazadan tashqariga qaradi. Osmonda ajoyib kamalak paydo bo'ldi. U shoshib hovliga chiqdi.\n\n\"Kamalak qayerdan chiqadi?\" deb so'radi u otasidan. Otasi dedi: \"Quyosh nuri yomg'ir tomchilariga teganda — kamalak tug'iladi.\"\n\nMalika juda hayron qoldi. U shundoq o'tirib, kamalakni kuzatdi. U qizil, to'q sariq, sariq, yashil, ko'k ranglardan iborat edi.\n\n\"Bu tabiatning rasmi,\" dedi otasi. Malika o'yladi: \"Demak, har bir yomg'irdan keyin — go'zallik keladi.\" U o'shandan beri yomg'irni sevib qoldi.",
  "ru":"После того как дождь закончился, маленькая Малика выглянула в окно. На небе появилась радуга. Она выбежала во двор.\n\n\"Откуда берётся радуга?\" — спросила она у папы. Папа ответил: \"Когда солнечный свет встречает капли дождя, рождается радуга.\"\n\nМалика была поражена. Она молча наблюдала за радугой. Та была красной, оранжевой, жёлтой, зелёной и синей.\n\n\"Это картина природы,\" — сказал папа. Малика задумалась: \"Значит, после каждого дождя приходит красота.\" С тех пор она полюбила дождь.",
  "en":"After the rain stopped, little Malika looked out the window. A beautiful rainbow had appeared in the sky. She ran outside.\n\n\"Where does the rainbow come from?\" she asked her father. Father said, \"When sunlight touches raindrops, a rainbow is born.\"\n\nMalika was amazed. She sat quietly and watched. It was red, orange, yellow, green, and blue.\n\n\"It is nature's painting,\" said her father. Malika thought: \"So after every rain, beauty comes.\" From that day on, she loved the rain."
}
```

- [ ] **Step 7: Add `text` to story-007**

```json
"text":{
  "uz":"Qishning oxiri edi. Kichkina Dono har kuni derazadan qarab, qorni kutardi. Bir ertalab uyg'onib ko'rsa — qor erib ketibdi, daraxtlar kurtak chiqaribdi.\n\nU yugurgilab hovliga chiqdi. Yer yashil o'tlar bilan qoplinib kelardi. Qushlar sayrardi. Gullarda asalarilar uchib yurardi. \"Bahor keldi!\" deb qichqirdi Dono.\n\nOnasi unga dedi: \"Tabiat har yili uyg'onadi. Huddi sen ertalab uyg'onganing kabi.\"\n\nDono gul ekdi. Kunlar o'tdi — gul o'sdi, ochildi. Dono endi tabiatni yanada yaxshi tushunardi: har narsa o'z vaqtida gullaydi.",
  "ru":"Была последняя неделя зимы. Маленькая Дона каждый день смотрела в окно. Но однажды утром она проснулась и увидела: снег растаял, на деревьях появились почки.\n\nОна выбежала во двор. Земля покрывалась зелёной травой. Птицы пели. На цветах кружились пчёлы. \"Пришла весна!\" — закричала Дона.\n\nМама сказала: \"Природа просыпается каждый год. Как и ты просыпаешься каждое утро.\"\n\nДона посадила цветок. Прошли дни — цветок вырос и расцвёл. Теперь Дона лучше понимала природу: всё расцветает в своё время.",
  "en":"It was the last week of winter. Little Dona looked out the window every day. Then one morning she woke up to find the snow had melted and buds had appeared on the trees.\n\nShe ran into the garden. The ground was turning green. Birds were singing. Bees buzzed around the flowers. \"Spring is here!\" Dona cried.\n\nHer mother said, \"Nature wakes up every year. Just like you wake up every morning.\"\n\nDona planted a flower. Days passed and it grew and bloomed. Now she understood nature better — everything blossoms in its own time."
}
```

- [ ] **Step 8: Add `text` to story-008**

```json
"text":{
  "uz":"Qish kuni qor yog'di. Akbar va singlisi Zulfiya hovliga chiqib qorbobo yasashdi. Ularga ikkita tosh — ko'zlari, sabzi — burni, eski ro'mol — bo'yniga bog'lashdi.\n\n\"U juda chiroyli!\" dedi Zulfiya. Ular har kuni qorboboni ko'rgani chiqardilar.\n\nBir kuni bahor keldi, qorbobo erib ketdi. Zulfiya yig'ladi. Akbar uni yupatdi: \"U eridi, lekin bizning xotiramizda qoladi.\"\n\nZulfiya o'yladi. \"To'g'ri. Biz uni yasadik, demak u doim bizda.\" Keyingi qishda ular yangi qorbobo yasadilar — bu safar undan ham chiroyliroq.",
  "ru":"Зимой выпал снег. Акбар и его сестра Зулфия вышли во двор лепить снеговика. Они сделали ему глаза из камней, нос из морковки и повязали старый шарф.\n\n\"Он такой красивый!\" — сказала Зулфия. Каждый день они выходили смотреть на своего снеговика.\n\nНо однажды пришла весна, и снеговик растаял. Зулфия заплакала. Акбар утешил её: \"Он растаял, но останется в нашей памяти.\"\n\nЗулфия подумала. \"Верно. Мы его слепили, значит, он всегда будет с нами.\" Следующей зимой они слепили нового снеговика — ещё красивее.",
  "en":"It snowed in winter. Akbar and his sister Zulfiya went outside to build a snowman. They gave him stone eyes, a carrot nose, and tied an old scarf around his neck.\n\n\"He is so beautiful!\" said Zulfiya. Every day they went out to see him.\n\nBut one day spring came and the snowman melted. Zulfiya cried. Akbar comforted her: \"He melted, but he lives in our memory.\"\n\nZulfiya thought about it. \"That's true. We made him, so he is always with us.\" The next winter they built a new snowman — even more beautiful than before."
}
```

- [ ] **Step 9: Add `text` to story-009**

```json
"text":{
  "uz":"Kamol juda qo'rqoq bola edi. U qorong'udan, baland joylardan va notanish odamlardan qo'rqardi.\n\nBir kuni bog'da kichkina mushukcha daraxtga chiqib qoldi va tushay olmadi. U miyovlab yig'lardi. Hamma o'tib ketdi, hech kim to'xtamadi.\n\nKamol to'xtadi. Yurak urib ketdi. Lekin mushukchaning ko'zlarini ko'rdi — u ham qo'rqayotgan edi. Kamol daraxtga chiqdi, mushukchani oldi va pastga tushdi.\n\nQo'llari titraydi, lekin mushukcha endi xavfsiz edi. \"Sen jasur bola ekansan,\" dedi qo'shni xotin.\n\nKamol tushundi: jasorat — qo'rqmaslik emas, balki qo'rqib turib baribir harakat qilishdir.",
  "ru":"Камол был очень робким мальчиком. Он боялся темноты, высоты и незнакомых людей.\n\nОднажды в парке маленький котёнок залез на дерево и не мог слезть. Он мяукал и плакал. Все проходили мимо.\n\nКамол остановился. Сердце колотилось. Но он посмотрел в глаза котёнку — тот тоже боялся. Камол полез на дерево, взял котёнка и спустился вниз.\n\nРуки дрожали, но котёнок был в безопасности. \"Ты смелый мальчик,\" — сказала соседка.\n\nКамол понял: храбрость — это не отсутствие страха, а умение действовать несмотря на него.",
  "en":"Kamol was a very timid boy. He was afraid of the dark, of heights, and of strangers.\n\nOne day in the park a little kitten climbed a tree and could not get down. It cried and meowed. Everyone walked past.\n\nKamol stopped. His heart pounded. But he looked into the kitten's eyes — it was afraid too. Kamol climbed the tree, picked up the kitten, and came back down.\n\nHis hands trembled, but the kitten was safe. \"You are a brave boy,\" said the neighbour.\n\nKamol understood: bravery is not the absence of fear, but the choice to act in spite of it."
}
```

- [ ] **Step 10: Add `text` to story-010**

```json
"text":{
  "uz":"Mahallada uch do'st — Sardor, Nilufar va Bobur — yashardilar. Ular kuchli qahramonlar haqida kitob o'qishni yaxshi ko'rishardi.\n\nBir kuni qo'shni buvi sumkasini tushirib yubordi, narsalari to'kildi. Sardor yugurdi va narsalarni yig'ishtirib berdi. Nilufar buviga sumkasini ko'tarishga yordam berdi. Bobur uning uyigacha kuzatib qo'ydi.\n\nBuvi rahmat aytdi va dedi: \"Siz haqiqiy qahramonlarsizlar.\"\n\nUch do'st bir-biriga qarashdi. Ular kostyum kiymasdilar, qanot ham yo'q edi ularda. Lekin bugun ular chin qahramonday his qilishdi. Qahramon bo'lish — odamga yordam berishdir.",
  "ru":"В одном дворе жили три друга — Сардор, Нилуфар и Бобур. Они любили читать книги о сильных героях.\n\nОднажды соседская бабушка уронила сумку, и все вещи рассыпались. Сардор подбежал и стал собирать их. Нилуфар помогла бабушке нести сумку. Бобур проводил её до самого дома.\n\nБабушка поблагодарила и сказала: \"Вы настоящие герои.\"\n\nТрое друзей переглянулись. На них не было костюмов и крыльев. Но сегодня они чувствовали себя настоящими героями. Быть героем — значит помогать людям.",
  "en":"Three friends — Sardor, Nilufar, and Bobur — lived in the same neighbourhood. They loved reading books about strong heroes.\n\nOne day the old woman next door dropped her bag and everything scattered. Sardor ran over and picked things up. Nilufar helped her carry the bag. Bobur walked her all the way home.\n\nThe old woman thanked them and said, \"You are real heroes.\"\n\nThe three friends looked at each other. They wore no costumes and had no wings. But today they felt like true heroes. Being a hero means helping people."
}
```

- [ ] **Step 11: Commit stories 001–010**

```bash
git add app/src/main/assets/www/content.json
git commit -m "feat(content): add story text for stories 001-010"
```

---

## Task 2: Add text to stories 011–020

**Files:**
- Modify: `app/src/main/assets/www/content.json`

- [ ] **Step 1: Add `text` to story-011**

```json
"text":{
  "uz":"Orziqul har kecha yulduzlarga qarab yotardi. Bir kecha deraza tagida kichkina yorug' narsani ko'rdi. U yaqinroq bordi — bu kichkina kosmik kema edi! Ichidan robot chiqdi: \"Yulduzlarga bormoqchimisan?\"\n\nOrziqul yuragini qo'liga olib chiqdi. Ular Oyga to'xtashdi — u kumush kabi yaltirardi. Keyin Saturnni ko'rdilar — halqalari kamalak rangida. Mars esa qizil va sirli edi.\n\nRobot dedi: \"Koinot katta, bilim undan ham katta.\"\n\nOrziqul uyiga qaytganda qo'lida Mars toshchasi bor edi. U o'shandan beri hech qachon darsdan qolmadi.",
  "ru":"Орзикул каждую ночь смотрел на звёзды. Однажды под окном он увидел маленький светящийся предмет. Подошёл ближе — это был крошечный космический корабль. Из него вышел робот: \"Хочешь полететь к звёздам?\"\n\nОрзикул согласился. Они остановились на Луне — она сияла, как серебро. Затем увидели Сатурн с кольцами цвета радуги. Марс был красным и загадочным.\n\nРобот сказал: \"Вселенная велика, но знания ещё больше.\"\n\nКогда Орзикул вернулся домой, в руке у него был камешек с Марса. С тех пор он не пропустил ни одного урока.",
  "en":"Orziqul watched the stars every night. One evening he spotted a small glowing object outside his window. He went closer — it was a tiny spaceship. A robot stepped out: \"Would you like to visit the stars?\"\n\nOrziqul said yes. They stopped at the Moon, shining like silver. Then they saw Saturn with its rainbow rings. Mars was red and mysterious.\n\nThe robot said, \"The universe is vast, but knowledge is even larger.\"\n\nWhen Orziqul returned home he held a small stone from Mars. From that day on he never missed a single lesson."
}
```

- [ ] **Step 2: Add `text` to story-012**

```json
"text":{
  "uz":"Lola har kecha tushida sehrli qudratga ega bo'lishni orzu qilardi. Bir kuni bog'da kapalakni ko'rdi — qanoti singan edi.\n\nLola uni olib, qanotini ehtiyotkorlik bilan bandaj bilan bog'lab qo'ydi. Bir hafta kapalakni parvarish qildi. Qanotlari bitgach, kapalak uchib ketdi.\n\nQo'shni kampir buni ko'rib dedi: \"Senda sehrli qudrat bor, bolam.\" Lola ajablandi. \"Menda hech qanday sehrli narsa yo'q-ku.\"\n\nKampir kulib dedi: \"Mehribonlik — eng katta sehrdir. Sen bugun jonni saqlab qolding.\"\n\nLola tushundi: sehrgar bo'lish uchun tayoqcha shart emas — faqat yaxshi yurak kerak.",
  "ru":"Лола каждую ночь мечтала о волшебной силе. Однажды в саду она увидела бабочку со сломанным крылом.\n\nЛола подняла её и осторожно перевязала крыло. Целую неделю она ухаживала за бабочкой. Когда крыло зажило, бабочка улетела.\n\nСоседская старушка это видела и сказала: \"У тебя есть волшебная сила, дитя.\" Лола удивилась: \"Но у меня нет ничего волшебного.\"\n\nСтарушка улыбнулась: \"Доброта — самое большое волшебство. Ты сегодня спасла жизнь.\"\n\nЛола поняла: чтобы быть волшебником, не нужна палочка — нужно только доброе сердце.",
  "en":"Lola dreamed every night of having magical powers. One day in the garden she found a butterfly with a broken wing.\n\nShe picked it up and carefully bandaged its wing. For a week she took care of the butterfly. When the wing healed, the butterfly flew away.\n\nThe old woman next door had watched and said, \"You have magical powers, child.\" Lola was surprised: \"But I have nothing magical.\"\n\nThe old woman smiled: \"Kindness is the greatest magic of all. Today you saved a life.\"\n\nLola understood — to be magical you do not need a wand, only a kind heart."
}
```

- [ ] **Step 3: Add `text` to story-013**

```json
"text":{
  "uz":"Karim oilasi yangi shaharga ko'chib o'tdi. U hech kimni tanimasdi. Birinchi kuni maktabga bordi — hamma uni begona ko'rardi. U xafa bo'ldi.\n\nUyga qaytib onasiga aytdi. Onasi dedi: \"Do'st orttirishning yo'li — avval o'zing do'st bo'l.\"\n\nErtasiga Karim bir bolaga o'ynashga taklif qildi. Bola rozi bo'ldi. Ko'p o'tmay uchinchi bola ham qo'shildi.\n\nBir haftadan so'ng Karimning to'rtta do'sti bor edi. U tushundi: yolg'izlik vaqtinchalik. Birinchi qadam qo'yishni bilsang — barchasi o'zgaradi.",
  "ru":"Семья Карима переехала в новый город. Он никого не знал. В первый день в школе все смотрели на него как на чужака. Ему было грустно.\n\nДома он рассказал маме. Мама сказала: \"Чтобы найти друга, сначала сам стань другом.\"\n\nНа следующий день Карим предложил одному мальчику поиграть вместе. Тот согласился. Вскоре к ним присоединился третий.\n\nЧерез неделю у Карима было уже четыре друга. Он понял: одиночество временно. Если сделать первый шаг — всё меняется.",
  "en":"Karim's family moved to a new city. He did not know anyone. On his first day at school everyone looked at him like a stranger. He felt sad.\n\nAt home he told his mother. She said, \"To find a friend, first be a friend yourself.\"\n\nThe next day Karim invited one boy to play. The boy said yes. Soon a third boy joined them.\n\nWithin a week Karim had four friends. He understood — loneliness is only temporary. If you take the first step, everything changes."
}
```

- [ ] **Step 4: Add `text` to story-014**

```json
"text":{
  "uz":"Har oqshom Muhammadali buvisi yoniga borib, ertak tinglardi. Buvi eski kitobdan ham, o'z boshidan o'tganlardan ham aytib berardi.\n\nBir kuni Muhammadali dedi: \"Buvi, siz juda ko'p narsalarni bilasiz.\" Buvi tabassum qildi: \"Men ham bir vaqtlar sening yoshingda edim. O'shanda men ham xuddi sening kabi so'rardim.\"\n\nMuhammadali hayron qoldi: \"Demak siz ham kichkina bo'lgansiz!\" Buvi kulib dedi: \"Ha, va menga ham kimdir ertak aytib bergan.\"\n\nMuhammadali tushundi: ertaklar bir avloddan ikkinchi avlodga o'tadi. Bu — oilaning boyligi.",
  "ru":"Каждый вечер Мухаммадали приходил к бабушке слушать сказки. Бабушка рассказывала и из старых книг, и из своей жизни.\n\nОднажды Мухаммадали сказал: \"Бабушка, ты знаешь так много!\" Бабушка улыбнулась: \"Я тоже когда-то была в твоём возрасте. И тогда я задавала такие же вопросы, как ты.\"\n\nМухаммадали удивился: \"Значит, ты тоже была маленькой!\" Бабушка засмеялась: \"Да, и мне тоже кто-то рассказывал сказки.\"\n\nМухаммадали понял: сказки передаются из поколения в поколение. Это богатство семьи.",
  "en":"Every evening Muhammadali went to his grandmother to listen to stories. She told tales from old books and from her own life.\n\nOne day Muhammadali said, \"Grandma, you know so much!\" Grandmother smiled: \"I was your age once too. I asked the same questions you ask.\"\n\nMuhammadali was surprised: \"So you were little too!\" Grandmother laughed: \"Yes, and someone told me stories too.\"\n\nMuhammadali understood — stories pass from one generation to the next. They are a family's greatest treasure."
}
```

- [ ] **Step 5: Add `text` to story-015**

```json
"text":{
  "uz":"Dilnoza yangi uyga ko'chib o'tishni xohlamasdi. U eski uyini, ko'chasini va do'stlarini sog'inardi. Birinchi kecha yangi xonada uxlay olmadi.\n\nErtalab oynadan qaradi — hovlida katta olma daraxti bor edi. U pastga tushdi. Daraxt tagida mushuk o'tirardi. Dilnoza unga sut olib keldi. Mushuk ichdi.\n\nKeyingi kun mushuk yana keldi, biroz keyin qo'shni qiz ham keldi. \"Bu mushuk menikimi yoki senikimi?\" deb so'radi qiz. \"Ikkalangizniki,\" dedi Dilnoza.\n\nShu bilan ularda do'stlik boshlandi. Dilnoza tushundi: yangi uy — yangi boshlanish demak.",
  "ru":"Дилноза не хотела переезжать в новый дом. Она скучала по старому дому, своей улице и друзьям. В первую ночь она не могла уснуть.\n\nУтром выглянула в окно — во дворе росла большая яблоня. Она спустилась вниз. Под деревом сидела кошка. Дилноза принесла ей молока. Кошка выпила.\n\nНа следующий день кошка пришла снова, а вместе с ней — соседская девочка. \"Эта кошка твоя или моя?\" — спросила девочка. \"Обеих,\" — сказала Дилноза.\n\nТак началась их дружба. Дилноза поняла: новый дом — это новое начало.",
  "en":"Dilnoza did not want to move to the new house. She missed her old home, her street, and her friends. The first night she could not sleep.\n\nIn the morning she looked out the window — there was a big apple tree in the yard. She went downstairs. A cat sat under the tree. Dilnoza brought it some milk. The cat drank.\n\nThe next day the cat came again, and this time a girl from next door came too. \"Is this cat yours or mine?\" asked the girl. \"Both of ours,\" said Dilnoza.\n\nAnd so their friendship began. Dilnoza understood — a new home is a new beginning."
}
```

- [ ] **Step 6: Add `text` to story-016**

```json
"text":{
  "uz":"Jamshid va Ulug'bek aka-uka edilar. Ular ko'p narsada bir-biridan farq qilishardi: Jamshid kitob o'qishni sevsa, Ulug'bek futbol o'ynashni yaxshi ko'rardi. Ular tez-tez janjallashishardi.\n\nBir kuni Ulug'bek daryoda suzayotib oyog'i tortishib qoldi. U qo'rqib qichqirdi. Jamshid suzishni bilmasdi, lekin daryo yonida uzun novdani topib Ulug'bekka uzatdi. Ulug'bek uni ushladi va qirg'oqqa chiqdi.\n\nIkkalasi uzoq jim o'tirdi. \"Men bilmasdim sen shuncha dadilligini,\" dedi Ulug'bek. \"Men ham bilmasdim,\" dedi Jamshid.\n\nAka-ukalik — farqlardan katta.",
  "ru":"Джамшид и Улугбек были братьями. Они во многом отличались: Джамшид любил читать, а Улугбек — играть в футбол. Они часто спорили.\n\nОднажды Улугбек купался в реке и у него свело ногу. Он закричал от страха. Джамшид не умел плавать, но нашёл у берега длинную ветку и протянул её Улугбеку. Тот схватился и выбрался на берег.\n\nОни долго сидели молча. \"Я не знал, что ты такой смелый,\" — сказал Улугбек. \"Я тоже,\" — ответил Джамшид.\n\nБратство сильнее любых различий.",
  "en":"Jamshid and Ulugbek were brothers. They were different in many ways — Jamshid loved reading while Ulugbek loved football. They argued often.\n\nOne day Ulugbek was swimming in the river when his leg cramped. He shouted in fear. Jamshid could not swim, but he found a long branch by the bank and held it out to Ulugbek. Ulugbek grabbed it and pulled himself to shore.\n\nThey sat in silence for a long time. \"I didn't know you were so brave,\" said Ulugbek. \"Neither did I,\" said Jamshid.\n\nBrotherhood is stronger than any difference."
}
```

- [ ] **Step 7: Add `text` to story-017**

```json
"text":{
  "uz":"Laylo kichkina teleskop bilan har kecha osmonga qarardi. U eng ko'p Katta Ayiq yulduz turkumini sevardi.\n\nBir kecha yulduzlar unga yorug'lik bilan signal bera boshladilar. Laylo ko'zini yumdi. Ko'zini ochsa — u koinotning o'rtasida edi! Atrofida minglab yulduz porlardi. Yaqinida katta moviy sayyora suzib o'tdi.\n\nLaylo qo'lini uzatdi — sovuq, lekin juda chiroyli edi.\n\nUyg'onganida u yotog'ida edi. Lekin teleskopda yangi yulduz ko'rindi — avval ko'rmagani. Laylo kuldi: \"Bu mening yulduzim.\" U uni o'z ismi bilan atadi — Laylo yulduzi.",
  "ru":"Лайло каждую ночь смотрела в маленький телескоп. Больше всего она любила созвездие Большой Медведицы.\n\nОднажды ночью звёзды начали посылать ей световые сигналы. Лайло закрыла глаза. Когда она их открыла — она была в самом центре космоса! Вокруг сияли тысячи звёзд. Рядом проплыла большая голубая планета.\n\nЛайло протянула руку — холодно, но невероятно красиво.\n\nПроснувшись, она оказалась в своей кровати. Но в телескопе появилась новая звезда — та, которой раньше не было. Лайло улыбнулась: \"Это моя звезда.\" И назвала её своим именем — звезда Лайло.",
  "en":"Layla looked through her small telescope every night. Her favourite was the Big Dipper.\n\nOne night the stars started sending her light signals. Layla closed her eyes. When she opened them she was in the middle of space! Thousands of stars shone around her. A large blue planet drifted past.\n\nShe reached out her hand — cold, but incredibly beautiful.\n\nWhen she woke up she was in her bed. But in the telescope was a new star, one she had never seen before. Layla smiled: \"That is my star.\" She named it after herself — Layla's Star."
}
```

- [ ] **Step 8: Add `text` to story-018**

```json
"text":{
  "uz":"Tog' qishlog'ida Zafar ismli bola yashardi. U yulduzlarga qarab: \"Ular nima uchun yarqiraydi?\" deb doim so'rardi.\n\nBir kecha tushida bir yulduz unga dedi: \"Biz yonamiz, chunki ichimizda issiqlik bor.\" Zafar uyg'ondi va o'yladi: \"Mening ichimda ham issiqlik bormi?\"\n\nU kichkina qog'oz olib, sevimli she'rini yozdi. Keyin singlisiga o'qib berdi. Singlisi ko'zlarini yumib tingladi va dedi: \"Bu juda chiroyli.\"\n\nZafar tushundi — uning ichidagi issiqlik so'zlar orqali chiqardi. U o'shandan beri she'r yozishni to'xtatmadi. Har bir bola — o'z yulduzi.",
  "ru":"В горном селе жил мальчик по имени Зафар. Он всегда смотрел на звёзды и спрашивал: \"Почему они светятся?\"\n\nОднажды во сне звезда ответила ему: \"Мы горим, потому что внутри нас есть тепло.\" Зафар проснулся и задумался: \"Есть ли тепло внутри меня?\"\n\nОн взял маленький листок и написал своё любимое стихотворение. Затем прочитал его сестрёнке. Она закрыла глаза и слушала, а потом сказала: \"Это очень красиво.\"\n\nЗафар понял — его внутреннее тепло выходит через слова. С тех пор он не переставал писать стихи. Каждый ребёнок — своя звезда.",
  "en":"In a mountain village lived a boy named Zafar. He always looked at the stars and asked, \"Why do they shine?\"\n\nOne night in a dream a star answered: \"We burn because we have warmth inside us.\" Zafar woke up and thought: \"Do I have warmth inside me?\"\n\nHe took a small piece of paper and wrote his favourite poem. Then he read it to his little sister. She closed her eyes to listen and said, \"That is so beautiful.\"\n\nZafar understood — the warmth inside him came out through words. From then on he never stopped writing poems. Every child is their own star."
}
```

- [ ] **Step 9: Add `text` to story-019**

```json
"text":{
  "uz":"Maftuna kelajakda kosmosga uchmoqchi edi. U har kuni koinot haqida kitob o'qirdi.\n\nBir kuni tushida u Mars sayyorasiga qo'ndi. Atrofi qizil toshlar bilan qoplangan edi. U temir kiyim kiyib, atrof-muhitni o'rganardi. Birdan yer titray boshladi. Maftuna tezda kemaga qaytdi.\n\nU kuzatuv asboblariga qaradi — Mars ostida suv borligini ko'rdi. \"Bu katta kashfiyot!\" dedi u.\n\nUyg'onganida u darhol daftariga yozdi. Ilm qizig'i — orzulardan ham kuchli bo'ladi. Maftuna endi kosmosni yanada ko'proq sevardi.",
  "ru":"Мафтуна мечтала полететь в космос. Каждый день она читала книги о вселенной.\n\nОднажды во сне она приземлилась на Марсе. Вокруг были красные камни. В скафандре она исследовала окрестности. Вдруг земля задрожала. Мафтуна быстро вернулась на корабль.\n\nОна посмотрела на приборы — под поверхностью Марса было обнаружено воды. \"Это великое открытие!\" — воскликнула она.\n\nПроснувшись, она сразу записала всё в тетрадь. Любопытство к науке сильнее любых мечтаний. Мафтуна теперь любила космос ещё больше.",
  "en":"Maftuna wanted to fly to space one day. She read books about the universe every day.\n\nOne night in a dream she landed on Mars. Red rocks surrounded her. In a space suit she explored the area. Suddenly the ground shook. Maftuna rushed back to her ship.\n\nShe checked the instruments — water had been detected beneath the surface of Mars. \"This is a great discovery!\" she cried.\n\nWhen she woke she immediately wrote everything in her notebook. Scientific curiosity is stronger than any dream. Maftuna loved space more than ever."
}
```

- [ ] **Step 10: Add `text` to story-020**

```json
"text":{
  "uz":"Temur yolg'iz o'g'il edi. Uning do'stlari ko'p emas edi. Bir kuni otasi unga kichkina robot sovg'a qildi.\n\nRobot gapira olardi, o'ynay olardi, va hatto qo'shiq aytardi. Temur xursand bo'ldi. Ular har kuni birga o'ynar, birga gapirishardilar.\n\nBir kuni robot batareyasi tugab qoldi. Temur yig'ladi. Otasi dedi: \"Robot — texnika. U senga do'st bo'lishni o'rgatdi. Endi haqiqiy do'st topish vaqti.\"\n\nTemur maktabga borib, birinchi marta o'zi tanishdi. Do'st orttirishni — robot o'rgatgan edi. Haqiqiy do'stlik — inson bilan.",
  "ru":"Темур был единственным ребёнком в семье. У него было мало друзей. Однажды папа подарил ему маленького робота.\n\nРобот умел говорить, играть и даже петь. Темур был счастлив. Они вместе играли и разговаривали каждый день.\n\nОднажды у робота села батарейка. Темур заплакал. Папа сказал: \"Робот — это техника. Он научил тебя дружить. Теперь время найти настоящего друга.\"\n\nТемур пришёл в школу и впервые сам познакомился с мальчиком. Дружить его научил робот. Но настоящая дружба — с человеком.",
  "en":"Temur was an only child. He did not have many friends. One day his father gave him a little robot.\n\nThe robot could talk, play games, and even sing. Temur was so happy. They played and talked together every day.\n\nOne day the robot's battery ran out. Temur cried. His father said, \"The robot is a machine. It taught you how to be a friend. Now it is time to find a real one.\"\n\nTemur went to school and introduced himself to another boy for the very first time. The robot had taught him friendship. But real friendship is with a person."
}
```

- [ ] **Step 11: Commit stories 011–020**

```bash
git add app/src/main/assets/www/content.json
git commit -m "feat(content): add story text for stories 011-020"
```

---

## Task 3: Build + install + verify

**Files:** none (build artifact)

- [ ] **Step 1: Build debug APK**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Install on device**

```bash
"C:\Users\Komiljon\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r -t app/build/outputs/apk/debug/app-debug.apk
```

Expected: `Success`

- [ ] **Step 3: Launch app**

```bash
"C:\Users\Komiljon\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n "uz.kidzone.app.debug/uz.kidzone.app.MainActivity"
```

- [ ] **Step 4: Smoke test (manual)**

1. Open **Ertaklar** tab
2. Tap **story-001 (🦁 Sher va Sichqon)** → ai-viewer opens with Uzbek text and title
3. Text reads aloud automatically
4. Switch language to RU → tap same card → Russian text appears
5. Switch to EN → English text appears
6. Tap **■** stop and close viewer → no crash
7. Tap a story that has no audio (all 20 for now) → text still shows, no audio error toast
8. Tap **■** close, switch to **O'yinlar** tab → games still load normally (no regression)
