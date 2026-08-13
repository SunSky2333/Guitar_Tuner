plugins {
    kotlin("jvm")
}

group = "com.example.guitartuner"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // 沙箱文件保护会持有旧 index.html，禁用 HTML 报告规避覆盖失败（XML 报告仍保留）
    reports {
        html.required.set(false)
    }
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
    }
}

// M1 验收明细报告：./gradlew :dsp:verifyReport
tasks.register<JavaExec>("verifyReport") {
    group = "verification"
    description = "输出 6 套调弦 30 个音的 YIN 检测误差明细（文档 §12.1 Kotlin 移植）"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.example.guitartuner.VerifyReportKt")
}
