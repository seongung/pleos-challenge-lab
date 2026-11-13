package com.pleosjournal

import com.pleosjournal.domain.ClueExtractor
import com.pleosjournal.domain.ClueKind.HEADLAMP
import com.pleosjournal.domain.ClueKind.REAR_RECLINE
import com.pleosjournal.domain.ClueKind.WINDOW_OPEN
import com.pleosjournal.domain.ClueKind.WIPER
import com.pleosjournal.domain.ObservedSpan
import com.pleosjournal.domain.isInputAllowed
import com.pleosjournal.domain.suggestScenes
import com.pleosjournal.signal.replay.parseScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** UC-12 장면 후보, UC-09 게이트 판정. */
class SceneSuggesterTest {
    @Test fun `제천 시나리오는 장면 후보 4개, 같은 종류의 겹친 구간은 하나로 묶인다`() {
        val spans = ClueExtractor()
            .apply { parseScenario(File("../scenarios/jecheon_daytrip.csv").readText()).forEach(::onEvent) }
            .finish(9_310_000)
        val scenes = suggestScenes(spans)
        assertEquals(listOf(WINDOW_OPEN, WIPER, REAR_RECLINE, HEADLAMP), scenes.map { it.kind })
        assertEquals(listOf(2, 1, 2, 1), scenes.map { it.evidence.size }) // 근거가 되는 관측 구간 수
        assertEquals(900_000L to 1_500_000L, scenes[0].startMs to scenes[0].endMs)
        assertEquals("이때 비가 왔나요?", scenes[1].question)
    }

    @Test fun `떨어져 있는 같은 종류 구간은 별개의 장면`() {
        val scenes = suggestScenes(listOf(ObservedSpan(WIPER, "FRONT", 0, 100), ObservedSpan(WIPER, "FRONT", 500, 900)))
        assertEquals(2, scenes.size)
    }

    @Test fun `부분적으로 겹치면 합쳐서 전체 범위가 된다`() {
        val scenes = suggestScenes(listOf(ObservedSpan(WINDOW_OPEN, "FRONT_LEFT", 0, 300), ObservedSpan(WINDOW_OPEN, "REAR_LEFT", 200, 800)))
        assertEquals(listOf(0L to 800L), scenes.map { it.startMs to it.endMs })
    }

    @Test fun `주행 제한 상태를 모르면 입력을 닫는다`() {
        assertFalse(isInputAllowed(requiresDistractionOptimization = null)) // 상태 불명 = 닫힘 (fail-safe)
        assertFalse(isInputAllowed(requiresDistractionOptimization = true))
        assertTrue(isInputAllowed(requiresDistractionOptimization = false))
    }
}
