package com.jarvis.assistant.local

import com.jarvis.assistant.neural.BrainProfile
import com.jarvis.assistant.neural.BrainProfiles
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.CancellationException

/** Filesystem operations for catalog-owned LLM downloads only.
 * The caller must stop/join inference and downloads and hold its management locks. */
object ModelRemovalPolicy {
    enum class IntegrityState { VERIFIED, NOT_INSTALLED, REPAIR_REQUIRED }
    data class IntegrityResult(val state: IntegrityState, val bytes: Long, val message: String) {
        val verified: Boolean get() = state == IntegrityState.VERIFIED
    }

    /** Never creates directories, follows links, or accepts caller-supplied filenames. */
    fun files(root: File, profile: BrainProfile): List<File> {
        require(BrainProfiles.all.any { it.available && it == profile }) { "Unknown managed brain profile." }
        require(!Files.isSymbolicLink(root.toPath())) { "Linked model storage is not managed storage." }
        // Android may expose private filesDir through an OS-owned ancestor alias.
        // The managed root itself must not be a link; containment uses its real path.
        val absoluteRoot = root.canonicalFile
        require(!root.exists() || root.isDirectory) { "Model storage must be a directory." }
        val directory = if (profile.id == BrainProfiles.lite.id) absoluteRoot else File(absoluteRoot, profile.id)
        require(!Files.isSymbolicLink(directory.toPath())) { "Linked model directories are not managed storage." }
        require(directory.canonicalFile.toPath().startsWith(absoluteRoot.toPath())) { "Model directory escapes managed storage." }
        require(!directory.exists() || directory.isDirectory) { "Model profile storage must be a directory." }
        val names = listOf(profile.file, "${profile.file}.part", "${profile.file}.resume", "verified-model.json", "verified-model.json.part")
        return names.map { name ->
            val file = File(directory, name)
            require(!Files.isSymbolicLink(file.toPath())) { "Linked model files are not managed files." }
            require(file.canonicalFile.parentFile == directory.canonicalFile) { "Model file escapes its managed directory." }
            require(!file.exists() || file.isFile) { "A managed model filename is occupied by a non-file." }
            file
        }
    }

    /** Includes resumable bytes and integrity records; excludes all unrelated files. */
    fun ownedStorageBytes(root: File, profile: BrainProfile): Long = files(root, profile)
        .fold(0L) { total, file -> Math.addExact(total, if (file.isFile) file.length() else 0L) }

    /** Returns bytes actually removed. Validation happens before the first deletion. */
    fun remove(root: File, profile: BrainProfile): Long {
        val owned = files(root, profile)
        var removed = 0L
        for (file in owned) {
            // Recheck paths in case storage changed between managed operations.
            files(root, profile)
            if (file.exists()) {
                val bytes = file.length()
                check(file.delete()) { "Cannot remove managed model file: ${file.name}." }
                removed = Math.addExact(removed, bytes)
            }
        }
        return removed
    }

    /** Rehashes actual weights; a plausible manifest and matching size are insufficient. */
    fun verify(root: File, profile: BrainProfile, cancelled: () -> Boolean = { false }): IntegrityResult {
        val owned = files(root, profile)
        val model = owned[0]
        val manifest = owned[3]
        fun stopped() { if (cancelled()) throw CancellationException("Model integrity check cancelled.") }
        fun repair(message: String) = IntegrityResult(IntegrityState.REPAIR_REQUIRED, model.length(), message)
        stopped()
        if (!model.isFile && !manifest.isFile) return IntegrityResult(IntegrityState.NOT_INSTALLED, 0L, "Local Brain is not installed. Downloaded partial files may be resumed or removed.")
        if (!model.isFile) return repair("The managed model file is missing. Repair this installation.")
        if (!manifest.isFile) return repair("The model integrity record is missing. Repair this installation.")
        if (manifest.length() !in 1..MAX_MANIFEST_BYTES) return repair("The model integrity record has an invalid size.")
        val metadata = try {
            val bytes = java.io.ByteArrayOutputStream()
            manifest.inputStream().use { input ->
                val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(bytes.size() + count <= MAX_MANIFEST_BYTES) { "Model manifest is too large." }
                    bytes.write(buffer, 0, count)
                }
            }
            JSONObject(bytes.toString(Charsets.UTF_8.name()))
        } catch (_: Exception) {
            return repair("The model integrity record is invalid.")
        }
        val expected = try {
            require(metadata.getString("repository") == profile.repository)
            require(metadata.getString("file") == profile.file)
            require(metadata.getString("revision").matches(Regex("[a-f0-9]{40}")))
            val hash = metadata.getString("sha256")
            require(hash.matches(Regex("[a-f0-9]{64}")))
            val bytes = metadata.getLong("size")
            require(bytes in profile.minimumBytes..profile.maximumBytes && bytes == model.length())
            hash to bytes
        } catch (_: Exception) { return repair("The model integrity record does not match this managed profile.") }
        val digest = MessageDigest.getInstance("SHA-256")
        val modified = model.lastModified()
        var read = 0L
        try {
            model.inputStream().use { input ->
                val buffer = ByteArray(128 * 1024)
                while (true) {
                    stopped()
                    val count = input.read(buffer)
                    if (count < 0) break
                    read = Math.addExact(read, count.toLong())
                    if (read > expected.second) return repair("The model changed during verification. Check it again.")
                    digest.update(buffer, 0, count)
                }
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: java.io.IOException) { return repair("The managed model could not be read. Repair this installation.") }
        stopped()
        files(root, profile)
        if (read != expected.second || model.length() != expected.second || model.lastModified() != modified)
            return repair("The model changed during verification. Check it again.")
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (actual != expected.first) return repair("Model integrity check failed. Repair this installation.")
        return IntegrityResult(IntegrityState.VERIFIED, read, "Managed model bytes match the recorded SHA-256. Native inference has not been tested by this check.")
    }

    private const val MAX_MANIFEST_BYTES = 16_384L
}
