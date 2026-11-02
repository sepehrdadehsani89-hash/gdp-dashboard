package com.jarvis.assistant.neural

object DownloadResumePolicy {
    /** A 200 response restarts safely; a 206 must bind the exact requested range and total. */
    fun initialBytes(offset:Long,expected:Long,code:Int,contentRange:String?,contentLength:Long):Long {
        require(offset in 0 until expected)
        val initial=when {
            code==200->0L
            code==206 && offset>0->{
                val m=Regex("bytes (\\d+)-(\\d+)/(\\d+)").matchEntire(contentRange.orEmpty()) ?: error("Invalid resumed range")
                require(m.groupValues[1].toLong()==offset && m.groupValues[2].toLong()==expected-1 && m.groupValues[3].toLong()==expected){"Resumed download does not match the verified file"};offset
            }
            else->throw IllegalArgumentException("Unsupported model response")
        }
        require(contentLength<0 || contentLength==expected-initial){"Unexpected model response size"}
        return initial
    }
}
