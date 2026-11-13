package com.pleosjournal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * 정적 구조의 의존 규칙을 검사한다. 규칙을 어기는 import가 생기면 실패한다.
 *
 *   ui  →  presentation(상태, 동작 계약)  →  domain / data / signal / speech / platform
 *
 * - ui 는 화면만 안다. 저장, 신호, 음성, SDK를 직접 만지지 않는다.
 * - domain 은 순수 Kotlin이다. Android, Room, SDK, 화면을 모른다.
 */
class ArchitectureTest {
    private val root = File("src/main/java/com/pleosjournal") // Gradle은 단위 테스트를 모듈 폴더(app/)에서 실행한다.

    private fun violations(pkg: String, forbidden: List<String>): List<String> =
        root.resolve(pkg).walk().filter { it.extension == "kt" }.flatMap { file ->
            file.readLines().filter { line -> line.startsWith("import ") && forbidden.any { line.removePrefix("import ").startsWith(it) } }
                .map { "${file.name}: $it" }
        }.toList()

    @Test fun `ui는 presentation과 domain 모델만 안다`() {
        val forbidden = listOf(
            "com.pleosjournal.data", "com.pleosjournal.signal", "com.pleosjournal.speech", "com.pleosjournal.platform",
            "androidx.room", "android.car", "ai.pleos", "androidx.lifecycle",
        )
        assertEquals(emptyList<String>(), violations("ui", forbidden))
    }

    @Test fun `domain은 순수 Kotlin이다`() {
        val forbidden = listOf(
            "android.", "androidx.", "ai.pleos",
            "com.pleosjournal.data", "com.pleosjournal.ui", "com.pleosjournal.presentation", "com.pleosjournal.platform", "com.pleosjournal.speech",
        )
        assertEquals(emptyList<String>(), violations("domain", forbidden))
    }

    @Test fun `SDK 타입은 signal, speech, platform 밖으로 나오지 않는다`() {
        val forbidden = listOf("ai.pleos", "android.car")
        for (pkg in listOf("ui", "presentation", "domain", "data")) assertEquals(emptyList<String>(), violations(pkg, forbidden))
    }
}
