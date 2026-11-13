package com.pleosjournal.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** UC-18 프라이버시 설정. 기본값은 "최초 동의 전에는 기록 안 함, 음성은 따로 동의하기 전까지 꺼짐". */
data class Privacy(
    val onboarded: Boolean = false,               // 최초 실행 동의를 마쳤는가. 마치기 전에는 아무것도 기록하지 않는다
    val paused: Boolean = false,                  // 기록 일시정지: 켜면 어떤 이벤트도 기록하지 않는다
    val voiceConsent: Boolean = false,            // 동승자 포함 음성 기록 동의. 꺼져 있으면 음성 입력을 열지 않는다
    val disabledKinds: Set<String> = emptySet(),  // 수집을 끈 단서 종류(ClueKind 이름)
)

/** SharedPreferences(Android 표준 키-값 저장소)에 기기 로컬로만 저장한다. */
class PrivacyStore(context: Context) {
    private val prefs = context.getSharedPreferences("privacy", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        Privacy(
            onboarded = prefs.getBoolean("onboarded", false),
            paused = prefs.getBoolean("paused", false),
            voiceConsent = prefs.getBoolean("voiceConsent", false),
            disabledKinds = prefs.getStringSet("disabledKinds", emptySet())!!.toSet(),
        ),
    )
    val state: StateFlow<Privacy> = _state

    fun update(p: Privacy) {
        prefs.edit()
            .putBoolean("onboarded", p.onboarded)
            .putBoolean("paused", p.paused)
            .putBoolean("voiceConsent", p.voiceConsent)
            .putStringSet("disabledKinds", p.disabledKinds)
            .apply()
        _state.value = p
    }
}
