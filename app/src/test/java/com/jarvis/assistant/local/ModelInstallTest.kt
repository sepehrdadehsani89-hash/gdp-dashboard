package com.jarvis.assistant.local

import com.jarvis.assistant.neural.BrainProfiles
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.Assert.*
import java.io.*
import java.nio.file.Files
import java.security.MessageDigest

class ModelInstallTest {
    // Small deterministic test bytes, never shipped or represented as model weights.
    private val bytes=ByteArray(1_200_031){(it*31).toByte()}
    private fun descriptor(data:ByteArray=bytes)=ModelDownloadPolicy.Descriptor("a".repeat(40),MessageDigest.getInstance("SHA-256").digest(data).joinToString(""){"%02x".format(it)},data.size.toLong())
    private fun file(block:suspend(File)->Unit)=runBlocking{val dir=Files.createTempDirectory("jarvis-model-transfer").toFile();try{block(File(dir,"fixture.part"))}finally{dir.deleteRecursively()}}
    @Test fun realStreamWritesEveryByteVerifiesHashAndLeavesFinalProgressToInstaller()=file{part->val progress=mutableListOf<Float>();assertEquals(descriptor().sha256,ModelFileTransfer.copyVerified(bytes.inputStream(),part,descriptor(),0,{progress+=it}));assertArrayEquals(bytes,part.readBytes());assertTrue(progress.isNotEmpty());assertTrue(progress.all{it<1f})}
    @Test fun interruptedDownloadResumesAndRehashesTheActualPrefix()=file{part->val offset=262144;part.writeBytes(bytes.copyOfRange(0,offset));ModelFileTransfer.copyVerified(bytes.copyOfRange(offset,bytes.size).inputStream(),part,descriptor(),offset.toLong(),{});assertArrayEquals(bytes,part.readBytes())}
    @Test fun corruptedResumePrefixCannotPassFullChecksum()=file{part->val offset=262144;part.writeBytes(ByteArray(offset));val error=runCatching{ModelFileTransfer.copyVerified(bytes.copyOfRange(offset,bytes.size).inputStream(),part,descriptor(),offset.toLong(),{})}.exceptionOrNull();assertTrue(error is IllegalArgumentException)}
    @Test fun restartTruncatesAStalePartialInsteadOfAppending()=file{part->part.writeBytes(ByteArray(bytes.size+32));ModelFileTransfer.copyVerified(bytes.inputStream(),part,descriptor(),0,{});assertArrayEquals(bytes,part.readBytes())}
    @Test fun cancellationRetainsOnlyResumableBytesAndNeverClaimsComplete()=file{part->var stop=false;val error=runCatching{ModelFileTransfer.copyVerified(bytes.inputStream(),part,descriptor(),0,{stop=true}){stop}}.exceptionOrNull();assertTrue(error is CancellationException);assertTrue(part.length() in 1 until bytes.size.toLong());val offset=part.length();ModelFileTransfer.copyVerified(bytes.copyOfRange(offset.toInt(),bytes.size).inputStream(),part,descriptor(),offset,{});assertArrayEquals(bytes,part.readBytes())}
    @Test fun truncatedStreamAndOversizedStreamAreRejected()=file{part->assertTrue(runCatching{ModelFileTransfer.copyVerified(bytes.copyOf(120).inputStream(),part,descriptor(),0,{})}.isFailure);assertTrue(runCatching{ModelFileTransfer.copyVerified((bytes+byteArrayOf(1)).inputStream(),part,descriptor(),0,{})}.isFailure)}
    @Test fun optionalOneGigabyteProfileRequiresRealWeightSizedMetadata(){val p=BrainProfiles.balanced;assertTrue(p.minimumBytes>=900_000_000);assertTrue(p.maximumBytes<=1_300_000_000);assertNotEquals(BrainProfiles.lite.repository,p.repository);assertEquals("qwen2.5-1.5b-instruct-q4_k_m.gguf",p.file)}
    @Test fun storageBudgetsUseOnlyBoundPartialBytesAndAlwaysKeepReserve(){assertEquals(1_100_000_000+ModelInstallPolicy.reserveBytes,ModelInstallPolicy.requiredFreeBytes(1_100_000_000,0));assertEquals(500_000_000+ModelInstallPolicy.reserveBytes,ModelInstallPolicy.requiredFreeBytes(1_100_000_000,600_000_000));assertEquals(1_100_000_000+ModelInstallPolicy.reserveBytes,ModelInstallPolicy.requiredFreeBytes(1_100_000_000,1_100_000_000));assertTrue(runCatching{ModelInstallPolicy.requiredFreeBytes(100,101)}.isFailure)}
    @Test fun activationChecksInstallationAndCompatibilityWithoutARamThreshold(){assertNotNull(ModelInstallPolicy.activationError(false,null));assertEquals("Too hot",ModelInstallPolicy.activationError(true,"Too hot"));assertNull(ModelInstallPolicy.activationError(true,null));assertNull(BrainProfiles.compatibilityError(BrainProfiles.balanced,0,true));assertNotNull(BrainProfiles.compatibilityError(BrainProfiles.balanced,3,true));assertNotNull(BrainProfiles.compatibilityError(BrainProfiles.balanced,0,false))}
}
