package com.jarvis.assistant.ui
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.jarvis.assistant.core.CaptionHighlightPolicy
fun confirmedCaption(text:String,label:String?):AnnotatedString=AnnotatedString.Builder(text).apply{
    CaptionHighlightPolicy.ranges(text,label).forEach{addStyle(SpanStyle(color=Color(0xFF22C55E)),it.first,it.last+1)}
}.toAnnotatedString()
