package com.pleosjournal.signal

import kotlinx.coroutines.flow.Flow

/** 입력 출처. 화면 배지와 저장 데이터에 모두 남긴다. */
enum class Source { REAL, REPLAY, DUMMY }

/** 앱 내부 정규화 신호 이름. Pleos SDK의 이름이 아니다(매핑은 PleosVehicleSource 담당). */
enum class Signal {
    SEAT_OCCUPIED, DOOR_OPEN, TRIP_DISTANCE_KM, CABIN_TEMP_C, HVAC_SET_TEMP_C, LOCATION,
    WINDOW_POSITION_PCT, WIPER_ACTIVE, SEAT_RECLINE_DEG, HEADLAMP_LOW, SPEECH,
}

/**
 * 도메인으로 들어가는 유일한 이벤트 타입. SDK 타입은 여기서 끊긴다.
 * [tMs]는 이벤트 발생 시각(epoch ms). 도메인 로직은 벽시계 대신 이 값만 써서 재생 테스트가 결정적이다.
 */
data class VehicleEvent(
    val tMs: Long,
    val signal: Signal,
    val target: String,
    val value: String,
    val source: Source,
) {
    val bool get() = value.toBoolean()
    val number get() = value.toDouble()
}

/** 모든 신호 소스(Pleos 실제, CSV 재생, 수동 더미)의 공통 인터페이스. */
interface VehicleSignalSource {
    val source: Source
    fun events(): Flow<VehicleEvent>
}
