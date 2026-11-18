package com.jarvis.assistant.studio

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class WallpaperStudioStoreTest {
    private class Fixture{var raw:String?=null;var exists=false;var fail=false;fun store()=WallpaperStudioStore({raw},{exists},{if(fail)error("disk");raw=it;exists=true})}
    private fun layer()=StudioLayer("core","PART:CORE")
    @Test fun emptyStoreKeepsOriginalWallpaper(){assertNull(Fixture().store().state.value.selected())}
    @Test fun savingProfileRequiresExplicitApply(){val s=Fixture().store();StudioPolicy.original();assertNull(s.state.value.active)}
    @Test fun explicitApplyPersistsChosenLayout(){val f=Fixture();val s=f.store();val p=StudioPolicy.original();s.apply(p);assertEquals(p,f.store().state.value.selected())}
    @Test fun profileChangesDoNotReplaceAnother(){val s=Fixture().store();val a=StudioPolicy.original("A");val b=StudioPolicy.original("B");s.apply(a);s.apply(b);assertEquals(2,s.state.value.profiles.size);assertTrue(s.state.value.profiles.contains(a))}
    @Test fun profileUpdateRetainsItsIdentity(){val s=Fixture().store();val a=StudioPolicy.original();s.apply(a);s.apply(a.copy(name="Changed"));assertEquals(1,s.state.value.profiles.size);assertEquals(a.id,s.state.value.active)}
    @Test fun maximumProfilesIsEnforced(){val s=Fixture().store();repeat(8){s.apply(StudioPolicy.original("Profile $it"))};assertThrows(IllegalStateException::class.java){s.apply(StudioPolicy.original("Ninth"))};assertEquals(8,s.state.value.profiles.size)}
    @Test fun restoringOriginalDoesNotDeleteProfiles(){val s=Fixture().store();s.apply(StudioPolicy.original());s.select(null);assertNull(s.state.value.selected());assertEquals(1,s.state.value.profiles.size)}
    @Test fun selectingOtherProfileIsExplicit(){val s=Fixture().store();val a=StudioPolicy.original("A");s.apply(a);s.apply(StudioPolicy.original("B"));s.select(a.id);assertEquals(a,s.state.value.selected())}
    @Test fun unknownSelectionCannotReplaceActiveProfile(){val s=Fixture().store();s.apply(StudioPolicy.original());val before=s.state.value;assertThrows(IllegalArgumentException::class.java){s.select(UUID.randomUUID().toString())};assertEquals(before,s.state.value)}
    @Test fun deletingSelectedProfileFallsBackToOriginal(){val s=Fixture().store();val a=StudioPolicy.original();s.apply(a);s.delete(a.id);assertNull(s.state.value.selected())}
    @Test fun deletingOtherProfileKeepsCurrent(){val s=Fixture().store();val a=StudioPolicy.original("A");val b=StudioPolicy.original("B");s.apply(a);s.apply(b);s.delete(a.id);assertEquals(b,s.state.value.selected())}
    @Test fun corruptStorageKeepsOriginalAndReportsUnavailable(){val f=Fixture();f.raw="broken";f.exists=true;val s=f.store();assertNull(s.state.value.selected());assertNotNull(s.storageError);assertEquals("broken",f.raw)}
    @Test fun corruptStorageCannotBeSilentlyOverwritten(){val f=Fixture();f.raw="broken";f.exists=true;assertThrows(Exception::class.java){f.store().apply(StudioPolicy.original())};assertEquals("broken",f.raw)}
    @Test fun undecryptableStorageIsPreserved(){val f=Fixture();f.exists=true;assertThrows(IllegalStateException::class.java){f.store().apply(StudioPolicy.original())};assertNull(f.raw)}
    @Test fun failedWritePreservesOldProfile(){val f=Fixture();val s=f.store();s.apply(StudioPolicy.original());val before=f.raw;val live=s.state.value;f.fail=true;assertThrows(IllegalStateException::class.java){s.apply(StudioPolicy.original())};assertEquals(before,f.raw);assertEquals(live,s.state.value)}
    @Test fun duplicateLayerIdsAreRejected(){val p=StudioPolicy.original();assertThrows(IllegalArgumentException::class.java){p.copy(layers=listOf(layer(),layer())).validated()}}
    @Test fun excessiveLayersAreRejected(){val p=StudioPolicy.original();assertThrows(IllegalArgumentException::class.java){p.copy(layers=(1..25).map{layer().copy(id="part_$it")}).validated()}}
    @Test fun unknownComponentIsRejected(){assertThrows(IllegalArgumentException::class.java){layer().copy(kind="SHELL:RUN").validated()}}
    @Test fun nonFinitePositionIsRejected(){assertThrows(IllegalArgumentException::class.java){layer().copy(x=Float.NaN).validated()}}
    @Test fun outOfBoundsPositionIsRejected(){assertThrows(IllegalArgumentException::class.java){layer().copy(y=1.1f).validated()}}
    @Test fun outOfBoundsScaleIsRejected(){assertThrows(IllegalArgumentException::class.java){layer().copy(scale=0f).validated()}}
    @Test fun invalidOpacityIsRejected(){assertThrows(IllegalArgumentException::class.java){layer().copy(opacity=-.1f).validated()}}
    @Test fun invalidLayerOrderIsRejected(){assertThrows(IllegalArgumentException::class.java){layer().copy(order=101).validated()}}
    @Test fun invalidFontScaleIsRejected(){assertThrows(IllegalArgumentException::class.java){layer().copy(fontScale=3f).validated()}}
    @Test fun invalidRotationIsRejected(){assertThrows(IllegalArgumentException::class.java){layer().copy(rotation=Float.POSITIVE_INFINITY).validated()}}
    @Test fun gradientRequiresFirstStop(){assertThrows(IllegalArgumentException::class.java){layer().copy(gradientEnd=0xff000000.toInt()).validated()}}
    @Test fun layersAndColorsRoundTripExactly(){val f=Fixture();val p=StudioPolicy.original().copy(layers=listOf(layer().copy(x=.2f,y=.8f,rotation=90f,opacity=.5f,order=40,color=0xffa12345.toInt(),gradientEnd=0xff778899.toInt(),fontScale=1.5f)));f.store().apply(p);assertEquals(p,f.store().state.value.selected())}
    @Test fun dragIsClampedToOwnedViewport(){val l=StudioPolicy.moved(layer(),-1f,2f,false);assertEquals(0f,l.x,0f);assertEquals(1f,l.y,0f)}
    @Test fun snapUsesFivePercentGrid(){val l=StudioPolicy.moved(layer(),.221f,.779f,true);assertEquals(.2f,l.x,.0001f);assertEquals(.8f,l.y,.0001f)}
    @Test fun freePlacementIsNotForcedToGrid(){val l=StudioPolicy.moved(layer(),.221f,.779f,false);assertEquals(.221f,l.x,0f);assertEquals(.779f,l.y,0f)}
    @Test fun privateWidgetsAreOffByDefault(){assertFalse(StudioPolicy.original().privateWidgets)}
    @Test fun lockedDeviceCannotDisplayPrivateWidgets(){assertFalse(StudioPolicy.visiblePrivate(true,false,false))}
    @Test fun quietModeCannotDisplayPrivateWidgets(){assertFalse(StudioPolicy.visiblePrivate(true,true,true))}
    @Test fun userOptInAndUnlockedActiveStateAreRequired(){assertFalse(StudioPolicy.visiblePrivate(false,true,false));assertTrue(StudioPolicy.visiblePrivate(true,true,false))}
    @Test fun allEightThemesAreRealValidatedProfiles(){assertEquals(8,StudioPolicy.themes.size);StudioPolicy.themes.forEach{StudioPolicy.theme(it).validated()}}
    @Test fun unknownThemeDoesNotBecomeFakeSavedState(){assertThrows(IllegalArgumentException::class.java){StudioPolicy.theme("Unknown")}}
}
