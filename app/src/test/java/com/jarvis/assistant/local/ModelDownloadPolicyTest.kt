package com.jarvis.assistant.local

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ModelDownloadPolicyTest {
    private fun metadata(): JSONObject = JSONObject("""{
        "id":"Qwen/Qwen2.5-0.5B-Instruct-GGUF",
        "sha":"0123456789abcdef0123456789abcdef01234567",
        "cardData":{"license":"apache-2.0"},
        "siblings":[{"rfilename":"qwen2.5-0.5b-instruct-q4_k_m.gguf","lfs":{"sha256":"${"a".repeat(64)}","size":400000000}}]
    }""")

    @Test fun `official metadata pins an immutable revision and expected bytes`() {
        val result = ModelDownloadPolicy.parseOfficialMetadata(metadata().toString())
        assertEquals(400_000_000L, result.size)
        assertEquals("a".repeat(64), result.sha256)
        assertEquals(40, result.revision.length)
        ModelDownloadPolicy.verifyDownloaded(result, result.size, result.sha256)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `different model repository cannot authorize download`() {
        ModelDownloadPolicy.parseOfficialMetadata(metadata().put("id", "untrusted/replacement").toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `mutable branch name cannot authorize a weight URL`() {
        ModelDownloadPolicy.parseOfficialMetadata(metadata().put("sha", "main").toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing checksum blocks installation instead of disabling verification`() {
        val invalid = metadata()
        invalid.getJSONArray("siblings").getJSONObject(0).getJSONObject("lfs").remove("sha256")
        ModelDownloadPolicy.parseOfficialMetadata(invalid.toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an unverified license blocks the download`() {
        val invalid = metadata().put("cardData", JSONObject().put("license", "unknown"))
        ModelDownloadPolicy.parseOfficialMetadata(invalid.toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `truncated download cannot be installed even with a matching claimed hash`() {
        val descriptor = ModelDownloadPolicy.parseOfficialMetadata(metadata().toString())
        ModelDownloadPolicy.verifyDownloaded(descriptor, descriptor.size - 1, descriptor.sha256)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `modified weights cannot be installed despite correct file length`() {
        val descriptor = ModelDownloadPolicy.parseOfficialMetadata(metadata().toString())
        ModelDownloadPolicy.verifyDownloaded(descriptor, descriptor.size, "b".repeat(64))
    }
}
