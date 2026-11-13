package com.pleosjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.pleosjournal.data.JournalDb
import com.pleosjournal.data.SpanEntity
import com.pleosjournal.data.TripEntity
import com.pleosjournal.domain.ClueKind
import com.pleosjournal.domain.Recorder
import com.pleosjournal.signal.replay.ReplaySource
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * M1: 시나리오 재생 → 트립(UC-01) + 관측 구간(UC-03) → Room → 타임라인(UC-11).
 * 재생 버튼은 개발, 데모용 디버그 조작이다. 주행 제한 게이트(UC-09)는 M3에서 붙인다.
 */
class MainActivity : ComponentActivity() {
    private val dao by lazy { Room.databaseBuilder(this, JournalDb::class.java, "journal.db").build().dao() }
    private val recorder by lazy { Recorder(dao) }
    private var replayJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // collectAsState: Flow의 최신 값을 Compose 상태로 바꾼다. 값이 바뀌면 이 화면이 다시 그려진다.
            val trips by dao.trips().collectAsState(emptyList())
            val spans by dao.spans().collectAsState(emptyList())
            val status by recorder.status.collectAsState()
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Pleos Journal", style = MaterialTheme.typography.headlineMedium)
                            Badge("REPLAY") // M1의 입력 소스는 시나리오 재생뿐이다.
                            Button(onClick = ::replay) { Text("시나리오 재생 (${SPEED}배속)") }
                            OutlinedButton(onClick = { lifecycleScope.launch { dao.deleteAll() } }) { Text("기록 지우기") }
                        }
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        if (trips.isEmpty()) Text("아직 기록된 트립이 없습니다.")
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(trips, key = { it.id }) { trip -> TripCard(trip, spans.filter { it.tripId == trip.id }) }
                        }
                    }
                }
            }
        }
    }

    private fun replay() {
        replayJob?.cancel() // 재생 중 다시 누르면 처음부터
        // lifecycleScope: 이 화면이 살아 있는 동안만 도는 코루틴 범위. 화면이 사라지면 자동 취소된다.
        replayJob = lifecycleScope.launch {
            val csv = assets.open("jecheon_daytrip.csv").bufferedReader().use { it.readText() }
            recorder.record(ReplaySource(csv, SPEED))
        }
    }

    private companion object {
        const val SPEED = 600 // 2시간 40분 시나리오가 16초
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text,
        Modifier.background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun TripCard(trip: TripEntity, spans: List<SpanEntity>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${fmt("M월 d일", trip.startMs)}  ${hm(trip.startMs)}~${hm(trip.endMs)}  ${"%.1f".format(trip.distanceKm)} km",
                    style = MaterialTheme.typography.titleLarge,
                )
                Badge(trip.source)
            }
            if (spans.isEmpty()) Text("관측된 단서 구간 없음")
            for (s in spans) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${label(s)} ${hm(s.startMs)}~${hm(s.endMs)}", Modifier.width(420.dp))
                    SpanBar(trip, s, Modifier.weight(1f))
                }
            }
        }
    }
}

/** 트립 전체를 가로축으로 놓고 구간 위치를 막대로 그린다. weight는 0이면 안 되므로 최소값을 둔다. */
@Composable
private fun SpanBar(trip: TripEntity, s: SpanEntity, modifier: Modifier) {
    fun w(ms: Long) = ms.toFloat().coerceAtLeast(1f)
    Row(modifier.height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Spacer(Modifier.weight(w(s.startMs - trip.startMs)))
        Box(Modifier.weight(w(s.endMs - s.startMs)).height(12.dp).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.weight(w(trip.endMs - s.endMs)))
    }
}

/** 관측 사실만 말한다. "비가 왔다" 같은 해석은 붙이지 않는다(해석은 M2에서 질문으로 제안). */
private fun label(s: SpanEntity): String {
    val what = when (ClueKind.valueOf(s.kind)) {
        ClueKind.WIPER -> "와이퍼 작동"
        ClueKind.HEADLAMP -> "전조등 켜짐"
        ClueKind.WINDOW_OPEN -> "창문 열림"
        ClueKind.REAR_RECLINE -> "뒷좌석 등받이 젖힘"
        ClueKind.DOOR_OPEN -> "도어 열림"
    }
    val where = when (s.target) {
        "" -> ""
        "DRIVER" -> "운전석"
        "FRONT_PASSENGER" -> "조수석"
        "FRONT_LEFT" -> "앞 왼쪽"
        "FRONT_RIGHT" -> "앞 오른쪽"
        "REAR_LEFT" -> "뒤 왼쪽"
        "REAR_RIGHT" -> "뒤 오른쪽"
        "FRONT" -> "앞"
        else -> s.target
    }
    return if (where.isEmpty()) what else "$what ($where)"
}

private fun fmt(pattern: String, ms: Long) = SimpleDateFormat(pattern, Locale.KOREA).format(Date(ms))
private fun hm(ms: Long) = fmt("HH:mm", ms)
