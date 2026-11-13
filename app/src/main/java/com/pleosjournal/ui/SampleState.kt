package com.pleosjournal.ui

import com.pleosjournal.domain.ClueKind
import com.pleosjournal.presentation.JournalActions
import com.pleosjournal.presentation.JournalUiState
import com.pleosjournal.presentation.KindToggleUi
import com.pleosjournal.presentation.PrivacyUi
import com.pleosjournal.presentation.SceneUi
import com.pleosjournal.presentation.ScenarioUi
import com.pleosjournal.presentation.SpanUi
import com.pleosjournal.presentation.StopUi
import com.pleosjournal.presentation.TripUi

/** 미리보기와 화면 작업용 가짜 상태. 값은 모두 가상이다. 화면 담당자가 경우를 늘려 가며 쓴다. */

private const val T0 = 1_794_000_000_000L // 임의의 기준 시각
private const val MIN = 60_000L

private fun span(kind: ClueKind, target: String, fromMin: Long, toMin: Long, tripMin: Long) =
    SpanUi(kind, target, T0 + fromMin * MIN, T0 + toMin * MIN, fromMin / tripMin.toFloat(), toMin / tripMin.toFloat())

private val windows = listOf(span(ClueKind.WINDOW_OPEN, "FRONT_LEFT", 15, 25, 155), span(ClueKind.WINDOW_OPEN, "FRONT_RIGHT", 15, 25, 155))
private val wiper = listOf(span(ClueKind.WIPER, "FRONT", 40, 70, 155))
private val recline = listOf(span(ClueKind.REAR_RECLINE, "REAR_LEFT", 75, 145, 155))

val sampleState = JournalUiState(
    inputAllowed = true,
    voiceAllowed = true,
    status = "재생 완료 (이벤트 29개)",
    scenarios = listOf(ScenarioUi("a.csv", "제천 당일치기"), ScenarioUi("b.csv", "편의점 경유")),
    trips = listOf(
        TripUi(
            id = 2, startMs = T0, endMs = T0 + 155 * MIN, distanceKm = 148.0, source = "REPLAY",
            spans = windows + wiper + recline,
            scenes = listOf(
                SceneUi(1, ClueKind.WINDOW_OPEN, "창문 열고 달린 구간이 있었어요. 어떤 길이었나요?", true, windows, false, null, null, false),
                SceneUi(2, ClueKind.WIPER, "이때 비가 왔나요?", false, wiper, true, null, null, false),
                SceneUi(3, ClueKind.REAR_RECLINE, "뒷좌석에서 쉬던 시간이었나요?", false, recline, false, null, null, false),
            ),
            memoQuestion = "이번 이동에서 기억에 남는 순간은?",
            memo = "오늘 도담삼봉에서 먹은 점심이 제일 기억에 남아.", memoSource = "REPLAY", memoListening = false,
            hiddenSceneCount = 1, stops = listOf(StopUi(1, T0 + 100 * MIN, T0 + 110 * MIN)),
            canMergeWithPrevious = true, mergeCandidateGapMin = 6,
        ),
        TripUi(
            id = 1, startMs = T0 - 20 * MIN, endMs = T0 - 6 * MIN, distanceKm = 6.5, source = "REPLAY",
            spans = emptyList(), scenes = emptyList(),
            memoQuestion = "이번 이동에서 기억에 남는 순간은?", memo = null, memoSource = null, memoListening = false,
            canMergeWithPrevious = false, mergeCandidateGapMin = null,
        ),
    ),
    privacy = PrivacyUi(paused = false, voiceConsent = true, kinds = ClueKind.entries.map { KindToggleUi(it, true) }),
)

/** 아무 일도 하지 않는 동작. 미리보기용. */
object NoActions : JournalActions {
    override fun replay(file: String) = Unit
    override fun recordReal() = Unit
    override fun stopRecording() = Unit
    override fun resetScene(sceneId: Long) = Unit
    override fun restoreHiddenScenes(tripId: Long) = Unit
    override fun splitAtStop(tripId: Long, stopId: Long) = Unit
    override fun completeOnboarding(voiceConsent: Boolean) = Unit
    override fun confirmScene(sceneId: Long) = Unit
    override fun denyScene(sceneId: Long) = Unit
    override fun answerSceneByVoice(sceneId: Long) = Unit
    override fun recordMemoByVoice(tripId: Long) = Unit
    override fun mergeWithPrevious(tripId: Long) = Unit
    override fun deleteTrip(tripId: Long) = Unit
    override fun setPaused(paused: Boolean) = Unit
    override fun setVoiceConsent(consent: Boolean) = Unit
    override fun setKindEnabled(kind: ClueKind, enabled: Boolean) = Unit
    override fun deleteAll() = Unit
}
