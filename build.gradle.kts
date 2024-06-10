@Suppress("DSL_SCOPE_VIOLATION")
plugins {
	alias(idofrontLibs.plugins.mia.kotlin.jvm)
	alias(idofrontLibs.plugins.kotlinx.serialization)
	alias(idofrontLibs.plugins.mia.papermc)
	alias(idofrontLibs.plugins.mia.copyjar)
	alias(idofrontLibs.plugins.mia.nms)
	alias(idofrontLibs.plugins.mia.publication)
	alias(idofrontLibs.plugins.mia.autoversion)
}

repositories {
	mavenCentral()
	maven("https://repo.mineinabyss.com/releases")
	maven("https://repo.mineinabyss.com/snapshots")
	maven("https://repo.dmulloy2.net/repository/public") // ProtocolLib
	maven("https://jitpack.io")
	maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
	mavenLocal()
}

tasks{
	build.get().dependsOn(publishToMavenLocal)
}

dependencies {
	// MineInAbyss platform
	compileOnly(idofrontLibs.bundles.idofront.core)
	compileOnly(idofrontLibs.idofront.nms)
	compileOnly(idofrontLibs.kotlinx.serialization.json)
	compileOnly(idofrontLibs.kotlinx.serialization.kaml)
	compileOnly(idofrontLibs.kotlinx.coroutines)
	compileOnly(idofrontLibs.minecraft.mccoroutine)

	compileOnly(libs.geary.papermc)
	compileOnly(libs.blocky)
	compileOnly(idofrontLibs.minecraft.plugin.protocollib)

	implementation(libs.minecraft.plugin.triumph.gui)
}
