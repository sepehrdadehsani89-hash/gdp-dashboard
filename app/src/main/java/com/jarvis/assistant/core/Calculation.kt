package com.jarvis.assistant.core

import java.math.BigDecimal
import java.math.MathContext

/** Bounded arithmetic only; never evaluates code, identifiers, or model-generated expressions. */
object Calculation {
    fun requested(text:String):String? {
        val match=Regex("^(?:calculate|compute|calcula|calcular|محاسبه کن|حساب کن)\\s+(.+)$",RegexOption.IGNORE_CASE).matchEntire(text.trim()) ?: return null
        return evaluate(match.groupValues[1])
    }
    fun evaluate(expression:String):String {
        require(expression.length in 1..200){"Use an arithmetic expression up to 200 characters."}
        val input=expression.map { c-> when(c){in '۰'..'۹'->'0'+(c-'۰');in '٠'..'٩'->'0'+(c-'٠');'×'->'*';'÷'->'/';'−'->'-';else->c} }.joinToString("")
        require(input.all { it.isDigit() || it in ".+-*/() \t" }){"Only numbers, parentheses and + − × ÷ are supported."}
        var pos=0;var depth=0
        fun space(){while(pos<input.length && input[pos].isWhitespace())pos++}
        lateinit var sum:()->BigDecimal
        fun atom():BigDecimal {
            space();require(++depth<=20){"Expression is nested too deeply."}
            val value=when {
                pos<input.length && input[pos]=='+'->{pos++;atom()}
                pos<input.length && input[pos]=='-'->{pos++;atom().negate()}
                pos<input.length && input[pos]=='('->{pos++;val n=sum();space();require(pos<input.length && input[pos++]==')'){"Missing closing parenthesis."};n}
                else->{val start=pos;while(pos<input.length && (input[pos].isDigit()||input[pos]=='.'))pos++;require(pos>start){"A number is required."};input.substring(start,pos).toBigDecimal(MathContext.DECIMAL64)}
            };depth--;return value
        }
        fun product():BigDecimal {var n=atom();while(true){space();if(pos>=input.length || input[pos] !in "*/")return n;val op=input[pos++];val r=atom();n=if(op=='*')n.multiply(r,MathContext.DECIMAL64) else n.divide(r,MathContext.DECIMAL64)}}
        sum={var n=product();while(true){space();if(pos>=input.length || input[pos] !in "+-")break;val op=input[pos++];val r=product();n=if(op=='+')n.add(r,MathContext.DECIMAL64) else n.subtract(r,MathContext.DECIMAL64)};n}
        val result=sum();space();require(pos==input.length){"Unexpected input."};require(result.precision()+kotlin.math.abs(result.scale())<500){"Result too large."};return result.stripTrailingZeros().toPlainString()
    }
}
