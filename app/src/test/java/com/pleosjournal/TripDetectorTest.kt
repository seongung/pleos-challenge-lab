package com.pleosjournal

import com.pleosjournal.domain.Trip
import com.pleosjournal.domain.TripDetector
import com.pleosjournal.domain.TripDetector.State
import com.pleosjournal.signal.replay.parseScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/** UC-01. 시나리오 CSV 문자열로 재현하는 테스트. 임계값: 일시정지 5분, 종료 확정 3분. */
class TripDetectorTest {
    private val min = 60_000L

    private fun run(csv: String, detector: TripDetector = TripDetector()): List<Trip> =
        parseScenario(csv).mapNotNull(detector::onEvent)

    private val start = """
        0,SEAT_OCCUPIED,DRIVER,true
        0,TRIP_DISTANCE_KM,,0.0
        ${1 * min},TRIP_DISTANCE_KM,,1.0
    """.trimIndent()

    @Test fun `제천 시나리오는 트립 1개`() {
        // Gradle은 단위 테스트를 모듈 폴더(app/)에서 실행한다.
        val trips = run(File("../scenarios/jecheon_daytrip.csv").readText())
        assertEquals(listOf(Trip(startMs = 0, endMs = 9_310_000, distanceKm = 148.0)), trips)
    }

    @Test fun `신호 대기로 오래 멈췄다 다시 가면 같은 트립`() {
        val d = TripDetector()
        val trips = run("$start\n${8 * min},CABIN_TEMP_C,,23\n${9 * min},TRIP_DISTANCE_KM,,1.5", d)
        assertEquals(emptyList<Trip>(), trips)
        assertEquals(State.ACTIVE, d.state)
    }

    @Test fun `거리 변화가 없으면 PAUSED`() {
        val d = TripDetector()
        run("$start\n${8 * min},CABIN_TEMP_C,,23", d)
        assertEquals(State.PAUSED, d.state)
    }

    @Test fun `동승자만 내리면 종료하지 않는다`() {
        val d = TripDetector()
        val trips = run(
            """
            $start
            ${2 * min},DOOR_OPEN,FRONT_PASSENGER,true
            ${2 * min},SEAT_OCCUPIED,FRONT_PASSENGER,false
            ${20 * min},TRIP_DISTANCE_KM,,2.0
            """.trimIndent(), d,
        )
        assertEquals(emptyList<Trip>(), trips)
        assertEquals(State.ACTIVE, d.state)
    }

    @Test fun `편의점 짧은 경유는 같은 트립`() {
        val trips = run(
            """
            $start
            ${2 * min},DOOR_OPEN,DRIVER,true
            ${2 * min},SEAT_OCCUPIED,DRIVER,false
            ${4 * min},SEAT_OCCUPIED,DRIVER,true
            ${5 * min},TRIP_DISTANCE_KM,,2.0
            """.trimIndent(),
        )
        assertEquals(emptyList<Trip>(), trips)
    }

    @Test fun `운전자가 내리고 오래 지나면 종료, 다시 타면 새 트립`() {
        val d = TripDetector()
        val trips = run(
            """
            $start
            ${2 * min},DOOR_OPEN,DRIVER,true
            ${2 * min},SEAT_OCCUPIED,DRIVER,false
            ${30 * min},SEAT_OCCUPIED,DRIVER,true
            """.trimIndent(), d,
        )
        assertEquals(listOf(Trip(0, 2 * min, 1.0)), trips)
        assertEquals(State.CANDIDATE, d.state)
    }

    @Test fun `이벤트가 없어도 tick으로 종료된다`() {
        val d = TripDetector()
        run("$start\n${2 * min},DOOR_OPEN,DRIVER,true\n${2 * min},SEAT_OCCUPIED,DRIVER,false", d)
        assertNull(d.tick(4 * min))
        assertEquals(Trip(0, 2 * min, 1.0), d.tick(10 * min))
    }

    @Test fun `타기만 하고 안 움직이면 트립 없음`() {
        val d = TripDetector()
        val trips = run("0,SEAT_OCCUPIED,DRIVER,true\n${1 * min},DOOR_OPEN,DRIVER,true\n${1 * min},SEAT_OCCUPIED,DRIVER,false\n${60 * min},CABIN_TEMP_C,,20", d)
        assertEquals(emptyList<Trip>(), trips)
        assertEquals(State.IDLE, d.state)
    }
}
