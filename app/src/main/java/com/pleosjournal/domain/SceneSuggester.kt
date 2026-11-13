package com.pleosjournal.domain

/**
 * 장면 후보(UC-12). 관측 구간을 근거로 "질문"을 만든다. 단정하지 않는다.
 * 흐름: 관측 사실 → 단서 제안(여기) → 사용자 확인 → 저장.
 */
data class SceneCandidate(
    val kind: ClueKind,
    val startMs: Long,
    val endMs: Long,
    val evidence: List<ObservedSpan>, // 이 후보를 만든 관측 구간. 화면에 근거로 그대로 보여준다.
) {
    val question get() = kind.question
}

/** 질문 문구는 docs/04_usecases.md 단서 규칙표를 따른다. */
val ClueKind.question: String
    get() = when (this) {
        ClueKind.WIPER -> "이때 비가 왔나요?"
        ClueKind.HEADLAMP -> "어두워진 뒤 달린 길이었나요?"
        ClueKind.WINDOW_OPEN -> "창문 열고 달린 구간이 있었어요. 어떤 길이었나요?"
        ClueKind.REAR_RECLINE -> "뒷좌석에서 쉬던 시간이었나요?"
        ClueKind.DOOR_OPEN -> "여기서 무엇을 했나요?"
    }

/** 예/아니오로 끝나지 않고 말로 답해야 하는 질문인가. */
val ClueKind.isOpenQuestion get() = this == ClueKind.WINDOW_OPEN || this == ClueKind.DOOR_OPEN

/** 같은 종류이고 시간이 겹치는 구간(예: 앞 왼쪽, 앞 오른쪽 창문)을 하나의 장면 후보로 묶는다. */
fun suggestScenes(spans: List<ObservedSpan>): List<SceneCandidate> {
    val scenes = mutableListOf<SceneCandidate>()
    for (span in spans.sortedBy { it.startMs }) {
        val i = scenes.indexOfLast { it.kind == span.kind && span.startMs <= it.endMs }
        if (i >= 0) {
            val s = scenes[i]
            scenes[i] = s.copy(endMs = maxOf(s.endMs, span.endMs), evidence = s.evidence + span)
        } else {
            scenes += SceneCandidate(span.kind, span.startMs, span.endMs, listOf(span))
        }
    }
    return scenes.sortedBy { it.startMs }
}
