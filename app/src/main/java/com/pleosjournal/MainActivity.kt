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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.pleosjournal.data.JournalDb
import com.pleosjournal.data.Privacy
import com.pleosjournal.data.PrivacyStore
import com.pleosjournal.data.SceneEntity
import com.pleosjournal.data.SpanEntity
import com.pleosjournal.data.TripEntity
import com.pleosjournal.domain.ClueKind
import com.pleosjournal.domain.DrivingGate
import com.pleosjournal.domain.Recorder
import com.pleosjournal.domain.isMergeCandidate
import com.pleosjournal.domain.isOpenQuestion
import com.pleosjournal.domain.question
import com.pleosjournal.signal.Signal
import com.pleosjournal.signal.replay.ReplaySource
import com.pleosjournal.signal.replay.parseScenario
import com.pleosjournal.speech.FakeStt
import com.pleosjournal.speech.FakeTts
import com.pleosjournal.speech.SpeechToText
import com.pleosjournal.speech.TextToSpeech
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * M1: 시나리오 재생 → 트립(UC-01) + 관측 구간(UC-03) → Room → 타임라인(UC-11).
 * M2: 장면 후보 확인(UC-12) + 음성 회고(UC-07b). 확인, 답변, 삭제는 주행 제한 게이트(UC-09)가 열렸을 때만 가능하다.
 * M3: 트립 병합, 삭제(UC-10) + 프라이버시 설정(UC-18). 이것도 게이트가 열렸을 때만 바꿀 수 있다.
 * 재생 버튼은 개발, 데모용 디버그 조작이라 게이트와 무관하게 둔다.
 */
class MainActivity : ComponentActivity() {
    private val dao by lazy {
        Room.databaseBuilder(this, JournalDb::class.java, "journal.db")
            // ponytail: 스키마가 바뀌면 기존 기록을 지우고 새로 만든다. 실사용 데이터가 생기기 전에 Migration으로 바꾼다.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build().dao()
    }
    private val privacy by lazy { PrivacyStore(this) }
    private val recorder by lazy { Recorder(dao) { privacy.state.value } }
    private val gate by lazy { DrivingGate(this) }
    private fun csv(name: String) = assets.open(name).bufferedReader().use { it.readText() }
    private val scenarioCsv by lazy { csv("jecheon_daytrip.csv") }
    private var showSettings by mutableStateOf(false)
    // 소스 선택 지점. 실제 Pleos STT/TTS가 열리면 여기만 바꾼다.
    private val stt: SpeechToText by lazy { FakeStt(parseScenario(scenarioCsv).filter { it.signal == Signal.SPEECH }.map { it.value }) }
    private val tts: TextToSpeech = FakeTts()
    private var replayJob: Job? = null
    private var listening by mutableStateOf<String?>(null) // 지금 듣고 있는 질문의 키. null이면 듣는 중 아님.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // collectAsState: Flow의 최신 값을 Compose 상태로 바꾼다. 값이 바뀌면 이 화면이 다시 그려진다.
            val trips by dao.trips().collectAsState(emptyList())
            val spans by dao.spans().collectAsState(emptyList())
            val scenes by dao.scenes().collectAsState(emptyList())
            val status by recorder.status.collectAsState()
            val allowed by gate.inputAllowed.collectAsState()
            val p by privacy.state.collectAsState()
            // 차량 화면은 멀리서 보므로 전체 UI를 1.5배로 키운다. Density를 바꾸면 dp, sp가 함께 커진다.
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density * 1.5f, density.fontScale)) {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Pleos Journal", style = MaterialTheme.typography.headlineMedium)
                            Badge("REPLAY") // M1의 입력 소스는 시나리오 재생뿐이다.
                            Badge(if (allowed) "입력 열림" else "입력 닫힘 (주행 제한 또는 상태 불명)")
                            Badge("STT ${stt.source}")
                            if (p.paused) Badge("기록 일시정지")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { replay("jecheon_daytrip.csv") }) { Text("재생: 제천 당일치기") }
                            Button(onClick = { replay("errand_short_stop.csv") }) { Text("재생: 편의점 경유") }
                            OutlinedButton(onClick = { showSettings = !showSettings }) { Text(if (showSettings) "타임라인" else "프라이버시 설정") }
                        }
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        if (showSettings) {
                            PrivacyScreen(p, allowed, privacy::update) { lifecycleScope.launch { dao.deleteAll() } }
                            return@Column
                        }
                        if (trips.isEmpty()) Text("아직 기록된 트립이 없습니다.")
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(trips, key = { it.id }) { trip ->
                                // trips는 최신순이므로 목록에서 바로 다음 항목이 시간상 직전 트립이다.
                                val prev = trips.getOrNull(trips.indexOf(trip) + 1)
                                TripCard(
                                    trip, prev, spans.filter { it.tripId == trip.id }, scenes.filter { it.tripId == trip.id },
                                    allowed, p.voiceConsent, listening, ::answerScene, ::askScene, ::askMemo, ::mergeWithPrev, ::deleteTrip,
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    /** UC-10 병합. 직전 트립 쪽으로 합친다. */
    private fun mergeWithPrev(trip: TripEntity, prev: TripEntity) {
        if (!gate.inputAllowed.value) return
        lifecycleScope.launch { dao.mergeTrips(keepId = prev.id, dropId = trip.id) }
    }

    private fun deleteTrip(trip: TripEntity) {
        if (!gate.inputAllowed.value) return
        lifecycleScope.launch { dao.deleteTrip(trip.id) }
    }

    private fun replay(file: String) {
        replayJob?.cancel() // 재생 중 다시 누르면 처음부터
        // lifecycleScope: 이 화면이 살아 있는 동안만 도는 코루틴 범위. 화면이 사라지면 자동 취소된다.
        replayJob = lifecycleScope.launch {
            recorder.record(ReplaySource(csv(file), SPEED))
        }
    }

    /** 예/아니오 확인(UC-12). 게이트가 닫혀 있으면 아무것도 하지 않는다. */
    private fun answerScene(scene: SceneEntity, status: String) {
        if (!gate.inputAllowed.value) return
        lifecycleScope.launch { dao.answerScene(scene.id, status) }
    }

    private fun askScene(scene: SceneEntity) = ask("scene-${scene.id}", ClueKind.valueOf(scene.kind).question) { text ->
        dao.answerScene(scene.id, SceneEntity.CONFIRMED, text, stt.source.name)
    }

    private fun askMemo(trip: TripEntity) = ask("trip-${trip.id}", MEMO_QUESTION) { text ->
        dao.setMemo(trip.id, text, stt.source.name)
    }

    /** 질문을 읽어 주고(TTS) 한 번 듣는다(STT). 듣는 도중 주행 제한이 걸리면 결과를 버린다(UC-09). */
    private fun ask(key: String, question: String, save: suspend (String) -> Unit) {
        // 음성은 게이트가 열려 있고, 음성 기록에 동의한 경우에만 듣는다(UC-09, UC-18).
        if (!gate.inputAllowed.value || !privacy.state.value.voiceConsent || listening != null) return
        lifecycleScope.launch {
            listening = key
            try {
                tts.speak(question)
                val text = stt.listen()
                if (text != null && gate.inputAllowed.value) save(text)
            } finally {
                listening = null
            }
        }
    }

    override fun onDestroy() {
        gate.close()
        super.onDestroy()
    }

    private companion object {
        const val SPEED = 600 // 2시간 40분 시나리오가 16초
    }
}

private const val MEMO_QUESTION = "이번 이동에서 기억에 남는 순간은?"

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
private fun TripCard(
    trip: TripEntity,
    prev: TripEntity?,
    spans: List<SpanEntity>,
    scenes: List<SceneEntity>,
    allowed: Boolean,
    voiceConsent: Boolean,
    listening: String?,
    onAnswer: (SceneEntity, String) -> Unit,
    onAskScene: (SceneEntity) -> Unit,
    onAskMemo: (TripEntity) -> Unit,
    onMerge: (TripEntity, TripEntity) -> Unit,
    onDelete: (TripEntity) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${fmt("M월 d일", trip.startMs)}  ${hm(trip.startMs)}~${hm(trip.endMs)}  ${"%.1f".format(trip.distanceKm)} km",
                    style = MaterialTheme.typography.titleLarge,
                )
                Badge(trip.source)
                // UC-10: 자동 병합은 하지 않는다. 후보라는 사실만 알리고 사용자가 고른다.
                if (prev != null && isMergeCandidate(prev.endMs, trip.startMs)) {
                    Badge("병합 후보: 직전 트립과 ${(trip.startMs - prev.endMs) / 60_000}분 간격")
                }
            }
            if (allowed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (prev != null) OutlinedButton(onClick = { onMerge(trip, prev) }) { Text("직전 트립과 병합") }
                    // 삭제는 되돌릴 수 없어 두 번 눌러야 실행된다. remember: 이 카드가 화면에 있는 동안 유지되는 상태.
                    var confirm by remember(trip.id) { mutableStateOf(false) }
                    OutlinedButton(onClick = { if (confirm) onDelete(trip) else confirm = true }) {
                        Text(if (confirm) "정말 삭제 (되돌릴 수 없음)" else "트립 삭제")
                    }
                }
            }
            if (spans.isEmpty()) Text("관측된 단서 구간 없음")
            for (s in spans) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${label(s)} ${hm(s.startMs)}~${hm(s.endMs)}", Modifier.width(420.dp))
                    SpanBar(trip, s, Modifier.weight(1f))
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            Text("장면 후보와 회고", style = MaterialTheme.typography.titleMedium)
            if (!allowed) Text("주행 제한 상태에서는 확인과 회고 입력이 닫힙니다.", color = MaterialTheme.colorScheme.error)
            // 사용자가 아니라고 한 후보는 숨긴다. 위의 원시 관측 구간은 그대로 남는다.
            for (scene in scenes.filter { it.status != SceneEntity.DENIED }) {
                val kind = ClueKind.valueOf(scene.kind)
                val evidence = spans.filter { it.kind == scene.kind && it.startMs < scene.endMs && it.endMs > scene.startMs }
                Column(Modifier.padding(top = 6.dp)) {
                    Text(kind.question, style = MaterialTheme.typography.titleMedium)
                    Text("근거: " + evidence.joinToString(", ") { "${label(it)} ${hm(it.startMs)}~${hm(it.endMs)}" })
                    if (scene.status == SceneEntity.CONFIRMED) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (scene.answer == null) "확인됨: 네" else "답변: ${scene.answer}", color = MaterialTheme.colorScheme.primary)
                            scene.answerSource?.let { Badge("STT $it") }
                        }
                    } else if (allowed) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (kind.isOpenQuestion) {
                                if (voiceConsent) VoiceButton("음성으로 답하기", listening, "scene-${scene.id}") { onAskScene(scene) }
                            } else {
                                Button(onClick = { onAnswer(scene, SceneEntity.CONFIRMED) }) { Text("맞아요") }
                            }
                            OutlinedButton(onClick = { onAnswer(scene, SceneEntity.DENIED) }) { Text(if (kind.isOpenQuestion) "해당 없음" else "아니에요") }
                        }
                    }
                }
            }
            Column(Modifier.padding(top = 6.dp)) {
                Text(MEMO_QUESTION, style = MaterialTheme.typography.titleMedium)
                trip.memo?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("답변: $it", color = MaterialTheme.colorScheme.primary)
                        trip.memoSource?.let { src -> Badge("STT $src") }
                    }
                }
                if (allowed && voiceConsent) VoiceButton(if (trip.memo == null) "음성으로 기록" else "다시 말하기", listening, "trip-${trip.id}") { onAskMemo(trip) }
                if (allowed && !voiceConsent) Text("음성 기록은 프라이버시 설정에서 동의한 뒤 사용할 수 있습니다.")
            }
        }
    }
}

/** UC-18 최소 화면: 항목별 수집 on/off, 기록 일시정지, 음성 기록 동의, 전체 삭제. 주행 제한 중에는 바꿀 수 없다. */
@Composable
private fun PrivacyScreen(p: Privacy, allowed: Boolean, onChange: (Privacy) -> Unit, onDeleteAll: () -> Unit) {
    @Composable
    fun Toggle(title: String, detail: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(checked = checked, enabled = allowed, onCheckedChange = onToggle)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("프라이버시 설정", style = MaterialTheme.typography.titleLarge)
        Text("모든 기록은 이 기기 안에만 저장됩니다. 앱이 외부 API를 직접 사용하지 않습니다.")
        if (!allowed) Text("주행 제한 상태에서는 설정을 바꿀 수 없습니다.", color = MaterialTheme.colorScheme.error)
        Toggle("기록 일시정지", "켜 두면 트립과 단서를 전혀 기록하지 않습니다.", p.paused) { onChange(p.copy(paused = it)) }
        Toggle("음성 기록 동의", "동승자를 포함해 차 안의 음성을 글자로 바꿔 저장하는 데 동의합니다. 꺼져 있으면 음성 입력을 열지 않습니다.", p.voiceConsent) {
            onChange(p.copy(voiceConsent = it))
        }
        HorizontalDivider()
        Text("항목별 수집", style = MaterialTheme.typography.titleMedium)
        for (kind in ClueKind.entries) {
            Toggle(kindName(kind), "끄면 이 항목의 관측 구간을 만들지 않습니다.", kind.name !in p.disabledKinds) { on ->
                onChange(p.copy(disabledKinds = if (on) p.disabledKinds - kind.name else p.disabledKinds + kind.name))
            }
        }
        HorizontalDivider()
        var confirm by remember { mutableStateOf(false) }
        OutlinedButton(enabled = allowed, onClick = { if (confirm) { onDeleteAll(); confirm = false } else confirm = true }) {
            Text(if (confirm) "정말 모두 삭제 (되돌릴 수 없음)" else "모든 기록 삭제")
        }
    }
}

/** 듣는 동안에는 모든 음성 버튼을 잠그고, 눌린 버튼만 "듣는 중"으로 바꾼다. */
@Composable
private fun VoiceButton(text: String, listening: String?, key: String, onClick: () -> Unit) {
    Button(enabled = listening == null, onClick = onClick) { Text(if (listening == key) "듣는 중…" else text) }
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
private fun kindName(kind: ClueKind) = when (kind) {
    ClueKind.WIPER -> "와이퍼 작동"
    ClueKind.HEADLAMP -> "전조등 켜짐"
    ClueKind.WINDOW_OPEN -> "창문 열림"
    ClueKind.REAR_RECLINE -> "뒷좌석 등받이 젖힘"
    ClueKind.DOOR_OPEN -> "도어 열림"
}

private fun label(s: SpanEntity): String {
    val what = kindName(ClueKind.valueOf(s.kind))
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
