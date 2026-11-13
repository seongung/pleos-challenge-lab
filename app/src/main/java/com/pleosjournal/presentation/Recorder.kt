package com.pleosjournal.presentation

import com.pleosjournal.data.JournalDao
import com.pleosjournal.data.Privacy
import com.pleosjournal.data.SceneEntity
import com.pleosjournal.data.SpanEntity
import com.pleosjournal.data.TripEntity
import com.pleosjournal.domain.ClueExtractor
import com.pleosjournal.domain.TripDetector
import com.pleosjournal.domain.suggestScenes
import com.pleosjournal.signal.VehicleSignalSource
import kotlinx.coroutines.flow.MutableStateFlow

/** 신호 소스 하나를 끝까지 읽어 트립(UC-01)과 관측 구간(UC-03)을 저장한다. 소스 종류를 모른다. */
class Recorder(
    private val dao: JournalDao,
    /** UC-18: 이벤트마다 현재 프라이버시 설정을 읽는다. 재생 도중 바꿔도 바로 반영된다. */
    private val privacy: () -> Privacy = { Privacy() },
) {
    /** 화면 상단에 보여줄 현재 상태 한 줄. */
    val status = MutableStateFlow("대기 중")

    suspend fun record(src: VehicleSignalSource) {
        val detector = TripDetector()
        val clues = ClueExtractor(enabled = { it.name !in privacy().disabledKinds })
        var count = 0
        src.events().collect { e ->
            if (privacy().paused) {
                // 일시정지 중에는 트립 판정에도 넣지 않는다. 그 사이의 이동은 기록에 남지 않는다.
                status.value = "기록 일시정지 중 (이벤트를 저장하지 않음)"
                return@collect
            }
            clues.onEvent(e)
            val trip = detector.onEvent(e)
            count++
            status.value = "이벤트 ${count}개, 트립 상태 ${detector.state}, 최근 ${e.signal} ${e.target} ${e.value.take(12)}"
            if (trip != null) {
                // ponytail: 트립 사이(IDLE)에 생긴 구간은 잘라서 버린다. 주차 중 단서가 필요해지면 따로 저장한다.
                val observed = clues.finish(trip.endMs)
                    .filter { it.startMs < trip.endMs && it.endMs > trip.startMs }
                    .map { it.copy(startMs = maxOf(it.startMs, trip.startMs), endMs = minOf(it.endMs, trip.endMs)) }
                val src = src.source.name
                dao.save(
                    TripEntity(startMs = trip.startMs, endMs = trip.endMs, distanceKm = trip.distanceKm, source = src),
                    observed.map { SpanEntity(tripId = 0, kind = it.kind.name, target = it.target, startMs = it.startMs, endMs = it.endMs, source = src) },
                    // UC-12: 트립이 끝나는 시점에 장면 후보를 만들어 둔다.
                    suggestScenes(observed).map { SceneEntity(tripId = 0, kind = it.kind.name, startMs = it.startMs, endMs = it.endMs) },
                )
            }
        }
        status.value = "재생 완료 (이벤트 ${count}개)"
    }
}
