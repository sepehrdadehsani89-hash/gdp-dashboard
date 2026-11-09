package com.jarvis.assistant.power

/** Real native frame-start gaps and CPU draw/post durations, bounded to the latest 120 samples. */
class RenderMeasurements {
    private val gaps=LongArray(120)
    private val draws=LongArray(120)
    private var count=0L
    @Synchronized fun record(gap:Long,draw:Long){
        val i=(count%120).toInt();gaps[i]=gap.coerceAtLeast(0);draws[i]=draw.coerceAtLeast(0);count++
    }
    @Synchronized fun summary():String {
        val n=minOf(count,120).toInt()
        if(n==0)return "Wallpaper timing: not measured in this process"
        val sorted=gaps.take(n).filter{it>0}.sorted()
        val p95=sorted.getOrNull(((sorted.size-1)*.95).toInt().coerceAtLeast(0))
        val gapText=p95?.let{"$it ms"} ?: "unavailable"
        return "Wallpaper: $count observed frames · p95 frame-start gap $gapText · max CPU draw/post ${draws.take(n).maxOrNull()} ms (not GPU completion)"
    }
}
