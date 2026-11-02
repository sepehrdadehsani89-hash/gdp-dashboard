package com.jarvis.assistant.local

import org.json.JSONObject

/** Only metadata for the fixed official repository can authorize this optional weight download. */
object ModelDownloadPolicy {
    data class Descriptor(val revision: String, val sha256: String, val size: Long)
    private val revisionPattern = Regex("[a-f0-9]{40}")
    private val checksumPattern = Regex("[a-f0-9]{64}")

    fun parseOfficialMetadata(text: String, profile: com.jarvis.assistant.neural.BrainProfile = com.jarvis.assistant.neural.BrainProfiles.lite): Descriptor {
        val metadata = JSONObject(text)
        require(metadata.getString("id") == profile.repository) { "Unexpected model repository." }
        require(metadata.optJSONObject("cardData")?.optString("license")?.lowercase() == "apache-2.0") {
            "The official model license could not be verified. Download was not started."
        }
        val revision = metadata.getString("sha")
        require(revision.matches(revisionPattern)) { "Missing immutable model revision." }
        val siblings = metadata.getJSONArray("siblings")
        val entry = (0 until siblings.length()).asSequence().map { siblings.getJSONObject(it) }
            .firstOrNull { it.optString("rfilename") == profile.file }
        val lfs = entry?.optJSONObject("lfs")
            ?: throw IllegalArgumentException("Model integrity metadata is unavailable.")
        val hash = lfs.optString("sha256").lowercase()
        val size = lfs.optLong("size", -1)
        require(hash.matches(checksumPattern)) { "Missing model SHA-256." }
        require(size in profile.minimumBytes..profile.maximumBytes) { "Unsupported model download size. No weights were installed." }
        return Descriptor(revision, hash, size)
    }

    fun verifyDownloaded(descriptor: Descriptor, actualSize: Long, actualSha256: String) {
        require(actualSize == descriptor.size) { "Incomplete model download. Please retry." }
        require(actualSha256 == descriptor.sha256) { "Model checksum failed. The file was rejected." }
    }
}
