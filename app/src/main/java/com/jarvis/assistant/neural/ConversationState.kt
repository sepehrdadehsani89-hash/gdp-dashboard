package com.jarvis.assistant.neural

import com.jarvis.assistant.core.ChatMessage
import com.jarvis.assistant.core.MessageRole

/** Ephemeral extractive context, not a hidden long-term memory or a model-authored fact. */
class ConversationState {
    private val turns=ArrayDeque<ChatMessage>();private val older=ArrayDeque<String>()
    var weatherCity:String?=null;private set
    var weatherDay:Int=0;private set
    private var topicAt=0L
    fun weather(city:String,day:Int,now:Long){weatherCity=city;weatherDay=day;topicAt=now}
    fun weatherFollowUp(input:String,now:Long):Int? {
        if(weatherCity==null || now-topicAt !in 0..600_000)return null
        val t=AppMatching.normalize(input)
        return when {
            t in setOf("and the day after","the day after","y el dia siguiente","و روز بعد","پس فردا چی")->(weatherDay+1).coerceAtMost(7)
            t in setOf("what about tomorrow","and tomorrow","y manana","فردا چی")->1
            t in setOf("what about next week","and next week","y la proxima semana","هفته بعد چی")->7
            else->null
        }
    }
    fun record(question:String,answer:String){turns.add(ChatMessage(role=MessageRole.USER,text=question.take(1200)));turns.add(ChatMessage(role=MessageRole.ASSISTANT,text=answer.take(1600)));while(turns.size>6){val q=turns.removeFirst();val a=turns.removeFirst();older.addLast("User: ${q.text.take(160)}; assistant excerpt (unverified): ${a.text.take(160)}");while(older.size>4)older.removeFirst()}}
    fun expire(before:Long){if(turns.any{it.timestamp<before})clear()}
    fun history()=turns.toList()
    fun summary()=older.joinToString("\n").take(900)
    fun clear(){turns.clear();older.clear();weatherCity=null;weatherDay=0;topicAt=0}
}
