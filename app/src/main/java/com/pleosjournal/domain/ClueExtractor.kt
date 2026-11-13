package com.pleosjournal.domain

import com.pleosjournal.signal.Signal
import com.pleosjournal.signal.VehicleEvent

/**
 * 관측 단서 종류와 최소 지속 시간. 이보다 짧은 구간은 버린다(와셔액 분사, 톨게이트 창문 등).
 * 값은 가정이며 시나리오 재생으로 튜닝한다(docs/04_usecases.md 단서 규칙표).
 */
enum class ClueKind(val minMs: Long) {
    WIPER(60_000),
    HEADLAMP(5 * 60_000),
    WINDOW_OPEN(2 * 60_000),
    REAR_RECLINE(10 * 60_000),
    DOOR_OPEN(60_000),
}

/** 관측 사실 한 구간. 해석("비가 왔다")은 담지 않는다. */
data class ObservedSpan(val kind: ClueKind, val target: String, val startMs: Long, val endMs: Long)

/** UC-03. 이벤트를 "무엇이 언제부터 언제까지 켜져 있었다"는 구간으로만 바꾼다. */
class ClueExtractor {
    private val open = LinkedHashMap<Pair<ClueKind, String>, Long>() // 아직 안 끝난 구간의 시작 시각
    private val closed = mutableListOf<ObservedSpan>()

    fun onEvent(e: VehicleEvent) {
        val (kind, on) = when (e.signal) {
            Signal.WIPER_ACTIVE -> ClueKind.WIPER to e.bool
            Signal.HEADLAMP_LOW -> ClueKind.HEADLAMP to e.bool
            Signal.DOOR_OPEN -> ClueKind.DOOR_OPEN to e.bool
            Signal.WINDOW_POSITION_PCT -> ClueKind.WINDOW_OPEN to (e.number > 0)
            Signal.SEAT_RECLINE_DEG ->
                if (e.target.startsWith("REAR")) ClueKind.REAR_RECLINE to (e.number >= RECLINE_DEG) else return
            else -> return
        }
        val key = kind to e.target
        if (on) {
            open.putIfAbsent(key, e.tMs) // 이미 열려 있으면 시작 시각 유지
        } else {
            open.remove(key)?.let { closed += ObservedSpan(kind, e.target, it, e.tMs) }
        }
    }

    /** 트립 종료 시 호출. 아직 켜져 있는 구간을 [endMs]에 닫고, 충분히 지속된 구간만 시작 시각순으로 돌려준다. */
    fun finish(endMs: Long): List<ObservedSpan> {
        open.forEach { (key, start) -> closed += ObservedSpan(key.first, key.second, start, endMs) }
        open.clear()
        return closed.filter { it.endMs - it.startMs >= it.kind.minMs }.sortedBy { it.startMs }
            .also { closed.clear() }
    }

    private companion object {
        /** 이 각도 이상이면 "젖힘"으로 기록. 가정값이며 실제 SDK 각도 기준 확인 후 조정. */
        const val RECLINE_DEG = 30.0
    }
}
