package com.pleosjournal.ui

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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pleosjournal.domain.ClueKind
import com.pleosjournal.presentation.JournalActions
import com.pleosjournal.presentation.JournalUiState
import com.pleosjournal.presentation.PrivacyUi
import com.pleosjournal.presentation.SceneUi
import com.pleosjournal.presentation.SpanUi
import com.pleosjournal.presentation.TripUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * ===== View (이 ui/ 패키지는 화면 담당자가 자유롭게 고친다) =====
 *
 * 규칙은 두 가지뿐이다.
 * 1. 화면은 JournalUiState만 보고 그린다. 동작은 JournalActions의 함수만 부른다.
 * 2. DB, SDK, 신호, 음성 쪽 코드를 import 하지 않는다(ArchitectureTest가 검사한다).
 *
 * 화면 전환, 삭제 재확인처럼 "보이는 방식"에만 관련된 상태는 여기서 remember로 관리한다.
 * 기능을 빼려면 그 부분을 그리지 않으면 된다. 에뮬레이터 없이 보려면 아래 @Preview를 쓴다(SampleState.kt).
 */

@Composable
fun JournalScreen(state: JournalUiState, actions: JournalActions) {
    var showSettings by remember { mutableStateOf(false) }
    // 차량 화면은 멀리서 보므로 전체 UI를 1.5배로 키운다. Density를 바꾸면 dp, sp가 함께 커진다.
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density * 1.5f, density.fontScale)) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pleos Journal", style = MaterialTheme.typography.headlineMedium)
                        Badge(state.inputSource)
                        Badge(if (state.inputAllowed) "입력 열림" else "입력 닫힘 (주행 제한 또는 상태 불명)")
                        Badge("STT ${state.sttSource}")
                        if (state.privacy.paused) Badge("기록 일시정지")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (s in state.scenarios) Button(onClick = { actions.replay(s.file) }) { Text("재생: ${s.title}") }
                        if (state.recordingReal) {
                            OutlinedButton(onClick = actions::stopRecording) { Text("실제 신호 기록 중지") }
                        } else {
                            Button(onClick = actions::recordReal) { Text("실제 신호 기록") }
                        }
                        OutlinedButton(onClick = { showSettings = !showSettings }) { Text(if (showSettings) "타임라인" else "프라이버시 설정") }
                    }
                    Text(state.status, style = MaterialTheme.typography.bodyMedium)
                    when {
                        state.needsOnboarding -> Onboarding(state.inputAllowed, actions)
                        showSettings -> PrivacyScreen(state.privacy, state.inputAllowed, actions)
                        else -> Timeline(state, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun Timeline(state: JournalUiState, actions: JournalActions) {
    if (state.trips.isEmpty()) Text("아직 기록된 트립이 없습니다.")
    val listState = rememberLazyListState()
    // 새 트립이 맨 위에 생기면 그쪽으로 스크롤한다. LaunchedEffect는 key(맨 위 트립 id)가 바뀔 때만 다시 실행된다.
    LaunchedEffect(state.trips.firstOrNull()?.id) { listState.animateScrollToItem(0) }
    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.trips, key = { it.id }) { TripCard(it, state.inputAllowed, state.voiceAllowed, state.voiceBusy, actions) }
    }
}

/** UC-18 최초 실행 동의. 무엇을 기록하는지 알리고, 동의한 뒤에만 기록을 시작한다. */
@Composable
private fun Onboarding(allowed: Boolean, actions: JournalActions) {
    var voice by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("시작하기 전에", style = MaterialTheme.typography.titleLarge)
        Text("이 앱은 이동 중 아래 차량 상태를 관측해 구간으로 기록합니다.")
        Text("운전석 착석과 도어, 트립 거리, 와이퍼, 전조등, 창문, 뒷좌석 등받이")
        Text("기록은 이 기기 안에만 저장됩니다. 앱이 외부 API를 직접 사용하지 않습니다.")
        Text("관측한 내용은 사실로만 남기고, 해석은 질문으로 여쭤본 뒤 확인하신 것만 저장합니다.")
        Text("항목별 수집 끄기, 기록 일시정지, 전체 삭제는 프라이버시 설정에서 언제든 할 수 있습니다.")
        Toggle(
            "음성 기록 동의 (선택)",
            "동승자를 포함해 차 안의 음성을 글자로 바꿔 저장하는 데 동의합니다. 나중에 바꿀 수 있습니다.",
            voice, allowed,
        ) { voice = it }
        if (allowed) {
            Button(onClick = { actions.completeOnboarding(voice) }) { Text("동의하고 시작") }
        } else {
            Text("주행 제한 상태에서는 동의할 수 없습니다.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun TripCard(trip: TripUi, allowed: Boolean, voiceAllowed: Boolean, voiceBusy: Boolean, actions: JournalActions) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${fmt("M월 d일", trip.startMs)}  ${hm(trip.startMs)}~${hm(trip.endMs)}  ${"%.1f".format(trip.distanceKm)} km",
                    style = MaterialTheme.typography.titleLarge,
                )
                Badge(trip.source)
                trip.mergeCandidateGapMin?.let { Badge("병합 후보: 직전 트립과 ${it}분 간격") }
            }
            if (allowed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (trip.canMergeWithPrevious) OutlinedButton(onClick = { actions.mergeWithPrevious(trip.id) }) { Text("직전 트립과 병합") }
                    ConfirmButton("트립 삭제", "정말 삭제 (되돌릴 수 없음)", trip.id) { actions.deleteTrip(trip.id) }
                }
            }

            if (trip.spans.isEmpty()) Text("관측된 단서 구간 없음")
            for (s in trip.spans) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(spanText(s), Modifier.width(420.dp))
                    SpanBar(s, Modifier.weight(1f))
                }
            }

            // UC-10: 정차 지점마다 트립을 나눌 수 있다. 병합으로 되돌릴 수 있어 재확인은 하지 않는다.
            for (stop in trip.stops) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("정차 ${hm(stop.startMs)}~${hm(stop.endMs)}")
                    if (allowed) OutlinedButton(onClick = { actions.splitAtStop(trip.id, stop.id) }) { Text("여기서 트립 나누기") }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            Text("장면 후보와 회고", style = MaterialTheme.typography.titleMedium)
            if (!allowed) Text("주행 제한 상태에서는 확인과 회고 입력이 닫힙니다.", color = MaterialTheme.colorScheme.error)
            for (scene in trip.scenes) SceneRow(scene, allowed, voiceAllowed, voiceBusy, actions)
            if (allowed && trip.hiddenSceneCount > 0) {
                OutlinedButton(onClick = { actions.restoreHiddenScenes(trip.id) }) { Text("숨긴 후보 ${trip.hiddenSceneCount}개 다시 보기") }
            }

            Column(Modifier.padding(top = 6.dp)) {
                Text(trip.memoQuestion, style = MaterialTheme.typography.titleMedium)
                trip.memo?.let { Answer("답변: $it", trip.memoSource) }
                if (voiceAllowed) {
                    VoiceButton(if (trip.memo == null) "음성으로 기록" else "다시 말하기", trip.memoListening, voiceBusy) { actions.recordMemoByVoice(trip.id) }
                } else if (allowed) {
                    Text("음성 기록은 프라이버시 설정에서 동의한 뒤 사용할 수 있습니다.")
                }
            }
        }
    }
}

@Composable
private fun SceneRow(scene: SceneUi, allowed: Boolean, voiceAllowed: Boolean, voiceBusy: Boolean, actions: JournalActions) {
    Column(Modifier.padding(top = 6.dp)) {
        Text(scene.question, style = MaterialTheme.typography.titleMedium)
        // 근거가 되는 관측 사실을 질문과 항상 함께 보여준다.
        Text("근거: " + scene.evidence.joinToString(", ", transform = ::spanText))
        if (scene.confirmed) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Answer(if (scene.answer == null) "확인됨: 네" else "답변: ${scene.answer}", scene.answerSource)
                if (allowed) OutlinedButton(onClick = { actions.resetScene(scene.id) }) { Text("되돌리기") }
            }
        } else if (allowed) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!scene.isOpenQuestion) {
                    Button(onClick = { actions.confirmScene(scene.id) }) { Text("맞아요") }
                } else if (voiceAllowed) {
                    VoiceButton("음성으로 답하기", scene.listening, voiceBusy) { actions.answerSceneByVoice(scene.id) }
                }
                OutlinedButton(onClick = { actions.denyScene(scene.id) }) { Text(if (scene.isOpenQuestion) "해당 없음" else "아니에요") }
            }
        }
    }
}

/** UC-18 최소 화면. 주행 제한 중에는 바꿀 수 없다. */
@Composable
private fun PrivacyScreen(p: PrivacyUi, allowed: Boolean, actions: JournalActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("프라이버시 설정", style = MaterialTheme.typography.titleLarge)
        Text("모든 기록은 이 기기 안에만 저장됩니다. 앱이 외부 API를 직접 사용하지 않습니다.")
        if (!allowed) Text("주행 제한 상태에서는 설정을 바꿀 수 없습니다.", color = MaterialTheme.colorScheme.error)
        Toggle("기록 일시정지", "켜 두면 트립과 단서를 전혀 기록하지 않습니다.", p.paused, allowed, actions::setPaused)
        Toggle(
            "음성 기록 동의",
            "동승자를 포함해 차 안의 음성을 글자로 바꿔 저장하는 데 동의합니다. 꺼져 있으면 음성 입력을 열지 않습니다.",
            p.voiceConsent, allowed, actions::setVoiceConsent,
        )
        HorizontalDivider()
        Text("항목별 수집", style = MaterialTheme.typography.titleMedium)
        for (t in p.kinds) {
            Toggle(kindName(t.kind), "끄면 이 항목의 관측 구간을 만들지 않습니다.", t.enabled, allowed) { actions.setKindEnabled(t.kind, it) }
        }
        HorizontalDivider()
        if (allowed) ConfirmButton("모든 기록 삭제", "정말 모두 삭제 (되돌릴 수 없음)", Unit, actions::deleteAll)
    }
}

// ----- 작은 부품 -----

@Composable
private fun Toggle(title: String, detail: String, checked: Boolean, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Switch(checked = checked, enabled = enabled, onCheckedChange = onToggle)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** 되돌릴 수 없는 동작용. 두 번 눌러야 실행된다. [key]가 바뀌면 확인 상태를 초기화한다. */
@Composable
private fun ConfirmButton(text: String, confirmText: String, key: Any, onConfirmed: () -> Unit) {
    var confirm by remember(key) { mutableStateOf(false) }
    OutlinedButton(onClick = { if (confirm) { confirm = false; onConfirmed() } else confirm = true }) {
        Text(if (confirm) confirmText else text)
    }
}

@Composable
private fun VoiceButton(text: String, listening: Boolean, busy: Boolean, onClick: () -> Unit) {
    // 듣는 동안에는 모든 음성 버튼을 잠그고, 듣고 있는 질문의 버튼만 "듣는 중"으로 바꾼다.
    Button(enabled = !busy, onClick = onClick) { Text(if (listening) "듣는 중…" else text) }
}

@Composable
private fun Answer(text: String, source: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, color = MaterialTheme.colorScheme.primary)
        source?.let { Badge("STT $it") }
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

/** 트립 전체를 가로축으로 놓고 구간 위치를 막대로 그린다. weight는 0이면 안 되므로 최소값을 둔다. */
@Composable
private fun SpanBar(s: SpanUi, modifier: Modifier) {
    fun w(f: Float) = f.coerceAtLeast(0.0001f)
    Row(modifier.height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Spacer(Modifier.weight(w(s.startFraction)))
        Box(Modifier.weight(w(s.endFraction - s.startFraction)).height(12.dp).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.weight(w(1f - s.endFraction)))
    }
}

// ----- 문구. 관측 사실만 말한다. "비가 왔다" 같은 해석은 붙이지 않는다. -----

private fun kindName(kind: ClueKind) = when (kind) {
    ClueKind.WIPER -> "와이퍼 작동"
    ClueKind.HEADLAMP -> "전조등 켜짐"
    ClueKind.WINDOW_OPEN -> "창문 열림"
    ClueKind.REAR_RECLINE -> "뒷좌석 등받이 젖힘"
    ClueKind.DOOR_OPEN -> "도어 열림"
}

private fun targetName(target: String) = when (target) {
    "" -> ""
    "DRIVER" -> "운전석"
    "FRONT_PASSENGER" -> "조수석"
    "FRONT_LEFT" -> "앞 왼쪽"
    "FRONT_RIGHT" -> "앞 오른쪽"
    "REAR_LEFT" -> "뒤 왼쪽"
    "REAR_RIGHT" -> "뒤 오른쪽"
    "FRONT" -> "앞"
    else -> target
}

private fun spanText(s: SpanUi): String {
    val where = targetName(s.target)
    return kindName(s.kind) + (if (where.isEmpty()) "" else " ($where)") + " ${hm(s.startMs)}~${hm(s.endMs)}"
}

private fun fmt(pattern: String, ms: Long) = SimpleDateFormat(pattern, Locale.KOREA).format(Date(ms))
private fun hm(ms: Long) = fmt("HH:mm", ms)

// ----- 미리보기: Android Studio의 Split/Design 창에서 에뮬레이터 없이 보인다 -----

@Preview(widthDp = 1680, heightDp = 1260)
@Composable
private fun TimelinePreview() = JournalScreen(sampleState, NoActions)

@Preview(widthDp = 1680, heightDp = 1260)
@Composable
private fun DrivingPreview() = JournalScreen(sampleState.copy(inputAllowed = false, voiceAllowed = false), NoActions)
