package com.pleosjournal.domain

import com.pleosjournal.data.JournalDao
import com.pleosjournal.data.SpanEntity
import com.pleosjournal.data.TripEntity
import com.pleosjournal.signal.VehicleSignalSource
import kotlinx.coroutines.flow.MutableStateFlow

/** 신호 소스 하나를 끝까지 읽어 트립(UC-01)과 관측 구간(UC-03)을 저장한다. 소스 종류를 모른다. */
class Recorder(private val dao: JournalDao) {
    /** 화면 상단에 보여줄 현재 상태 한 줄. */
    val status = MutableStateFlow("대기 중")

    suspend fun record(src: VehicleSignalSource) {
        val detector = TripDetector()
        val clues = ClueExtractor()
        var count = 0
        src.events().collect { e ->
            clues.onEvent(e)
            val trip = detector.onEvent(e)
            count++
            status.value = "이벤트 ${count}개, 트립 상태 ${detector.state}, 최근 ${e.signal} ${e.target} ${e.value.take(12)}"
            if (trip != null) {
                // ponytail: 트립 사이(IDLE)에 생긴 구간은 잘라서 버린다. 주차 중 단서가 필요해지면 따로 저장한다.
                val spans = clues.finish(trip.endMs)
                    .filter { it.startMs < trip.endMs && it.endMs > trip.startMs }
                    .map {
                        SpanEntity(
                            tripId = 0, kind = it.kind.name, target = it.target,
                            startMs = maxOf(it.startMs, trip.startMs), endMs = minOf(it.endMs, trip.endMs),
                            source = src.source.name,
                        )
                    }
                dao.save(TripEntity(startMs = trip.startMs, endMs = trip.endMs, distanceKm = trip.distanceKm, source = src.source.name), spans)
            }
        }
        status.value = "재생 완료 (이벤트 ${count}개)"
    }
}
