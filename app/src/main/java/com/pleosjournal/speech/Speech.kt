package com.pleosjournal.speech

import android.util.Log
import com.pleosjournal.signal.Source
import kotlinx.coroutines.delay

/**
 * 음성 입출력 인터페이스(UC-07b). Pleos STT/TTS SDK는 문서상 "준비 중"이라 실제 구현은 아직 없다.
 * TODO(확인 필요): Pleos SpeechToText, TextToSpeech의 의존성 좌표와 API가 공개되면 PleosStt, PleosTts를 추가한다.
 *   추측으로 쓰지 않는다. 출처: https://document.pleos.ai/docs/pleos-only/create-my-project
 */
interface SpeechToText {
    val source: Source
    /** 한 번 듣고 인식된 문장을 돌려준다. 인식 실패나 취소는 null. */
    suspend fun listen(): String?
}

interface TextToSpeech {
    val source: Source
    fun speak(text: String)
}

/** 시나리오의 SPEECH 행을 순서대로 "인식 결과"로 돌려주는 가짜 STT. 화면에는 REPLAY 배지로 표시한다. */
class FakeStt(private val lines: List<String>) : SpeechToText {
    override val source = Source.REPLAY
    private var next = 0

    override suspend fun listen(): String? {
        delay(1500) // 듣는 중 상태를 화면에서 볼 수 있게
        return lines.getOrNull(next++ % lines.size.coerceAtLeast(1))
    }
}

/** 소리를 내지 않고 로그만 남기는 가짜 TTS. 질문 문구는 화면에도 항상 표시된다. */
class FakeTts : TextToSpeech {
    override val source = Source.DUMMY
    override fun speak(text: String) {
        Log.i("FakeTts", text)
    }
}
