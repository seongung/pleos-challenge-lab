package com.pleosjournal

import com.pleosjournal.domain.ClueExtractor
import com.pleosjournal.domain.ClueKind
import com.pleosjournal.domain.ObservedSpan
import com.pleosjournal.domain.Trip
import com.pleosjournal.domain.TripDetector
import com.pleosjournal.domain.isMergeCandidate
import com.pleosjournal.domain.merge
import com.pleosjournal.signal.replay.parseScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** UC-10 트립 보정, UC-18 항목별 수집 끄기. */
class TripCorrectionTest {
    private val errand = File("../scenarios/errand_short_stop.csv").readText()

    @Test fun `편의점 경유 시나리오는 트립 2개로 오판되고 병합 후보가 되며 병합하면 하나`() {
        val detector = TripDetector()
        val trips = parseScenario(errand).mapNotNull(detector::onEvent)
        assertEquals(listOf(Trip(0, 665_000, 6.5), Trip(1_025_000, 1_865_000, 7.5)), trips)
        assertTrue(isMergeCandidate(prevEndMs = trips[0].endMs, nextStartMs = trips[1].startMs)) // 간격 6분
        assertEquals(Trip(0, 1_865_000, 14.0), merge(trips[0], trips[1]))
    }

    @Test fun `간격이 길면 병합 후보가 아니다`() {
        assertFalse(isMergeCandidate(prevEndMs = 0, nextStartMs = 2 * 3_600_000L))
    }

    @Test fun `꺼 둔 항목은 수집하지 않는다`() {
        val clues = ClueExtractor(enabled = { it != ClueKind.WIPER })
        parseScenario("0,WIPER_ACTIVE,FRONT,true\n0,HEADLAMP_LOW,,true").forEach(clues::onEvent)
        assertEquals(listOf(ObservedSpan(ClueKind.HEADLAMP, "", 0, 3_600_000)), clues.finish(3_600_000))
    }
}
