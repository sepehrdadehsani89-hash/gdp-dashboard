package com.jarvis.assistant.contacts
import com.jarvis.assistant.core.CaptionHighlightPolicy
import org.junit.Assert.*
import org.junit.Test
class CaptionHighlightPolicyTest {
    @Test fun uncertainOrMissingEntityNeverGetsAConfirmedHighlight(){assertTrue(CaptionHighlightPolicy.ranges("call Dad",null).isEmpty());assertTrue(CaptionHighlightPolicy.ranges("open Spotify","").isEmpty())}
    @Test fun confirmedAliasesHighlightOnlyActualCaptionWords(){
        val text="call Dad";val range=CaptionHighlightPolicy.ranges(text,"dad").single();assertEquals("Dad",text.substring(range))
        assertTrue(CaptionHighlightPolicy.ranges("Baghdad","dad").isEmpty())
        val fa="تماس با بابا";assertEquals("بابا",fa.substring(CaptionHighlightPolicy.ranges(fa,"بابا").single()))
    }
}
