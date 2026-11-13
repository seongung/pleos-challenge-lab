plugins {
    id("com.android.application") // AGP 9는 Kotlin 지원 내장 (kotlin-android 플러그인 불필요)
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.pleosjournal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pleosjournal"
        minSdk = 28 // 공모전 요구사항: API 28 이상
        targetSdk = 35 // 공식 샘플 앱과 동일
        versionCode = 1
        versionName = "0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    // CarUxRestrictions(UC-09)용 Android 표준 차량 API. SDK에 들어 있는 선택 라이브러리라 새 다운로드는 없다.
    useLibrary("android.car")
    // 저장소 루트의 scenarios/ 를 그대로 앱 assets로 넣는다(복사본을 두지 않기 위함).
    sourceSets["main"].assets.srcDir("../scenarios")
}

dependencies {
    // 출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup
    implementation("ai.pleos.playground:Vehicle:2.3.2")

    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Room: 로컬 저장(네트워크 없음). ksp는 @Dao, @Entity에서 SQL 코드를 컴파일 시점에 생성한다.
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    testImplementation("junit:junit:4.13.2")
}
