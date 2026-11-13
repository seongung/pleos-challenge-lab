package com.pleosjournal.signal.replay

import com.pleosjournal.signal.Signal
import com.pleosjournal.signal.Source
import com.pleosjournal.signal.VehicleEvent
import com.pleosjournal.signal.VehicleSignalSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 시나리오 CSV(`t_ms,signal,target,value`)를 이벤트 목록으로 바꾼다. 형식은 docs/05_prototype_plan.md.
 * 헤더와 `#` 주석 행은 건너뛴다. 이벤트 시각은 [baseMs] + t_ms.
 */
fun parseScenario(csv: String, baseMs: Long = 0): List<VehicleEvent> =
    csv.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("t_ms") }
        .map { line ->
            // limit = 4: value(예: SPEECH 문장)에 쉼표가 들어가도 잘리지 않는다.
            val (t, signal, target, value) = line.split(",", limit = 4)
            VehicleEvent(baseMs + t.toLong(), Signal.valueOf(signal), target, value, Source.REPLAY)
        }
        .sortedBy { it.tMs } // sortedBy는 안정 정렬이라 같은 시각의 행 순서는 유지된다.
        .toList()

/** CSV 시나리오를 [speed]배속으로 흘려보낸다. 예: 600배속이면 2시간 40분이 16초. */
class ReplaySource(
    private val csv: String,
    private val speed: Int,
    private val baseMs: Long = System.currentTimeMillis(),
) : VehicleSignalSource {
    override val source = Source.REPLAY

    // flow { }: collect 될 때마다 처음부터 실행되는 cold stream. emit()으로 값을 내보낸다.
    override fun events(): Flow<VehicleEvent> = flow {
        var prev = baseMs
        for (e in parseScenario(csv, baseMs)) {
            delay((e.tMs - prev) / speed)
            prev = e.tMs
            emit(e)
        }
    }
}
