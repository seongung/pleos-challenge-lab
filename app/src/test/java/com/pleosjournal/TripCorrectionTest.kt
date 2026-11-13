package com.pleosjournal

import com.pleosjournal.domain.ClueExtractor
import com.pleosjournal.domain.ClueKind
import com.pleosjournal.domain.ObservedSpan
import com.pleosjournal.domain.Stop
import com.pleosjournal.domain.Trip
import com.pleosjournal.domain.TripDetector
import com.pleosjournal.domain.isMergeCandidate
import com.pleosjournal.domain.merge
import com.pleosjournal.domain.split
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
        // 병합하면 두 트립 사이의 공백이 정차로 남는다.
        val gap = Stop(startMs = 665_000, endMs = 1_025_000, kmAtStop = 6.5)
        val merged = merge(trips[0], trips[1])
        assertEquals(Trip(0, 1_865_000, 14.0, listOf(gap)), merged)
        // 그 정차에서 나누면 원래 두 트립으로 돌아간다(병합 되돌리기).
        assertEquals(trips[0] to trips[1], split(merged, gap))
    }

    @Test fun `트립 안에서 오래 서 있으면 정차로 기록되고 그 지점에서 나눌 수 있다`() {
        val min = 60_000L
        val detector = TripDetector()
        val trip = parseScenario(
            """
            0,SEAT_OCCUPIED,DRIVER,true
            0,TRIP_DISTANCE_KM,,0.0
            ${2 * min},TRIP_DISTANCE_KM,,2.0
            ${12 * min},TRIP_DISTANCE_KM,,2.2
            ${14 * min},TRIP_DISTANCE_KM,,5.0
            ${15 * min},DOOR_OPEN,DRIVER,true
            ${15 * min},SEAT_OCCUPIED,DRIVER,false
            ${30 * min},CABIN_TEMP_C,,20
            """.trimIndent(),
        ).mapNotNull(detector::onEvent).single()
        val stop = Stop(2 * min, 12 * min, 2.0)
        assertEquals(Trip(0, 15 * min, 5.0, listOf(stop)), trip)
        assertEquals(Trip(0, 2 * min, 2.0) to Trip(12 * min, 15 * min, 3.0), split(trip, stop))
    }

    @Test fun `신호가 드문드문 온 것은 정차가 아니다`() {
        val detector = TripDetector()
        val trips = parseScenario(File("../scenarios/jecheon_daytrip.csv").readText()).mapNotNull(detector::onEvent)
        assertEquals(emptyList<Stop>(), trips.single().stops)
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
