package com.jarvis.assistant.local

import com.jarvis.assistant.neural.BrainProfiles
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.CancellationException

class ModelRemovalPolicyTest {
    private val profile = BrainProfiles.lite
    private val data = ByteArray(1_000_019) { (it * 19).toByte() }
    private fun fixture(block: (File, File) -> Unit) {
        val base = Files.createTempDirectory("jarvis-model-removal").toFile()
        val root = File(base, "local-brain").apply { mkdirs() }
        try { block(root, base) } finally {
            // Do not follow links when cleaning adversarial filesystem fixtures.
            Files.walk(base.toPath()).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
        }
    }
    private fun install(root: File): Pair<File, File> {
        val model = File(root, profile.file).apply { writeBytes(data) }
        val manifest = File(root, "verified-model.json").apply { writeText(metadata().toString()) }
        return model to manifest
    }
    private fun metadata() = JSONObject().apply {
        put("repository", profile.repository); put("file", profile.file); put("revision", "a".repeat(40))
        put("sha256", MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) })
        put("size", data.size.toLong())
    }
    private fun invalid(block: () -> Unit) { assertTrue(runCatching(block).exceptionOrNull() is IllegalArgumentException) }

    @Test fun liteHasExactlyFiveOwnedNames() = fixture { root, _ ->
        assertEquals(listOf(profile.file, "${profile.file}.part", "${profile.file}.resume", "verified-model.json", "verified-model.json.part"), ModelRemovalPolicy.files(root, profile).map { it.name })
    }
    @Test fun balancedFilesStayInTheirOwnProfileDirectory() = fixture { root, _ ->
        assertTrue(ModelRemovalPolicy.files(root, BrainProfiles.balanced).all { it.parentFile == File(root, "balanced") })
        assertFalse(File(root, "balanced").exists())
    }
    @Test fun missingRootIsNotCreatedByInspection() = fixture { _, base ->
        val missing = File(base, "absent")
        assertEquals(0L, ModelRemovalPolicy.ownedStorageBytes(missing, profile)); assertFalse(missing.exists())
    }
    @Test fun foreignIdCannotBecomeADeletionTarget() = fixture { root, _ -> invalid { ModelRemovalPolicy.remove(root, profile.copy(id = "voice")) } }
    @Test fun modifiedRepositoryIsRejected() = fixture { root, _ -> invalid { ModelRemovalPolicy.files(root, profile.copy(repository = "someone/else")) } }
    @Test fun traversalFilenameIsRejectedBeforeAnyDeletion() = fixture { root, base ->
        val outside = File(base, "outside.gguf").apply { writeText("keep") }
        invalid { ModelRemovalPolicy.remove(root, profile.copy(file = "../outside.gguf")) }; assertEquals("keep", outside.readText())
    }
    @Test fun unavailableCatalogProfileCannotBeRemoved() = fixture { root, _ -> invalid { ModelRemovalPolicy.files(root, BrainProfiles.advanced) } }
    @Test fun alteredCatalogLimitsCannotAuthorizeTinyWeights() = fixture { root, _ -> invalid { ModelRemovalPolicy.verify(root, profile.copy(minimumBytes = 0)) } }
    @Test fun linkedRootIsRejectedAndTargetPreserved() = fixture { _, base ->
        val target = File(base, "target").apply { mkdirs() }; val keep = File(target, profile.file).apply { writeText("keep") }
        val link = File(base, "linked"); Files.createSymbolicLink(link.toPath(), target.toPath())
        invalid { ModelRemovalPolicy.remove(link, profile) }; assertEquals("keep", keep.readText())
    }
    @Test fun linkedProfileDirectoryIsRejected() = fixture { root, base ->
        val target = File(base, "voice-models").apply { mkdirs() }; val keep = File(target, BrainProfiles.balanced.file).apply { writeText("keep") }
        Files.createSymbolicLink(File(root, "balanced").toPath(), target.toPath())
        invalid { ModelRemovalPolicy.remove(root, BrainProfiles.balanced) }; assertEquals("keep", keep.readText())
    }
    @Test fun linkedWeightsAreRejectedBeforeManifestDeletion() = fixture { root, base ->
        val target = File(base, "voice.bin").apply { writeText("keep") }
        Files.createSymbolicLink(File(root, profile.file).toPath(), target.toPath())
        val manifest = File(root, "verified-model.json").apply { writeText("also keep") }
        invalid { ModelRemovalPolicy.remove(root, profile) }; assertEquals("keep", target.readText()); assertTrue(manifest.isFile)
    }
    @Test fun danglingManagedLinkIsRejected() = fixture { root, base ->
        Files.createSymbolicLink(File(root, "${profile.file}.part").toPath(), File(base, "missing").toPath())
        invalid { ModelRemovalPolicy.ownedStorageBytes(root, profile) }
    }
    @Test fun trustedPlatformAncestorAliasUsesCanonicalContainment() = fixture { root, base ->
        val alias = File(base, "alias"); Files.createSymbolicLink(alias.toPath(), root.toPath())
        val files = ModelRemovalPolicy.files(File(alias, "child"), profile)
        assertTrue(files.all { it.parentFile == File(root.canonicalFile, "child") })
        assertFalse(File(root, "child").exists())
    }
    @Test fun directoryAtManagedFilenameIsNeverRecursivelyDeleted() = fixture { root, _ ->
        val directory = File(root, profile.file).apply { mkdirs() }; val keep = File(directory, "user.txt").apply { writeText("keep") }
        invalid { ModelRemovalPolicy.remove(root, profile) }; assertEquals("keep", keep.readText())
    }
    @Test fun nondirectoryRootIsRejected() = fixture { _, base ->
        val root = File(base, "model-file").apply { writeText("keep") }
        invalid { ModelRemovalPolicy.files(root, profile) }; assertEquals("keep", root.readText())
    }
    @Test fun storageIncludesPartialAndMetadataBytesOnly() = fixture { root, _ ->
        File(root, profile.file).writeBytes(ByteArray(23)); File(root, "${profile.file}.part").writeBytes(ByteArray(17)); File(root, "verified-model.json").writeBytes(ByteArray(11))
        File(root, "benchmark-last.json").writeBytes(ByteArray(99))
        assertEquals(51L, ModelRemovalPolicy.ownedStorageBytes(root, profile))
    }
    @Test fun removalDeletesOnlyCatalogFilesAndReturnsTheirBytes() = fixture { root, _ ->
        ModelRemovalPolicy.files(root, profile).forEachIndexed { i, file -> file.writeBytes(ByteArray(i + 1)) }
        assertEquals(15L, ModelRemovalPolicy.remove(root, profile)); assertEquals(0L, ModelRemovalPolicy.ownedStorageBytes(root, profile)); assertTrue(root.isDirectory)
    }
    @Test fun removalPreservesVoiceAssetsUserFilesAndBenchmarks() = fixture { root, base ->
        val keep = listOf(File(root, "benchmark-last.json"), File(root, "user-document.txt"), File(base, "vosk/model.bin"), File(base, "sherpa/voice.onnx"), File(base, "credentials/account.json"), File(base, "databases/history.db"))
        keep.forEach { it.parentFile!!.mkdirs(); it.writeText("keep") }; install(root)
        ModelRemovalPolicy.remove(root, profile); keep.forEach { assertEquals(it.path, "keep", it.readText()) }
    }
    @Test fun removingLitePreservesOtherBrainFiles() = fixture { root, _ ->
        val other = File(root, "balanced/${BrainProfiles.balanced.file}").apply { parentFile!!.mkdirs(); writeText("keep") }; install(root)
        ModelRemovalPolicy.remove(root, profile); assertEquals("keep", other.readText())
    }
    @Test fun removingAbsentProfileIsIdempotent() = fixture { root, _ -> assertEquals(0L, ModelRemovalPolicy.remove(root, profile)); assertEquals(0L, ModelRemovalPolicy.remove(root, profile)) }
    @Test fun absentModelAndManifestAreNotInstalled() = fixture { root, _ -> assertEquals(ModelRemovalPolicy.IntegrityState.NOT_INSTALLED, ModelRemovalPolicy.verify(root, profile).state) }
    @Test fun aPartialDownloadIsNeverVerifiedAsInstalled() = fixture { root, _ ->
        File(root, "${profile.file}.part").writeBytes(data)
        assertEquals(ModelRemovalPolicy.IntegrityState.NOT_INSTALLED, ModelRemovalPolicy.verify(root, profile).state)
    }
    @Test fun realBytesMatchingManifestPassStreamingIntegrity() = fixture { root, _ ->
        install(root); val result = ModelRemovalPolicy.verify(root, profile)
        assertTrue(result.verified); assertEquals(data.size.toLong(), result.bytes); assertTrue(result.message.contains("inference has not been tested"))
    }
    @Test fun sameSizeCorruptionCannotPassIntegrity() = fixture { root, _ ->
        val (model, _) = install(root); val corrupt = data.clone(); corrupt[corrupt.lastIndex] = (corrupt.last() + 1).toByte(); model.writeBytes(corrupt)
        assertEquals(ModelRemovalPolicy.IntegrityState.REPAIR_REQUIRED, ModelRemovalPolicy.verify(root, profile).state)
    }
    @Test fun missingManifestRequiresRepairWithoutDeletingWeights() = fixture { root, _ ->
        val (model, manifest) = install(root); manifest.delete()
        assertEquals(ModelRemovalPolicy.IntegrityState.REPAIR_REQUIRED, ModelRemovalPolicy.verify(root, profile).state); assertTrue(model.isFile)
    }
    @Test fun orphanedManifestRequiresRepair() = fixture { root, _ ->
        val (model, _) = install(root); model.delete(); assertEquals(ModelRemovalPolicy.IntegrityState.REPAIR_REQUIRED, ModelRemovalPolicy.verify(root, profile).state)
    }
    @Test fun malformedManifestRequiresRepair() = fixture { root, _ ->
        val (_, manifest) = install(root); manifest.writeText("not JSON"); assertFalse(ModelRemovalPolicy.verify(root, profile).verified)
    }
    @Test fun oversizedManifestIsRejectedBeforeParsing() = fixture { root, _ ->
        val (_, manifest) = install(root); manifest.writeBytes(ByteArray(16_385)); assertFalse(ModelRemovalPolicy.verify(root, profile).verified)
    }
    @Test fun wrongRepositoryCannotVerifyMatchingBytes() = fixture { root, _ ->
        install(root); File(root, "verified-model.json").writeText(metadata().put("repository", "other/model").toString()); assertFalse(ModelRemovalPolicy.verify(root, profile).verified)
    }
    @Test fun wrongFilenameCannotVerifyMatchingBytes() = fixture { root, _ ->
        install(root); File(root, "verified-model.json").writeText(metadata().put("file", "voice.onnx").toString()); assertFalse(ModelRemovalPolicy.verify(root, profile).verified)
    }
    @Test fun malformedRevisionAndDigestCannotVerify() = fixture { root, _ ->
        install(root); val manifest = File(root, "verified-model.json")
        manifest.writeText(metadata().put("revision", "main").toString()); assertFalse(ModelRemovalPolicy.verify(root, profile).verified)
        manifest.writeText(metadata().put("sha256", "F".repeat(64)).toString()); assertFalse(ModelRemovalPolicy.verify(root, profile).verified)
    }
    @Test fun manifestSizeMustMatchActualWeightsAndCatalogRange() = fixture { root, _ ->
        install(root); val manifest = File(root, "verified-model.json")
        manifest.writeText(metadata().put("size", -1).toString()); assertFalse(ModelRemovalPolicy.verify(root, profile).verified)
        manifest.writeText(metadata().put("size", data.size + 1).toString()); assertFalse(ModelRemovalPolicy.verify(root, profile).verified)
    }
    @Test fun cancellationBeforeVerificationDoesNotDeleteOrClaimCorruption() = fixture { root, _ ->
        val (model, manifest) = install(root)
        assertTrue(runCatching { ModelRemovalPolicy.verify(root, profile) { true } }.exceptionOrNull() is CancellationException)
        assertTrue(model.isFile && manifest.isFile)
    }
    @Test fun cancellationDuringStreamingLeavesInstallationUntouched() = fixture { root, _ ->
        val (model, manifest) = install(root); var checks = 0
        assertTrue(runCatching { ModelRemovalPolicy.verify(root, profile) { ++checks >= 4 } }.exceptionOrNull() is CancellationException)
        assertArrayEquals(data, model.readBytes()); assertTrue(manifest.isFile)
    }
}
