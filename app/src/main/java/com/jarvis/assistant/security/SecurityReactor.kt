package com.jarvis.assistant.security

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Original native Sentinel-inspired motion, driven by real security state; no credential fields. */
@Composable fun SecurityReactor(state:SecurityStage,reducedMotion:Boolean,completed:Int=0){
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var angle by remember{mutableFloatStateOf(0f)}
    LaunchedEffect(state,reducedMotion,lifecycle){if(!reducedMotion)lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){while(true){angle=(SystemClock.elapsedRealtime()%18000)/50f;delay(50)}}}
    val color=when(state){SecurityStage.VERIFIED->Color(0xFF22C59A);SecurityStage.VERIFYING,SecurityStage.AUTHENTICATION_REQUIRED->Color(0xFF7C4DFF);SecurityStage.CANCELLED->Color(0xFFFFBF55);SecurityStage.UNAVAILABLE->Color(0xFFFF5368);else->Color(0xFF00B8FF)}
    Canvas(Modifier.fillMaxWidth().height(150.dp)){
        val radius=size.minDimension*.36f
        drawCircle(color.copy(alpha=.15f),radius*1.25f,style=Stroke(3.dp.toPx()))
        for(i in 0..2){val r=radius*(1f-i*.2f);drawArc(color,angle*(if(i%2==0)1f else -1.3f)+i*80,245f,false,Offset(center.x-r,center.y-r),androidx.compose.ui.geometry.Size(r*2,r*2),style=Stroke((2-i*.4f).dp.toPx()))}
        if(completed>0)drawArc(Color(0xFF22C59A),-90f,(completed.coerceIn(0,3)/3f)*360,false,Offset(center.x-radius*1.18f,center.y-radius*1.18f),androidx.compose.ui.geometry.Size(radius*2.36f,radius*2.36f),style=Stroke(3.dp.toPx()))
    }
}
