package com.pleosjournal.presentation

import com.pleosjournal.data.JournalDao
import com.pleosjournal.data.Privacy
import com.pleosjournal.data.SceneEntity
import com.pleosjournal.data.SpanEntity
import com.pleosjournal.data.StopEntity
import com.pleosjournal.data.TripEntity
import com.pleosjournal.domain.ClueExtractor
import com.pleosjournal.domain.Trip
import com.pleosjournal.domain.TripDetector
import com.pleosjournal.domain.suggestScenes
import com.pleosjournal.signal.VehicleSignalSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** 신호 소스 하나를 읽어 트립(UC-01), 관측 구간(UC-03), 장면 후보(UC-12)를 저장한다. 소스 종류를 모른다. */
class Recorder(
    private val dao: JournalDao,
    /** UC-18: 이벤트마다 현재 프라이버시 설정을 읽는다. 기록 도중 바꿔도 바로 반영된다. */
    private val privacy: () -> Privacy = { Privacy(onboarded = true) },
) {
    /** 화면 상단에 보여줄 현재 상태 한 줄. */
    val status = MutableStateFlow("대기 중")

    /**
     * [src]가 끝날 때까지(실제 신호는 취소될 때까지) 기록한다.
     * [tickMs]를 주면 그 주기로 시간 경과를 알려, 이벤트가 없어도 트립 종료를 판정한다. 실제 신호에 쓴다.
     * 재생 소스는 이벤트 시각이 가상이라 tick을 쓰지 않는다.
     */
    suspend fun record(src: VehicleSignalSource, tickMs: Long? = null) = coroutineScope {
        val detector = TripDetector()
        val clues = ClueExtractor(enabled = { it.name !in privacy().disabledKinds })
        val source = src.source.name
        var count = 0

        suspend fun save(trip: Trip) {
            // ponytail: 트립 사이(IDLE)에 생긴 구간은 잘라서 버린다. 주차 중 단서가 필요해지면 따로 저장한다.
            val observed = clues.finish(trip.endMs)
                .filter { it.startMs < trip.endMs && it.endMs > trip.startMs }
                .map { it.copy(startMs = maxOf(it.startMs, trip.startMs), endMs = minOf(it.endMs, trip.endMs)) }
            dao.save(
                TripEntity(startMs = trip.startMs, endMs = trip.endMs, distanceKm = trip.distanceKm, source = source),
                observed.map { SpanEntity(tripId = 0, kind = it.kind.name, target = it.target, startMs = it.startMs, endMs = it.endMs, source = source) },
                // UC-12: 트립이 끝나는 시점에 장면 후보를 만들어 둔다.
                suggestScenes(observed).map { SceneEntity(tripId = 0, kind = it.kind.name, startMs = it.startMs, endMs = it.endMs) },
                trip.stops.map { StopEntity(tripId = 0, startMs = it.startMs, endMs = it.endMs, kmAtStop = it.kmAtStop) },
            )
        }

        // 같은 스레드(Main)에서 번갈아 실행되므로 detector를 두 코루틴이 함께 써도 안전하다.
        val ticker = tickMs?.let {
            launch {
                while (true) {
                    delay(it)
                    detector.tick(System.currentTimeMillis())?.let { trip -> save(trip) }
                }
            }
        }
        status.value = "$source 기록 시작"
        try {
            src.events().collect { e ->
                val p = privacy()
                if (!p.onboarded || p.paused) {
                    // 동의 전이거나 일시정지 중에는 트립 판정에도 넣지 않는다. 그 사이의 이동은 기록에 남지 않는다.
                    status.value = if (p.onboarded) "기록 일시정지 중 (이벤트를 저장하지 않음)" else "최초 동의 전 (이벤트를 저장하지 않음)"
                    return@collect
                }
                clues.onEvent(e)
                val trip = detector.onEvent(e)
                count++
                status.value = "$source 이벤트 ${count}개, 트립 상태 ${detector.state}, 최근 ${e.signal} ${e.target} ${e.value.take(12)}"
                if (trip != null) save(trip)
            }
            status.value = "재생 완료 (이벤트 ${count}개)"
        } finally {
            ticker?.cancel()
        }
    }
}
