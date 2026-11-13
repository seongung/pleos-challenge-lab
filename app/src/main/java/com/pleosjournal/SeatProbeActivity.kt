package com.pleosjournal

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalTime

/**
 * M0 리스크 검증 화면(디버그용, 런처에 없음): Vehicle SDK 좌석 착석 신호 조회 + 변경 콜백을 로그로 확인한다.
 * M1에서 이 SDK 호출부는 signal/pleos/PleosVehicleSource 로 옮긴다.
 *
 * API 시그니처 출처:
 * - 공식 샘플(Door 패턴): https://github.com/Pleos-Playground/pleos-sample-vehicle-app
 * - Seat 메서드명: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/seat/entry
 * - 파라미터 타입: ai.pleos.playground:Vehicle:2.3.2 AAR 의 클래스 시그니처로 확인
 */
class SeatProbeActivity : ComponentActivity() {

    // mutableStateListOf: Compose가 변경을 관찰하는 리스트. 값이 추가되면 화면이 자동으로 다시 그려진다.
    private val lines = mutableStateListOf<String>()
    private lateinit var vehicle: Vehicle
    private val registered = mutableListOf<Pair<SeatArea, SeatOccupancyListener>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(24.dp)) {
                        Text("M0: Seat occupancy  [REAL]", style = MaterialTheme.typography.headlineSmall)
                        LazyColumn { items(lines) { Text(it) } }
                    }
                }
            }
        }
        startSeatWatch()
    }

    private fun startSeatWatch() {
        try {
            vehicle = Vehicle(applicationContext)
            vehicle.initialize()
        } catch (e: Exception) {
            // 에뮬레이터가 아닌 기기(Vehicle Service 없음)에서는 여기로 온다.
            log("initialize failed: $e")
            return
        }
        val seat = vehicle.getSeat()
        // 차종마다 지원 좌석이 다르므로 capability가 알려주는 좌석만 구독한다. 운전석 위치를 가정하지 않는다.
        seat.checkSeatOccupancyCapabilities(
            onComplete = { areas: List<SeatArea> ->
                log("capability: ${areas.map(::name)}")
                for (area in areas) {
                    seat.getCurrentSeatOccupancy(
                        area,
                        { occupied -> log("get ${name(area)} = $occupied") },
                        { e -> log("get ${name(area)} failed: $e") },
                    )
                    val listener = object : SeatOccupancyListener {
                        override fun onSeatOccupancyUpdated(occupied: Boolean?) =
                            log("callback ${name(area)} = $occupied")

                        override fun onFailed(e: Exception) = log("callback ${name(area)} failed: $e")
                    }
                    seat.registerSeatOccupancy(area, listener)
                    registered += area to listener
                }
            },
            onFailed = { e -> log("capability failed: $e") },
        )
    }

    override fun onDestroy() {
        // 등록할 때와 같은 area, 같은 listener 객체로 해제해야 한다(샘플 앱 튜토리얼 규칙).
        if (::vehicle.isInitialized) {
            try {
                registered.forEach { (area, l) -> vehicle.getSeat().unregisterSeatOccupancy(area, l) }
                vehicle.release()
            } catch (e: Exception) {
                Log.w(TAG, "release failed", e)
            }
        }
        super.onDestroy()
    }

    private fun name(area: SeatArea) = area.values.joinToString("+")

    private fun log(msg: String) {
        Log.i(TAG, msg)
        // SDK 콜백이 어느 스레드에서 오는지 문서에 없으므로 UI 갱신은 메인 스레드로 넘긴다.
        runOnUiThread { lines.add(0, "${LocalTime.now().withNano(0)}  $msg") }
    }

    private companion object {
        const val TAG = "PleosJournalM0"
    }
}
