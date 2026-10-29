package uz.kidzone.app.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiBackendAnswerTest {

    @Test
    fun `a story from the backend is ready`() {
        val answer = aiBackendAnswer(200, """{"title":"Kosmos","text":"Bir bor ekan..."}""", ::parseStory)

        assertEquals(AiBackendAnswer.Ready(GeneratedStory("Kosmos", "Bir bor ekan...")), answer)
    }

    @Test
    fun `402 means the free daily allowance is used up`() {
        assertEquals(AiBackendAnswer.DailyLimitReached, aiBackendAnswer(402, """{"status":402}""", ::parseStory))
        assertEquals(AiBackendAnswer.DailyLimitReached, aiBackendAnswer(402, null, ::parseAnswer))
    }

    @Test
    fun `no model yet, other errors and odd replies fall back to the offline engine`() {
        // 503 until an AI key is configured; 429 is Premium's abuse cap.
        listOf(503, 429, 500, 401, 404).forEach { code ->
            assertEquals("$code", AiBackendAnswer.Unavailable, aiBackendAnswer(code, "{}", ::parseStory))
        }
        assertEquals(AiBackendAnswer.Unavailable, aiBackendAnswer(200, """{"title":"","text":"x"}""", ::parseStory))
        assertEquals(AiBackendAnswer.Unavailable, aiBackendAnswer(200, "not json", ::parseAnswer))
        assertEquals(AiBackendAnswer.Unavailable, aiBackendAnswer(200, "", ::parseAnswer))
    }

    @Test
    fun `an answer is trimmed and a blank one falls back`() {
        assertEquals(AiBackendAnswer.Ready("Quyosh nuri"), aiBackendAnswer(200, """{"answer":" Quyosh nuri "}""", ::parseAnswer))
        assertEquals(AiBackendAnswer.Unavailable, aiBackendAnswer(200, """{"answer":"  "}""", ::parseAnswer))
    }
}
