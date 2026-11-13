package com.pleosjournal

import com.pleosjournal.domain.ClueExtractor
import com.pleosjournal.domain.ClueKind.DOOR_OPEN
import com.pleosjournal.domain.ClueKind.HEADLAMP
import com.pleosjournal.domain.ClueKind.REAR_RECLINE
import com.pleosjournal.domain.ClueKind.WINDOW_OPEN
import com.pleosjournal.domain.ClueKind.WIPER
import com.pleosjournal.domain.ObservedSpan
import com.pleosjournal.signal.replay.parseScenario
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** UC-03. 관측 사실만 구간으로 남기는지 확인한다. */
class ClueExtractorTest {
    private fun spans(csv: String, endMs: Long): List<ObservedSpan> =
        ClueExtractor().apply { parseScenario(csv).forEach(::onEvent) }.finish(endMs)

    @Test fun `제천 시나리오의 관측 구간`() {
        val expected = listOf(
            ObservedSpan(WINDOW_OPEN, "FRONT_LEFT", 900_000, 1_500_000),
            ObservedSpan(WINDOW_OPEN, "FRONT_RIGHT", 900_000, 1_500_000),
            ObservedSpan(WIPER, "FRONT", 2_400_000, 4_200_000),
            ObservedSpan(REAR_RECLINE, "REAR_LEFT", 4_500_000, 8_700_000),
            ObservedSpan(REAR_RECLINE, "REAR_RIGHT", 4_500_000, 8_700_000),
            // 끝까지 꺼지지 않은 전조등은 트립 종료 시각에 닫는다.
            ObservedSpan(HEADLAMP, "", 5_400_000, 9_310_000),
            // 운전석 도어 10초 열림은 최소 지속 시간 미만이라 빠진다.
        )
        assertEquals(expected, spans(File("../scenarios/jecheon_daytrip.csv").readText(), endMs = 9_310_000))
    }

    @Test fun `짧은 와셔액 분사는 구간이 되지 않는다`() {
        assertEquals(emptyList<ObservedSpan>(), spans("0,WIPER_ACTIVE,FRONT,true\n5000,WIPER_ACTIVE,FRONT,false", 600_000))
    }

    @Test fun `창문을 더 여는 것은 같은 구간`() {
        val csv = "0,WINDOW_POSITION_PCT,FRONT_LEFT,30\n60000,WINDOW_POSITION_PCT,FRONT_LEFT,80\n300000,WINDOW_POSITION_PCT,FRONT_LEFT,0"
        assertEquals(listOf(ObservedSpan(WINDOW_OPEN, "FRONT_LEFT", 0, 300_000)), spans(csv, 600_000))
    }

    @Test fun `앞좌석 등받이는 대상이 아니다`() {
        assertEquals(emptyList<ObservedSpan>(), spans("0,SEAT_RECLINE_DEG,DRIVER,40", 3_600_000))
    }

    @Test fun `finish 후에는 상태가 비워진다`() {
        val e = ClueExtractor()
        parseScenario("0,DOOR_OPEN,DRIVER,true").forEach(e::onEvent)
        assertEquals(listOf(ObservedSpan(DOOR_OPEN, "DRIVER", 0, 120_000)), e.finish(120_000))
        assertEquals(emptyList<ObservedSpan>(), e.finish(999_000))
    }
}
