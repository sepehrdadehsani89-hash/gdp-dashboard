package com.jarvis.assistant.companion

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class NearbyCompanionPolicyTest {
    private val id=UUID.randomUUID().toString()
    private val pair=DesktopPair("https://192.168.1.8:8765","a".repeat(64),id,UUID.randomUUID().toString(),UUID.randomUUID().toString(),"T".repeat(43))
    private fun attrs(device:String=id,protocol:String="1")=mapOf("device" to device.toByteArray(),"protocol" to protocol.toByteArray())
    private fun candidate(host:String="192.168.1.8",port:Int=8765,attributes:Map<String,ByteArray> = attrs(),saved:List<DesktopPair> = listOf(pair))=NearbyCompanionPolicy.candidate(host,port,attributes,saved)
    @Test fun exactServiceTypeRetainsProtocol(){assertEquals("_jarvis-pc._tcp.",NearbyCompanionPolicy.TYPE)}
    @Test fun savedIdentityIsOnlyATrustCandidate(){assertTrue(candidate()!!.known)}
    @Test fun unknownIdentityStillRequiresPairing(){assertFalse(candidate(attributes=attrs(UUID.randomUUID().toString()))!!.known)}
    @Test fun noSavedPairIsCreatedByDiscovery(){assertFalse(candidate(saved=emptyList())!!.known)}
    @Test fun storedCredentialsAreNotInTheCandidate(){val c=candidate()!!;assertFalse(c.toString().contains(pair.token));assertFalse(c.toString().contains(pair.sessionId))}
    @Test fun publicAddressRejected(){assertNull(candidate("8.8.8.8"))}
    @Test fun wildcardBindRejected(){assertNull(candidate("0.0.0.0"))}
    @Test fun linkLocalMetadataAddressRejected(){assertNull(candidate("169.254.169.254"))}
    @Test fun internetHostnameRejected(){assertNull(candidate("example.com"))}
    @Test fun ipv6UnsupportedIsExplicitlyExcluded(){assertNull(candidate("::1"))}
    @Test fun lowPortRejected(){assertNull(candidate(port=443))}
    @Test fun overrangePortRejected(){assertNull(candidate(port=65536))}
    @Test fun malformedDeviceRejected(){assertNull(candidate(attributes=attrs("not a UUID")))}
    @Test fun missingDeviceRejected(){assertNull(candidate(attributes=mapOf("protocol" to "1".toByteArray())))}
    @Test fun missingProtocolRejected(){assertNull(candidate(attributes=mapOf("device" to id.toByteArray())))}
    @Test fun unsupportedProtocolRejected(){assertNull(candidate(attributes=attrs(protocol="2")))}
    @Test fun injectedCertificateDoesNotReplaceTrust(){val c=candidate(attributes=attrs()+mapOf("certificate" to "trust-anything".toByteArray()))!!;assertTrue(NearbyCompanionPolicy.canUseSavedEndpoint(c,pair));assertEquals("a".repeat(64),pair.pin)}
    @Test fun sameDeviceAtAnotherAddressIsNotSilentlyRebound(){assertFalse(NearbyCompanionPolicy.canUseSavedEndpoint(candidate("192.168.1.9")!!,pair))}
    @Test fun sameDeviceAtAnotherPortIsNotSilentlyRebound(){assertFalse(NearbyCompanionPolicy.canUseSavedEndpoint(candidate(port=8766)!!,pair))}
    @Test fun unknownCandidateCannotUseSavedEndpoint(){assertFalse(NearbyCompanionPolicy.canUseSavedEndpoint(candidate(saved=emptyList())!!,pair))}
    @Test fun otherDeviceCannotHijackSavedEndpoint(){val other=pair.copy(deviceId=UUID.randomUUID().toString());assertFalse(NearbyCompanionPolicy.canUseSavedEndpoint(candidate()!!,other))}
    @Test fun exactSavedEndpointCanBeAuthenticated(){assertTrue(NearbyCompanionPolicy.canUseSavedEndpoint(candidate()!!,pair))}
}
