package com.pleosjournal.domain

import com.pleosjournal.signal.Signal
import com.pleosjournal.signal.VehicleEvent

data class Trip(val startMs: Long, val endMs: Long, val distanceKm: Double)

/** 이 간격 안에 다음 트립이 시작됐으면 "병합 후보"로 표시한다(UC-10). 자동 병합은 하지 않는다. 튜닝 대상. */
const val MERGE_GAP_MS = 30 * 60_000L

fun isMergeCandidate(prevEndMs: Long, nextStartMs: Long) = nextStartMs - prevEndMs in 0..MERGE_GAP_MS

/** 두 트립을 하나로 합친 결과(UC-10). 사이의 정차 시간은 트립에 포함된다. */
fun merge(a: Trip, b: Trip) = Trip(minOf(a.startMs, b.startMs), maxOf(a.endMs, b.endMs), a.distanceKm + b.distanceKm)

/**
 * UC-01 트립 자동 초안. 시동, 기어, 속도 신호 없이 운전석 착석, 운전석 도어, 트립 거리만으로 추정한다.
 *
 * IDLE → CANDIDATE(운전석 착석) → ACTIVE(거리 증가) ⇄ PAUSED(거리 변화 없음) → 종료(Trip 반환 후 IDLE)
 *
 * 주의: 결과는 기록용 추정이다. 안전 판단이나 UX 제한 해제 근거로 쓰지 않는다(CLAUDE.md 제약 6).
 * 오판은 사용자가 UC-10(병합, 분할, 삭제)으로 고친다.
 */
class TripDetector(
    /** 이 시간 동안 거리가 늘지 않으면 PAUSED. 시나리오 재생으로 튜닝할 값. */
    private val pauseAfterMs: Long = 5 * 60_000,
    /** 운전자가 내린 뒤 이 시간이 지나면 종료 확정. 짧은 경유(편의점)를 같은 트립으로 묶기 위한 값. */
    private val endConfirmMs: Long = 3 * 60_000,
) {
    enum class State { IDLE, CANDIDATE, ACTIVE, PAUSED }

    var state = State.IDLE
        private set // 밖에서는 읽기만 가능

    private var driverSeated = false
    private var driverDoorOpened = false // 마지막 이동 이후 운전석 도어가 열린 적이 있는가
    private var leftAtMs: Long? = null   // "도어 열림 + 운전석 비움"이 성립한 시각
    private var startMs = 0L
    private var lastMoveMs = 0L
    private var startKm: Double? = null
    private var lastKm: Double? = null

    /** 이벤트 하나를 반영한다. 이 이벤트 시각 기준으로 트립이 끝났으면 그 [Trip]을 돌려준다. */
    fun onEvent(e: VehicleEvent): Trip? {
        // 먼저 시간을 흘려보낸다. 순서가 반대면 "30분 뒤 다시 탑승" 이벤트가 종료 조건을 지워버린다.
        val ended = tick(e.tMs)
        when {
            e.signal == Signal.SEAT_OCCUPIED && e.target == DRIVER -> {
                driverSeated = e.bool
                if (driverSeated && state == State.IDLE) {
                    state = State.CANDIDATE
                    startMs = e.tMs
                    startKm = lastKm
                    driverDoorOpened = false
                } else if (!driverSeated && state == State.CANDIDATE) {
                    state = State.IDLE // 타기만 하고 움직이지 않음
                }
            }
            e.signal == Signal.DOOR_OPEN && e.target == DRIVER && e.bool -> driverDoorOpened = true
            e.signal == Signal.TRIP_DISTANCE_KM -> {
                val km = e.number
                val prev = lastKm
                lastKm = km
                if (startKm == null) startKm = km
                if (prev != null && km > prev && state != State.IDLE) {
                    state = State.ACTIVE
                    lastMoveMs = e.tMs
                    driverDoorOpened = false
                }
            }
        }
        val left = driverDoorOpened && !driverSeated
        leftAtMs = if (left) leftAtMs ?: e.tMs else null
        return ended
    }

    /** 이벤트가 없어도 시간 경과만으로 일시정지, 종료를 판정한다. 실차에서는 주기 타이머가 호출한다. */
    fun tick(nowMs: Long): Trip? {
        if (state == State.ACTIVE && nowMs - lastMoveMs >= pauseAfterMs) state = State.PAUSED
        val leftAt = leftAtMs
        if (state == State.PAUSED && leftAt != null && nowMs - leftAt >= endConfirmMs) {
            state = State.IDLE
            leftAtMs = null
            // ponytail: 트립 미터가 주행 중 리셋되면 거리가 틀린다. 필요해지면 증가분 누적으로 바꾼다.
            return Trip(startMs, leftAt, (lastKm ?: 0.0) - (startKm ?: 0.0))
        }
        return null
    }

    private companion object {
        const val DRIVER = "DRIVER"
    }
}
