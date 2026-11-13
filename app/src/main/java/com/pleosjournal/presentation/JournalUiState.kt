package com.pleosjournal.presentation

import com.pleosjournal.domain.ClueKind

/*
 * ===== View 계약 (ui/ 패키지와 나머지 사이의 유일한 접점) =====
 *
 * - View(ui/)는 [JournalUiState]를 받아 그리기만 하고, 사용자의 동작은 [JournalActions]로만 요청한다.
 * - View는 DB, SDK, 신호 소스, 음성 소스를 모른다. ui/ 에서 그쪽을 import 하면 ArchitectureTest가 실패한다.
 * - 기능을 화면에서 빼려면 ui/ 에서 그 부분을 그리지 않으면 된다. 다른 코드는 고치지 않는다.
 * - 새 기능이 필요하면 이 파일에 필드나 동작을 "추가"한다. 기존 필드의 의미는 바꾸지 않는다.
 * - 주행 제한(UC-09)과 음성 동의(UC-18)는 ViewModel이 한 번 더 막는다. View가 버튼을 잘못 열어도 동작은 실행되지 않는다.
 */

/** 화면 전체 상태. 값이 바뀌면 새 객체가 내려온다(불변). */
data class JournalUiState(
    /** 신호 입력 출처 배지: REAL / REPLAY / DUMMY */
    val inputSource: String = "REPLAY",
    /** 음성 인식 출처 배지 */
    val sttSource: String = "REPLAY",
    /** UC-09. false면 입력, 편집, 회고 UI를 열지 않는다. 상태를 모를 때도 false. */
    val inputAllowed: Boolean = false,
    /** 음성 입력 가능 여부 = 주행 제한 없음 + 음성 기록 동의. */
    val voiceAllowed: Boolean = false,
    /** 음성을 듣는 중인가. true인 동안에는 다른 음성 입력을 시작할 수 없다. */
    val voiceBusy: Boolean = false,
    /** 최초 실행 동의(UC-18)가 아직 안 됐는가. true면 동의 화면을 보여준다. 동의 전에는 아무것도 기록되지 않는다. */
    val needsOnboarding: Boolean = false,
    /** 실제 차량 신호(REAL)를 기록하는 중인가. */
    val recordingReal: Boolean = false,
    /** 기록기 상태 한 줄(디버그, 데모용). */
    val status: String = "",
    /** 재생할 수 있는 시나리오(디버그, 데모용). */
    val scenarios: List<ScenarioUi> = emptyList(),
    /** 최신순 트립 목록(UC-11). */
    val trips: List<TripUi> = emptyList(),
    val privacy: PrivacyUi = PrivacyUi(),
)

data class ScenarioUi(val file: String, val title: String)

data class TripUi(
    val id: Long,
    val startMs: Long,
    val endMs: Long,
    val distanceKm: Double,
    val source: String,
    /** 관측 사실 구간(UC-03). 해석이 아니다. */
    val spans: List<SpanUi>,
    /** 사용자가 거부하지 않은 장면 후보(UC-12). */
    val scenes: List<SceneUi>,
    /** 트립 전체에 대한 회고 질문과 답(UC-07b). */
    val memoQuestion: String,
    val memo: String?,
    val memoSource: String?,
    val memoListening: Boolean,
    /** 사용자가 거부해 숨긴 장면 후보 수. 0보다 크면 다시 보기를 제공할 수 있다. */
    val hiddenSceneCount: Int = 0,
    /** 트립 안의 정차 구간. 각 정차 지점에서 트립을 나눌 수 있다(UC-10). */
    val stops: List<StopUi> = emptyList(),
    /** 직전 트립이 있어 병합할 수 있는가(UC-10). */
    val canMergeWithPrevious: Boolean,
    /** 병합 후보라면 직전 트립과의 간격(분). 후보가 아니면 null. */
    val mergeCandidateGapMin: Long?,
)

data class StopUi(val id: Long, val startMs: Long, val endMs: Long)

data class SpanUi(
    val kind: ClueKind,
    /** 대상 위치 코드: DRIVER, FRONT_LEFT, REAR_RIGHT, FRONT, 또는 빈 문자열. */
    val target: String,
    val startMs: Long,
    val endMs: Long,
    /** 트립 전체를 0~1로 봤을 때의 시작, 끝 위치. 시간축 막대를 그릴 때 쓴다. */
    val startFraction: Float,
    val endFraction: Float,
)

data class SceneUi(
    val id: Long,
    val kind: ClueKind,
    /** 단정이 아닌 질문 문구. */
    val question: String,
    /** true면 예/아니오가 아니라 말로 답하는 질문. */
    val isOpenQuestion: Boolean,
    /** 이 질문의 근거가 된 관측 구간. 반드시 함께 보여준다. */
    val evidence: List<SpanUi>,
    val confirmed: Boolean,
    val answer: String?,
    val answerSource: String?,
    val listening: Boolean,
)

data class PrivacyUi(
    val paused: Boolean = false,
    val voiceConsent: Boolean = false,
    val kinds: List<KindToggleUi> = emptyList(),
)

data class KindToggleUi(val kind: ClueKind, val enabled: Boolean)

/** View가 요청할 수 있는 모든 동작. */
interface JournalActions {
    /** 시나리오 재생(디버그, 데모용). */
    fun replay(file: String)

    /** 실제 차량 신호(REAL) 기록을 시작한다. 재생 중이면 재생을 멈춘다. */
    fun recordReal()
    /** 재생 또는 실제 신호 기록을 멈춘다. */
    fun stopRecording()

    fun confirmScene(sceneId: Long)            // UC-12 "맞아요"
    fun denyScene(sceneId: Long)               // UC-12 "아니에요": 후보를 숨긴다. 관측 구간은 남는다.
    fun resetScene(sceneId: Long)              // UC-12 확인이나 답변을 취소하고 다시 묻는 상태로 되돌린다
    fun restoreHiddenScenes(tripId: Long)      // UC-12 거부해서 숨긴 후보를 다시 보이게 한다
    fun answerSceneByVoice(sceneId: Long)      // UC-07b
    fun recordMemoByVoice(tripId: Long)        // UC-07b

    fun mergeWithPrevious(tripId: Long)        // UC-10. splitAtStop으로 되돌릴 수 있다.
    fun splitAtStop(tripId: Long, stopId: Long) // UC-10. 정차 지점에서 둘로 나눈다. mergeWithPrevious로 되돌릴 수 있다.
    fun deleteTrip(tripId: Long)               // UC-10. 되돌릴 수 없다. 재확인은 View가 맡는다.

    /** UC-18 최초 실행 동의. 이후 기록이 시작된다. 음성 기록 동의는 따로 받아 함께 넘긴다. */
    fun completeOnboarding(voiceConsent: Boolean)
    fun setPaused(paused: Boolean)             // UC-18
    fun setVoiceConsent(consent: Boolean)      // UC-18
    fun setKindEnabled(kind: ClueKind, enabled: Boolean) // UC-18
    fun deleteAll()                            // UC-18. 되돌릴 수 없다.
}
