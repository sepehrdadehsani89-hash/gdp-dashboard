package com.jarvis.assistant.voice

/** Monotonic command gate. Wake-word detection can continue after this gate closes. */
class ConversationWindow(private val now: () -> Long) {
    enum class Mode { OFF, WAKE_ONLY, WAITING_FOR_CAPTURE, COMMAND, FOLLOW_UP, PAUSED }
    data class Snapshot(val mode: Mode, val deadline: Long, val resetSerial: Long) {
        val awaitingCommand: Boolean get() = mode == Mode.COMMAND || mode == Mode.FOLLOW_UP
    }
    private var mode = Mode.OFF
    private var deadline = 0L
    private var resetSerial = 0L
    private var lastActivation: Long? = null

    @Synchronized fun snapshot() = Snapshot(mode, deadline, resetSerial)
    @Synchronized fun enable() { change(Mode.WAKE_ONLY) }
    @Synchronized fun disable() { change(Mode.OFF) }
    @Synchronized fun pause() { if (mode != Mode.OFF) change(Mode.PAUSED) }
    @Synchronized fun resume(followUp: Boolean) {
        if (mode == Mode.OFF) return
        change(if (followUp) Mode.WAITING_FOR_CAPTURE else Mode.WAKE_ONLY)
    }
    /** Start the follow-up timer only once the recorder and echo guard are ready. */
    @Synchronized fun captureReady(): Boolean {
        if (mode != Mode.WAITING_FOR_CAPTURE) return false
        mode = Mode.FOLLOW_UP
        deadline = now() + DURATION_MS
        return true
    }
    /** Partials and repeated phrases within an open window never restart its timer. */
    @Synchronized fun wake(): Boolean {
        if (mode != Mode.WAKE_ONLY) return false
        val time = now()
        if (lastActivation?.let { time - it < COOLDOWN_MS } == true) return false
        lastActivation = time
        mode = Mode.COMMAND
        deadline = time + DURATION_MS
        return true
    }
    @Synchronized fun expire(): Boolean {
        if (mode !in listOf(Mode.COMMAND, Mode.FOLLOW_UP) || now() < deadline) return false
        change(Mode.WAKE_ONLY)
        return true
    }
    @Synchronized fun acceptsCommand(serial: Long = resetSerial): Boolean =
        serial == resetSerial && mode in listOf(Mode.COMMAND, Mode.FOLLOW_UP) && now() < deadline
    @Synchronized fun accept(serial: Long = resetSerial): Boolean {
        if (!acceptsCommand(serial)) return false
        change(Mode.PAUSED)
        return true
    }
    private fun change(value: Mode) { mode = value; deadline = 0; resetSerial++ }
    companion object { const val DURATION_MS = 10_000L; private const val COOLDOWN_MS = 1_500L }
}
