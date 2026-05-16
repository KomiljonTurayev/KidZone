# Kidzo AI Agent — UI Dizayn Spesifikatsiyasi

**Sana:** 2026-05-17
**Loyiha:** KidZone — Bolalar uchun ertaklar, qo'shiqlar va mini-o'yinlar
**Bog'liq spec:** `2026-05-14-kidzo-ai-agent-design.md` (arxitektura, state machine)

---

## 1. Maqsad

Mavjud `KidzoAgent` state machine uchun to'liq UI qatlami yaratish. Arxitektura o'zgarmaydi — faqat `KidzoBottomSheet`, `KidzoCardAdapter`, layout fayllar va FAB qayta yoziladi.

---

## 2. UI Qarorlari (Brainstorming natijalari)

| Komponent | Tanlangan variant | Tavsif |
|-----------|-------------------|--------|
| RECOMMENDATIONS layout | Rangli karusel | Gorizontal scroll, har karta o'z rangida |
| THINKING holati | Header + progress bar | Orange header, horizontal ProgressBar, izoh matn |
| CHATTING holati | Karusel + pastda chat | Mini karusel qoladi, chat section qo'shiladi |
| FAB tugmasi | Pulsli animatsiya | ObjectAnimator bilan scale 1.0→1.18→1.0, INFINITE |

---

## 3. BottomSheet Tuzilmasi (Unified Layout)

Bitta `bottom_sheet_kidzo.xml` layout, seksiyalar state bo'yicha `VISIBLE`/`GONE` almashadi.

```
BottomSheet
├── drag_handle (40dp × 4dp, markazda)
├── header  ← har doim ko'rinadi
│   ├── 🐦 (24sp emoji)
│   ├── "Kidzo" (bold, oq, 15sp)
│   └── ✕ close tugmasi (o'ng)
├── [thinking_section]
│   ├── ProgressBar (horizontal, indeterminate, #FF6B35)
│   └── TextView "Bugun sening uchun eng yaxshi kontent topmoqda..."
├── [carousel_section]
│   └── RecyclerView (gorizontal LinearLayoutManager, nestedScrolling=false)
├── [recommendations_footer]
│   └── MaterialButton "💬 Kidzo bilan gaplash" (#FFF9C4 fon)
├── [chat_section]
│   ├── ScrollView → LinearLayout (chat_messages)
│   └── [input_row]: EditText + "➤" MaterialButton (#FF6B35)
└── [error_section]
    ├── TextView (xato matni, markazda)
    └── [button_row]: "Qayta urinish" + "Yopish"
```

### State — Ko'rinish Jadvali

| Seksiya | THINKING | RECOMMENDATIONS | CHATTING | ERROR |
|---------|:--------:|:---------------:|:--------:|:-----:|
| thinking_section | ✅ | — | — | — |
| carousel_section | — | ✅ | ✅ | — |
| recommendations_footer | — | ✅ | — | — |
| chat_section | — | — | ✅ | — |
| error_section | — | — | — | ✅ |

> **Muhim:** CHATTING holatida karusel "mini" ko'rinadi — chat_section pastdan joy egallaydi, karusel tabiiy ravishda siqiladi. Alohida size animatsiyasi shart emas.

---

## 4. Komponent Detallari

### 4.1 Karta Layouti (`item_kidzo_card.xml`)

Gorizontal RecyclerView uchun. `minWidth: 100dp`, `padding: 12dp`.

```
┌──────────────────┐
│   [rang foni]    │  ← CARD_COLORS tsiklidan
│   emoji  34sp    │
│   Sarlavha 9sp   │  ← bold, oq
│   tur    8sp     │  ← "Ertak" / "Qo'shiq", shaffof oq
│  [▶ Eshit]       │  ← oq mini tugma
└──────────────────┘
```

### 4.2 Rang Palitasi (KidzoCardAdapter)

Karta indeksiga qarab tsikl:

```java
static final int[] CARD_COLORS = {
    0xFFFF6B35,  // to'q sariq-qizil (1-karta)
    0xFF4ECDC4,  // ko'k-yashil
    0xFFA78BFA,  // binafsha
    0xFFFFD93D,  // sariq
    0xFF6BCB77,  // yashil
    0xFF4D96FF   // ko'k
};
// Ishlatish: CARD_COLORS[position % CARD_COLORS.length]
```

### 4.3 Chat Pufaklari (`addChatBubble`)

Dasturiy yaratish (`GradientDrawable` bilan):

| Tomonlama | Fon | Matn | Hizalash |
|-----------|-----|------|----------|
| Kidzo (chap) | Oq, `#FF6B35` chegara 2dp, radius 16dp | `#333333` | `Gravity.START` |
| Foydalanuvchi (o'ng) | `#FF6B35`, radius 16dp | Oq | `Gravity.END` |

Kidzo pufagi boshida `"🐦 "` prefiksi qo'shiladi.

Yangi Kidzo xabari kelganda `ScrollView.fullScroll(View.FOCUS_DOWN)` chaqiriladi.

### 4.4 THINKING Seksiyasi

```
[🐦 Kidzo header — #FF6B35]
────────────────────────────
🔄 ProgressBar (horizontal, indeterminate)
   "Bugun sening uchun eng yaxshi
    kontent topmoqda..." (12sp, #888)
```

### 4.5 Header

```xml
<!-- Har doim ko'rinadi -->
<LinearLayout background="#FF6B35" padding="12dp">
  <TextView text="🐦" 24sp />
  <TextView text="Kidzo" bold white 15sp weight=1 />
  <ImageButton src="ic_close" tint="white" />
</LinearLayout>
```

Close tugmasi bosilganda: `agent.dismiss()` → `IDLE` → `dismiss()`.

---

## 5. FAB Dizayni

`activity_main.xml` da `FloatingActionButton` (agar yo'q bo'lsa qo'shiladi):

```xml
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/fab_kidzo"
    android:layout_gravity="bottom|end"
    android:layout_margin="16dp"
    app:backgroundTint="#FF6B35"
    app:srcCompat="@drawable/ic_kidzo_bird"
    app:tint="@android:color/white" />
```

**Pulse animatsiyasi** (`MainActivity.onCreate`):

```java
ObjectAnimator scaleX = ObjectAnimator.ofFloat(fabKidzo, "scaleX", 1f, 1.18f);
ObjectAnimator scaleY = ObjectAnimator.ofFloat(fabKidzo, "scaleY", 1f, 1.18f);
// duration: 900ms, repeatCount: INFINITE, repeatMode: REVERSE
AnimatorSet pulse = new AnimatorSet();
pulse.playTogether(scaleX, scaleY);
pulse.start();
```

FAB bosilganda: `KidzoBottomSheet` yaratib `show()` qilinadi.

---

## 6. O'zgartiriluvchi Fayllar

| Fayl | Harakat |
|------|---------|
| `res/layout/bottom_sheet_kidzo.xml` | To'liq qayta yozish — unified layout |
| `res/layout/item_kidzo_card.xml` | Yangi — rangli karta layout |
| `res/drawable/bg_chat_kidzo.xml` | Yangi — Kidzo pufagi `GradientDrawable` |
| `res/drawable/bg_chat_user.xml` | Yangi — foydalanuvchi pufagi `GradientDrawable` |
| `res/drawable/ic_kidzo_bird.xml` | Yangi — FAB uchun qush ikonkasi (vector) |
| `kidzo/KidzoCardAdapter.java` | To'liq qayta yozish — horizontal karusel, rang |
| `kidzo/KidzoBottomSheet.java` | Yangilash — unified seksiyalar, chat bubble stil |
| `res/layout/activity_main.xml` | FAB qo'shish |
| `MainActivity.java` | FAB pulse animatsiyasi, BottomSheet ochish |

> **Eslatma:** FAB `layout_gravity` ishlashi uchun `activity_main.xml` root view `CoordinatorLayout` bo'lishi kerak. Agar hozir `ConstraintLayout` bo'lsa, implementation planda almashtirish ko'rsatiladi.

**O'zgarmaydi:**
- `KidzoAgent.java`, `KidzoState.java`, `KidzoStateListener.java`
- `ActionParser.java`, `ContentFilter.java`, `ContentItem.java`
- `RealGeminiCaller.java`, `GeminiCaller.java`, `MainThreadRunner.java`
- `content.json`, barcha HTML5 o'yinlar, `BirdAiManager.java`

---

## 7. Cheklovlar

- `BirdAiManager.java` bu spec doirasida o'zgarmaydi — u alohida dialog
- Animatsiya faqat FAB pulse; BottomSheet state o'tishlari `setVisibility()` bilan (no transitions)
- Chat tarixi xotirada saqlanadi (session ichida); BottomSheet yopilganda tozalanadi
- Gemini API kaliti `BuildConfig.GEMINI_API_KEY` orqali (mavjud yondashuv)
