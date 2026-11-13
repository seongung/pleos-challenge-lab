package com.pleosjournal.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.pleosjournal.data.JournalDb
import com.pleosjournal.data.Privacy
import com.pleosjournal.data.PrivacyStore
import com.pleosjournal.data.SceneEntity
import com.pleosjournal.data.SpanEntity
import com.pleosjournal.data.TripEntity
import com.pleosjournal.domain.ClueKind
import com.pleosjournal.domain.isMergeCandidate
import com.pleosjournal.domain.isOpenQuestion
import com.pleosjournal.domain.question
import com.pleosjournal.platform.DrivingGate
import com.pleosjournal.signal.Signal
import com.pleosjournal.signal.replay.ReplaySource
import com.pleosjournal.signal.replay.parseScenario
import com.pleosjournal.speech.FakeStt
import com.pleosjournal.speech.FakeTts
import com.pleosjournal.speech.SpeechToText
import com.pleosjournal.speech.TextToSpeech
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 화면과 나머지(도메인, 저장, 신호, 음성, 플랫폼)를 잇는 유일한 지점.
 * - 여러 Flow를 합쳐 [JournalUiState] 하나로 만든다.
 * - [JournalActions]를 구현한다. 주행 제한(UC-09)과 음성 동의(UC-18) 검사는 여기서 한다.
 * - 소스 선택(REAL / REPLAY / DUMMY)도 여기 한 곳에서 한다.
 *
 * ViewModel은 화면이 다시 만들어져도 살아남는 객체다. viewModelScope의 코루틴은 ViewModel이 끝날 때 취소된다.
 */
class JournalViewModel(app: Application) : AndroidViewModel(app), JournalActions {
    // ----- 조립(소스 선택 지점). 실제 Pleos 소스가 준비되면 여기만 바꾼다. -----
    private val dao = Room.databaseBuilder(app, JournalDb::class.java, "journal.db")
        // ponytail: 스키마가 바뀌면 기존 기록을 지우고 새로 만든다. 실사용 데이터가 생기기 전에 Migration으로 바꾼다.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build().dao()
    private val privacy = PrivacyStore(app)
    private val recorder = Recorder(dao) { privacy.state.value }
    private val gate = DrivingGate(app)
    private val stt: SpeechToText =
        FakeStt(parseScenario(csv(SPEECH_SCENARIO)).filter { it.signal == Signal.SPEECH }.map { it.value })
    private val tts: TextToSpeech = FakeTts()

    private val scenarios = app.assets.list("").orEmpty().filter { it.endsWith(".csv") }.sorted()
        .map { ScenarioUi(it, SCENARIO_TITLES[it] ?: it.removeSuffix(".csv")) }

    private var replayJob: Job? = null
    private val listening = MutableStateFlow<String?>(null) // 지금 듣고 있는 질문의 키

    // ----- 상태 -----
    private val records = combine(dao.trips(), dao.spans(), dao.scenes(), ::Triple)

    val state: StateFlow<JournalUiState> =
        combine(records, recorder.status, gate.inputAllowed, privacy.state, listening) { (trips, spans, scenes), status, allowed, p, key ->
            JournalUiState(
                sttSource = stt.source.name,
                inputAllowed = allowed,
                voiceAllowed = allowed && p.voiceConsent,
                status = status,
                scenarios = scenarios,
                trips = trips.mapIndexed { i, trip ->
                    // trips는 최신순이므로 목록의 다음 항목이 시간상 직전 트립이다.
                    tripUi(trip, trips.getOrNull(i + 1), spans.filter { it.tripId == trip.id }, scenes.filter { it.tripId == trip.id }, key)
                },
                privacy = PrivacyUi(p.paused, p.voiceConsent, ClueKind.entries.map { KindToggleUi(it, it.name !in p.disabledKinds) }),
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, JournalUiState(scenarios = scenarios))

    private fun tripUi(trip: TripEntity, prev: TripEntity?, spans: List<SpanEntity>, scenes: List<SceneEntity>, key: String?): TripUi {
        val length = (trip.endMs - trip.startMs).coerceAtLeast(1).toFloat()
        fun spanUi(s: SpanEntity) = SpanUi(
            ClueKind.valueOf(s.kind), s.target, s.startMs, s.endMs,
            ((s.startMs - trip.startMs) / length).coerceIn(0f, 1f), ((s.endMs - trip.startMs) / length).coerceIn(0f, 1f),
        )
        return TripUi(
            id = trip.id, startMs = trip.startMs, endMs = trip.endMs, distanceKm = trip.distanceKm, source = trip.source,
            spans = spans.map(::spanUi),
            scenes = scenes.filter { it.status != SceneEntity.DENIED }.map { scene ->
                val kind = ClueKind.valueOf(scene.kind)
                SceneUi(
                    id = scene.id, kind = kind, question = kind.question, isOpenQuestion = kind.isOpenQuestion,
                    evidence = spans.filter { it.kind == scene.kind && it.startMs < scene.endMs && it.endMs > scene.startMs }.map(::spanUi),
                    confirmed = scene.status == SceneEntity.CONFIRMED, answer = scene.answer, answerSource = scene.answerSource,
                    listening = key == "scene-${scene.id}",
                )
            },
            memoQuestion = MEMO_QUESTION, memo = trip.memo, memoSource = trip.memoSource, memoListening = key == "trip-${trip.id}",
            canMergeWithPrevious = prev != null,
            mergeCandidateGapMin = prev?.takeIf { isMergeCandidate(it.endMs, trip.startMs) }?.let { (trip.startMs - it.endMs) / 60_000 },
        )
    }

    // ----- 동작 -----
    override fun replay(file: String) {
        replayJob?.cancel() // 재생 중 다시 누르면 처음부터
        replayJob = viewModelScope.launch { recorder.record(ReplaySource(csv(file), REPLAY_SPEED)) }
    }

    override fun confirmScene(sceneId: Long) = edit { dao.answerScene(sceneId, SceneEntity.CONFIRMED) }
    override fun denyScene(sceneId: Long) = edit { dao.answerScene(sceneId, SceneEntity.DENIED) }

    override fun answerSceneByVoice(sceneId: Long) {
        val scene = state.value.trips.flatMap { it.scenes }.firstOrNull { it.id == sceneId } ?: return
        ask("scene-$sceneId", scene.question) { dao.answerScene(sceneId, SceneEntity.CONFIRMED, it, stt.source.name) }
    }

    override fun recordMemoByVoice(tripId: Long) = ask("trip-$tripId", MEMO_QUESTION) { dao.setMemo(tripId, it, stt.source.name) }

    override fun mergeWithPrevious(tripId: Long) {
        val trips = state.value.trips
        val prev = trips.getOrNull(trips.indexOfFirst { it.id == tripId } + 1) ?: return
        edit { dao.mergeTrips(keepId = prev.id, dropId = tripId) }
    }

    override fun deleteTrip(tripId: Long) = edit { dao.deleteTrip(tripId) }
    override fun deleteAll() = edit { dao.deleteAll() }

    override fun setPaused(paused: Boolean) = setPrivacy { it.copy(paused = paused) }
    override fun setVoiceConsent(consent: Boolean) = setPrivacy { it.copy(voiceConsent = consent) }
    override fun setKindEnabled(kind: ClueKind, enabled: Boolean) =
        setPrivacy { it.copy(disabledKinds = if (enabled) it.disabledKinds - kind.name else it.disabledKinds + kind.name) }

    /** 모든 편집의 공통 관문(UC-09). 주행 제한 중이거나 상태를 모르면 아무것도 하지 않는다. */
    private fun edit(block: suspend () -> Unit) {
        if (!gate.inputAllowed.value) return
        viewModelScope.launch { block() }
    }

    private fun setPrivacy(change: (Privacy) -> Privacy) {
        if (!gate.inputAllowed.value) return
        privacy.update(change(privacy.state.value))
    }

    /** 질문을 읽어 주고(TTS) 한 번 듣는다(STT). 듣는 도중 주행 제한이 걸리면 결과를 버린다. */
    private fun ask(key: String, question: String, save: suspend (String) -> Unit) {
        if (!gate.inputAllowed.value || !privacy.state.value.voiceConsent || listening.value != null) return
        viewModelScope.launch {
            listening.value = key
            try {
                tts.speak(question)
                val text = stt.listen()
                if (text != null && gate.inputAllowed.value) save(text)
            } finally {
                listening.value = null
            }
        }
    }

    private fun csv(file: String) = getApplication<Application>().assets.open(file).bufferedReader().use { it.readText() }

    override fun onCleared() = gate.close()

    private companion object {
        const val REPLAY_SPEED = 600 // 2시간 40분 시나리오가 16초
        const val SPEECH_SCENARIO = "jecheon_daytrip.csv"
        const val MEMO_QUESTION = "이번 이동에서 기억에 남는 순간은?"
        val SCENARIO_TITLES = mapOf("jecheon_daytrip.csv" to "제천 당일치기", "errand_short_stop.csv" to "편의점 경유")
    }
}
