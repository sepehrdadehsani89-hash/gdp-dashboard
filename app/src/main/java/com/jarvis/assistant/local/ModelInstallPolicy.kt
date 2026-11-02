package com.jarvis.assistant.local

/** Installation never grants permission to execute a model or switch the selected provider. */
object ModelInstallPolicy {
    const val reserveBytes = 150L * 1024 * 1024
    fun requiredFreeBytes(modelBytes:Long, resumableBytes:Long):Long {
        require(modelBytes > 0 && resumableBytes in 0..modelBytes)
        // A complete but unverified partial is restarted by the downloader.
        val retained = if(resumableBytes == modelBytes) 0 else resumableBytes
        return Math.addExact(modelBytes - retained, reserveBytes)
    }
    fun activationError(installed:Boolean, hardwareError:String?):String? =
        if(!installed) "Install and verify this model first. Your current brain remains selected." else hardwareError
    fun formatBytes(bytes:Long):String = java.lang.String.format(java.util.Locale.ROOT,"%.2f GB (%,d bytes)",bytes/1_000_000_000.0,bytes)
}
