package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.Normalizer
import java.util.Locale

/** Bounded arithmetic grammar, not code evaluation or a substitute for open-ended local AI. */
object LocalArithmetic {
    enum class Error { DIVIDE_BY_ZERO, INVALID_EXPRESSION, LIMIT_EXCEEDED }
    data class Request(val expression:String)
    data class Result(val value:String?=null,val approximate:Boolean=false,val error:Error?=null)
    private val small=listOf("zero","one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen")
    private val numbers=small.mapIndexed{i,s->s to i}.toMap()+mapOf("twenty" to 20,"thirty" to 30,"forty" to 40,"fifty" to 50,"sixty" to 60,"seventy" to 70,"eighty" to 80,"ninety" to 90,"cero" to 0,"uno" to 1,"dos" to 2,"tres" to 3,"cuatro" to 4,"cinco" to 5,"seis" to 6,"siete" to 7,"ocho" to 8,"nueve" to 9,"diez" to 10,"once" to 11,"doce" to 12,"trece" to 13,"catorce" to 14,"quince" to 15,"dieciseis" to 16,"diecisiete" to 17,"dieciocho" to 18,"diecinueve" to 19,"veinte" to 20,"veintiuno" to 21,"veintidos" to 22,"veintitres" to 23,"veinticuatro" to 24,"veinticinco" to 25,"veintiseis" to 26,"veintisiete" to 27,"veintiocho" to 28,"veintinueve" to 29,"treinta" to 30,"cuarenta" to 40,"cincuenta" to 50,"sesenta" to 60,"setenta" to 70,"ochenta" to 80,"noventa" to 90,"cien" to 100,"ciento" to 100)
    private val context=MathContext(34,RoundingMode.HALF_UP)
    fun request(text:String):Request? {
        if(text.length !in 1..300)return null
        var s=Normalizer.normalize(text,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT).trim().trimStart('¿','¡').trimEnd('?','!','.')
        s=s.removePrefix("hey jarvis ").removePrefix("oye jarvis ").removePrefix("jarvis ").removePrefix("please ").replace("what's","what is")
        s=s.replace(Regex("^(?:what is|how much is|calculate|compute|cuanto es|cuanto da|calcula)\\s+"),"")
        s=s.replace('×','*').replace('÷','/').replace('−','-').replace(',','.')
        val percentage=Regex("^(.+?)\\s*(?:percent of|por ciento de|%\\s*(?:of|de))\\s+(.+)$").matchEntire(s)
        if(percentage!=null)s="(${percentage.groupValues[1]}) / 100 * (${percentage.groupValues[2]})"
        s=s.replace(Regex("\\b(?:multiplied by|times|multiplicado por|por)\\b"),"*")
            .replace(Regex("\\b(?:divided by|dividido entre|dividido por|entre|over)\\b"),"/")
            .replace(Regex("\\b(?:plus|mas)\\b"),"+").replace(Regex("\\b(?:minus|negative|menos|negativo)\\b"),"-")
            .replace(Regex("\\b(?:point|punto)\\b"),".")
        val words=Regex("[a-z]+|(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)|[+*/%().-]").findAll(s).map{it.value}.toList()
        if(words.isEmpty() || words.size>80)return null
        if(words.joinToString("")!=s.replace(Regex("\\s+"),""))return null
        val result=mutableListOf<String>();var i=0
        while(i<words.size){
            val token=words[i]
            if(token in numbers){
                var current=0;var seen=false;var previousWasNumber=false
                while(i<words.size){
                    val n=numbers[words[i]]
                    if(n!=null){if(previousWasNumber && current%100!=0 && !(current%10==0 && current%100>=20 && n in 1..9))return null;current+=n;seen=true;previousWasNumber=true;i++}
                    else if(words[i]=="hundred" && seen && current in 1..9){current*=100;i++;previousWasNumber=false}
                    else if(words[i] in setOf("and","y") && seen && i+1<words.size && words[i+1] in numbers){i++;previousWasNumber=false}
                    else break
                    if(current>999)return null
                }
                result+=current.toString()
            }else{
                if(token.any(Char::isLetter))return null
                result+=token;i++
            }
        }
        val expression=result.joinToString(" ").replace(Regex("([0-9]+)\\s+\\.\\s+([0-9]+)"),"$1.$2")
        if(!expression.any{it in "+-*/%"})return null
        return Request(expression)
    }
    fun evaluate(request:Request):Result {
        if(request.expression.length>300)return Result(error=Error.LIMIT_EXCEEDED)
        val tokens=request.expression.split(" ").filter(String::isNotBlank)
        if(tokens.size>80)return Result(error=Error.LIMIT_EXCEEDED)
        class Failure(val code:Error):RuntimeException()
        var position=0;var depth=0;var operations=0;var approximate=false
        fun checked(n:BigDecimal):BigDecimal {if(n.abs()>BigDecimal("1E100"))throw Failure(Error.LIMIT_EXCEEDED);return n}
        lateinit var expression:()->BigDecimal
        lateinit var factor:()->BigDecimal
        factor={
            if(++depth>16)throw Failure(Error.LIMIT_EXCEEDED)
            val token=tokens.getOrNull(position++) ?: throw Failure(Error.INVALID_EXPRESSION)
            var value=when(token){
                "+"->factor()
                "-"->factor().negate()
                "("->expression().also{if(tokens.getOrNull(position++)!=")")throw Failure(Error.INVALID_EXPRESSION)}
                else->{if(token.length>40)throw Failure(Error.LIMIT_EXCEEDED);if(!token.matches(Regex("(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)")))throw Failure(Error.INVALID_EXPRESSION);BigDecimal(token)}
            }
            if(tokens.getOrNull(position)=="%"){position++;value=value.divide(BigDecimal(100),context)}
            depth--;checked(value)
        }
        fun term():BigDecimal {var value=factor();while(tokens.getOrNull(position) in listOf("*","/")){if(++operations>32)throw Failure(Error.LIMIT_EXCEEDED);val op=tokens[position++];val right=factor();if(op=="/" && right.compareTo(BigDecimal.ZERO)==0)throw Failure(Error.DIVIDE_BY_ZERO);value=checked(if(op=="*")value.multiply(right)else try{value.divide(right)}catch(_:ArithmeticException){approximate=true;value.divide(right,context)})};return value}
        expression={var value=term();while(tokens.getOrNull(position) in listOf("+","-")){if(++operations>32)throw Failure(Error.LIMIT_EXCEEDED);val op=tokens[position++];val right=term();value=checked(if(op=="+")value.add(right)else value.subtract(right))};value}
        return try{val value=expression();if(position!=tokens.size)throw Failure(Error.INVALID_EXPRESSION);val rounded=value.setScale(10,RoundingMode.HALF_UP).stripTrailingZeros();Result(rounded.toPlainString(),approximate || value.compareTo(rounded)!=0)}catch(e:Failure){Result(error=e.code)}catch(_:ArithmeticException){Result(error=Error.INVALID_EXPRESSION)}
    }
    fun answer(text:String,language:AppLanguage):String? {
        val request=request(text) ?: return null
        val result=evaluate(request)
        val es=language==AppLanguage.ES
        return when(result.error){
            Error.DIVIDE_BY_ZERO->if(es)"Señor, no se puede dividir entre cero." else "Sir, division by zero is undefined."
            Error.INVALID_EXPRESSION->if(es)"No pude completar esa expresión matemática local. Revisa los números y operadores." else "That local arithmetic expression is incomplete. Please check its numbers and operators."
            Error.LIMIT_EXCEEDED->if(es)"Ese cálculo supera los límites de la calculadora local." else "That calculation exceeds the local calculator's limits."
            null->if(es)"Señor, el resultado es ${if(result.approximate)"aproximadamente " else ""}${result.value}." else "Sir, the result is ${if(result.approximate)"approximately " else ""}${result.value}."
        }
    }
}
