import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.20"
    id("com.gradleup.shadow") version "9.6.1"
    `maven-publish`
}

group = "com.awabi2048"
version = "26.930.6"

repositories {
    mavenLocal()
    maven { url = uri("../.m2-paper26-kotlin2320") }
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.nexomc.com/releases/")
    maven("https://repo.dmulloy2.net/repository/public/")
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

// pom.xml の provided スコープ相当（compileOnly へ配置し、テストのコンパイル・実行双方で見えるよう testImplementation にも追加）
val providedDeps = listOf(
    "io.papermc.paper:paper-api:26.2.build.129-stable",
    // bungeecord-chat等が同梱する旧gsonを上書き解決するため明示宣言（Mavenでは宣言順で先行解決されていた）
    "com.google.code.gson:gson:2.13.2",
    "org.jetbrains.kotlin:kotlin-stdlib:2.3.20",
    "net.luckperms:api:5.4",
    "com.sk89q.worldedit:worldedit-bukkit:7.3.16",
    "com.sk89q.worldguard:worldguard-bukkit:7.0.14",
    "com.griefcraft:lwc:2.4.2",
    "org.geysermc.cumulus:cumulus:2.0.0-SNAPSHOT",
    "com.moulberry.axiom:AxiomPaper:5.0.1",
    "net.dmulloy2:ProtocolLib:5.4.0",
)

dependencies {
    providedDeps.forEach {
        compileOnly(it)
        testImplementation(it)
    }
    // geyser:common が旧gsonを同梱しクラスパス上で先行解決されるため、推移依存から除外（ソースでは floodgate-api/cumulus のみ使用）
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT") {
        exclude(group = "org.geysermc.geyser", module = "common")
    }
    testImplementation("org.geysermc.floodgate:api:2.2.5-SNAPSHOT") {
        exclude(group = "org.geysermc.geyser", module = "common")
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.test {
    useJUnitPlatform()
}

// pom.xml の resources filtering 相当（${project.version} を展開）
tasks.processResources {
    val versionString = project.version.toString()
    inputs.property("projectVersion", versionString)
    filesMatching("plugin.yml") {
        expand(mapOf("project" to mapOf("version" to versionString)))
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
}

publishing {
    publications {
        create<MavenPublication>("plugin") {
            artifactId = "CC-System"
            from(components["shadow"])
        }
    }
    repositories {
        maven {
            name = "workspace"
            url = uri("../.m2-paper26-kotlin2320")
        }
    }
}
