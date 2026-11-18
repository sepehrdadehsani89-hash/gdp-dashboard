package com.jarvis.assistant.studio

import org.junit.Assert.*
import org.junit.Test

class StudioColorTest {
    @Test fun rgbHexIsOpaque(){assertEquals(0xffff0000.toInt(),StudioColor.hex("#FF0000"))}
    @Test fun argbHexPreservesTransparency(){assertEquals(0x80123456.toInt(),StudioColor.hex("#80123456"))}
    @Test fun lowercaseHexWorks(){assertEquals(0xff12abef.toInt(),StudioColor.hex("#12abef"))}
    @Test fun surroundingWhitespaceWorks(){assertEquals(0xff00ff00.toInt(),StudioColor.hex("  #00FF00  "))}
    @Test fun fullSpectrumIsNotLimitedToPresets(){assertEquals(0xffb41c72.toInt(),StudioColor.hex("B41C72"))}
    @Test fun shortAmbiguousHexIsRejected(){assertThrows(IllegalArgumentException::class.java){StudioColor.hex("#abc")}}
    @Test fun nonHexIsRejected(){assertThrows(IllegalArgumentException::class.java){StudioColor.hex("#GGFFFF")}}
    @Test fun excessiveHexIsRejected(){assertThrows(IllegalArgumentException::class.java){StudioColor.hex("#0011223344")}}
    @Test fun argbRoundTripPreservesAllChannels(){val c=0x12345678;assertEquals(c,StudioColor.hex(StudioColor.hex(c)))}
    @Test fun rgbUsesActualChannels(){assertEquals(0xff123456.toInt(),StudioColor.rgb(18,52,86))}
    @Test fun rgbAlphaIsApplied(){assertEquals(0x80123456.toInt(),StudioColor.rgb(18,52,86,.5f))}
    @Test fun rgbOutOfRangeIsRejected(){assertThrows(IllegalArgumentException::class.java){StudioColor.rgb(256,0,0)}}
    @Test fun negativeRgbIsRejected(){assertThrows(IllegalArgumentException::class.java){StudioColor.rgb(0,-1,0)}}
    @Test fun invalidAlphaIsRejected(){assertThrows(IllegalArgumentException::class.java){StudioColor.rgb(0,0,0,Float.NaN)}}
    @Test fun hsvRedIsCorrect(){assertEquals(0xffff0000.toInt(),StudioColor.hsv(0f,1f,1f))}
    @Test fun hsvGreenIsCorrect(){assertEquals(0xff00ff00.toInt(),StudioColor.hsv(120f,1f,1f))}
    @Test fun hsvBlueIsCorrect(){assertEquals(0xff0000ff.toInt(),StudioColor.hsv(240f,1f,1f))}
    @Test fun fullCircleHueWrapsCorrectly(){assertEquals(StudioColor.hsv(0f,1f,1f),StudioColor.hsv(360f,1f,1f))}
    @Test fun hsvZeroSaturationIsGray(){assertEquals(0xff808080.toInt(),StudioColor.hsv(170f,0f,.5f))}
    @Test fun hsvZeroValueIsBlack(){assertEquals(0xff000000.toInt(),StudioColor.hsv(170f,1f,0f))}
    @Test fun hsvRejectsNonFiniteValues(){assertThrows(IllegalArgumentException::class.java){StudioColor.hsv(Float.POSITIVE_INFINITY,1f,1f)}}
    @Test fun hsvRejectsUnnormalizedSaturation(){assertThrows(IllegalArgumentException::class.java){StudioColor.hsv(0f,2f,1f)}}
    @Test fun hslRedIsCorrect(){assertEquals(0xffff0000.toInt(),StudioColor.hsl(0f,1f,.5f))}
    @Test fun hslYellowIsCorrect(){assertEquals(0xffffff00.toInt(),StudioColor.hsl(60f,1f,.5f))}
    @Test fun hslZeroLightnessIsBlack(){assertEquals(0xff000000.toInt(),StudioColor.hsl(220f,1f,0f))}
    @Test fun hslFullLightnessIsWhite(){assertEquals(0xffffffff.toInt(),StudioColor.hsl(220f,1f,1f))}
    @Test fun hslRejectsInvalidLightness(){assertThrows(IllegalArgumentException::class.java){StudioColor.hsl(0f,1f,-.1f)}}
    @Test fun hslPreservesAlpha(){assertEquals(0x8000ff00.toInt(),StudioColor.hsl(120f,1f,.5f,.5f))}
}
