package com.pleosjournal.signal.pleos

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.LightStateListener
import ai.pleos.playground.vehicle.listener.SeatBackrestAnglePositionListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.WindowPositionListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.content.Context
import android.util.Log
import com.pleosjournal.signal.Signal
import com.pleosjournal.signal.Source
import com.pleosjournal.signal.VehicleEvent
import com.pleosjournal.signal.VehicleSignalSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 실제 Pleos Vehicle SDK 신호를 앱 내부 이벤트(VehicleEvent)로 바꾼다. SDK 타입은 이 파일 밖으로 나가지 않는다.
 *
 * API 출처 (메서드 시그니처와 퍼미션은 각 메서드 문서에서 확인):
 *   https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/
 *     seat/register-seat-occupancy, seat/register-seat-backrest-angle-position, door/register-door-position,
 *     window/register-window-position, wiper/register-wiper-state, light/register-low-beam-light-state,
 *     odometer/register-distance-driven (단위: km)
 *   호출 패턴: https://github.com/Pleos-Playground/pleos-sample-vehicle-app
 *
 * ===== 문서에 값의 의미가 없어 에뮬레이터 관측으로 정한 것 (확인 필요, docs/06_open_questions.md 17번) =====
 * 실차에서 다르면 아래 상수와 매핑 함수만 고친다.
 */
class PleosVehicleSource(private val context: Context) : VehicleSignalSource {
    override val source = Source.REAL

    // callbackFlow: 콜백 기반 API를 Flow로 바꾼다. trySend로 값을 내보내고, 수집이 끝나면 awaitClose 블록이 실행된다.
    override fun events(): Flow<VehicleEvent> = callbackFlow {
        val vehicle = Vehicle(context.applicationContext)
        try {
            vehicle.initialize()
        } catch (e: Exception) {
            close(e) // Vehicle Service가 없는 기기
            return@callbackFlow
        }
        val cleanup = CopyOnWriteArrayList<() -> Unit>() // SDK 콜백은 다른 스레드에서 온다
        fun emit(signal: Signal, target: String, value: Any) {
            trySend(VehicleEvent(System.currentTimeMillis(), signal, target, value.toString(), Source.REAL))
        }
        fun failed(what: String) = { e: Exception -> Log.w(TAG, "$what failed: $e"); Unit }

        // 차종마다 지원 범위가 달라 capability가 알려준 대상만 구독한다. 등록하면 현재 값이 한 번 온다.
        val seat = vehicle.getSeat()
        seat.checkSeatOccupancyCapabilities(
            onComplete = { areas: List<SeatArea> ->
                for (area in areas) {
                    val target = seatTarget(area) ?: continue
                    val l = object : SeatOccupancyListener {
                        override fun onSeatOccupancyUpdated(occupancy: Boolean?) {
                            if (occupancy != null) emit(Signal.SEAT_OCCUPIED, target, occupancy == SEAT_OCCUPIED_RAW)
                        }
                        override fun onFailed(e: Exception) = failed("seat occupancy $target")(e)
                    }
                    seat.registerSeatOccupancy(area, l)
                    cleanup += { seat.unregisterSeatOccupancy(area, l) }
                }
            },
            onFailed = failed("seat occupancy capability"),
        )
        seat.checkSeatBackrestAnglePositionCapabilities(
            onComplete = { _, areas ->
                for (area in areas.keys) {
                    val target = seatTarget(area)?.takeIf { it.startsWith("REAR") } ?: continue
                    val l = object : SeatBackrestAnglePositionListener {
                        override fun onSeatBackrestAnglePositionUpdated(position: Int?) {
                            if (position != null) emit(Signal.SEAT_RECLINE_DEG, target, position)
                        }
                        override fun onFailed(e: Exception) = failed("backrest $target")(e)
                    }
                    seat.registerSeatBackrestAnglePosition(area, l)
                    cleanup += { seat.unregisterSeatBackrestAnglePosition(area, l) }
                }
            },
            onFailed = failed("backrest capability"),
        )

        val door = vehicle.getDoor()
        door.checkDoorPositionCapabilities(
            onComplete = { areas: Map<DoorArea, *> ->
                for (area in areas.keys) {
                    val target = doorTarget(area) ?: continue
                    val l = object : DoorPositionListener {
                        override fun onPositionUpdated(pos: Int?) {
                            if (pos != null) emit(Signal.DOOR_OPEN, target, pos > DOOR_CLOSED_RAW)
                        }
                        override fun onFailed(e: Exception) = failed("door $target")(e)
                    }
                    door.registerDoorPosition(area, l)
                    cleanup += { door.unregisterDoorPosition(area, l) }
                }
            },
            onFailed = failed("door capability"),
        )

        val window = vehicle.getWindow()
        window.checkWindowPositionCapabilities(
            onComplete = { areas: Map<WindowArea, *> ->
                for (area in areas.keys) {
                    val target = windowTarget(area) ?: continue
                    val l = object : WindowPositionListener {
                        override fun onWindowPositionUpdated(position: Int?) {
                            if (position != null) emit(Signal.WINDOW_POSITION_PCT, target, position)
                        }
                        override fun onFailed(e: Exception) = failed("window $target")(e)
                    }
                    window.registerWindowPosition(area, l)
                    cleanup += { window.unregisterWindowPosition(area, l) }
                }
            },
            onFailed = failed("window capability"),
        )

        val wiper = vehicle.getWiper()
        wiper.checkWiperStateCapabilities(
            onComplete = { areas: Map<WindowArea, List<WiperState>> ->
                for (area in areas.keys) {
                    if (area.values.singleOrNull() != WindowValue.FRONT_WINDSHIELD) continue
                    val l = object : WiperStateListener {
                        override fun onWiperStateUpdated(state: WiperState?) {
                            if (state != null) emit(Signal.WIPER_ACTIVE, "FRONT", state == WiperState.ON)
                        }
                        override fun onFailed(e: Exception) = failed("wiper")(e)
                    }
                    wiper.registerWiperState(area, l)
                    cleanup += { wiper.unregisterWiperState(area, l) }
                }
            },
            onFailed = failed("wiper capability"),
        )

        val light = vehicle.getLight()
        val lightListener = object : LightStateListener {
            override fun onLightStateUpdated(state: LightState?) {
                // 주간주행등(DAYTIME_RUNNING)은 "전조등 켜짐"으로 치지 않는다.
                if (state != null) emit(Signal.HEADLAMP_LOW, "", state == LightState.ON)
            }
            override fun onFailed(e: Exception) = failed("low beam")(e)
        }
        runCatching { light.registerLowBeamLightState(lightListener) }
            .onSuccess { cleanup += { light.unregisterLowBeamLightState(lightListener) } }
            .onFailure { Log.w(TAG, "low beam register failed: $it") }

        val odometer = vehicle.getOdometer()
        // 거리는 누적 주행거리(odometer, km)를 쓴다. TripDetector는 증가분만 보므로 누적값이어도 된다.
        // "현재 트립 거리"(registerTripCurrentDrivingDistance)는 에뮬레이터 v2.1.0에서 ERROR_NOT_SUPPORTED_PROPERTY다.
        val distanceListener = object : DistanceDrivenListener {
            override fun onDistanceDrivenUpdated(value: Float?) {
                if (value != null) emit(Signal.TRIP_DISTANCE_KM, "", value)
            }
            override fun onFailed(e: Exception) = failed("odometer")(e)
        }
        runCatching { odometer.registerDistanceDriven(distanceListener) }
            .onSuccess { cleanup += { odometer.unregisterDistanceDriven(distanceListener) } }
            .onFailure { Log.w(TAG, "odometer register failed: $it") }

        awaitClose {
            // 등록할 때와 같은 area, 같은 listener 객체로 해제해야 한다(샘플 앱 규칙). 해제 후 release.
            cleanup.forEach { runCatching(it) }
            runCatching { vehicle.release() }
        }
    }

    private companion object {
        const val TAG = "PleosVehicleSource"

        /**
         * SDK의 착석 Boolean 중 "앉아 있음"을 뜻하는 값. 문서에는 의미가 없다.
         * 에뮬레이터 v2.1.0의 Virtual Console에서 Occupied로 두면 false가 온다(반복 재현). 실차 확인 필요.
         */
        const val SEAT_OCCUPIED_RAW = false

        /** 도어 위치 값 중 "닫힘". 이보다 크면 열림으로 본다. 에뮬레이터 관측값. 실차 확인 필요. */
        const val DOOR_CLOSED_RAW = 0

        // 좌핸들(LHD) 기준. 우핸들 차량이면 DRIVER와 FRONT_PASSENGER가 바뀐다. 확인 필요.
        fun seatTarget(area: SeatArea) = when (area.values.singleOrNull()) {
            SeatValue.ROW_1_LEFT -> "DRIVER"
            SeatValue.ROW_1_RIGHT -> "FRONT_PASSENGER"
            SeatValue.ROW_2_LEFT -> "REAR_LEFT"
            SeatValue.ROW_2_CENTER -> "REAR_CENTER"
            SeatValue.ROW_2_RIGHT -> "REAR_RIGHT"
            else -> null
        }

        fun doorTarget(area: DoorArea) = when (area.values.singleOrNull()) {
            DoorValue.ROW_1_LEFT -> "DRIVER"
            DoorValue.ROW_1_RIGHT -> "FRONT_PASSENGER"
            DoorValue.ROW_2_LEFT -> "REAR_LEFT"
            DoorValue.ROW_2_RIGHT -> "REAR_RIGHT"
            else -> null // 후드, 트렁크 등은 쓰지 않는다
        }

        fun windowTarget(area: WindowArea) = when (area.values.singleOrNull()) {
            WindowValue.ROW_1_LEFT -> "FRONT_LEFT"
            WindowValue.ROW_1_RIGHT -> "FRONT_RIGHT"
            WindowValue.ROW_2_LEFT -> "REAR_LEFT"
            WindowValue.ROW_2_RIGHT -> "REAR_RIGHT"
            else -> null
        }
    }
}
