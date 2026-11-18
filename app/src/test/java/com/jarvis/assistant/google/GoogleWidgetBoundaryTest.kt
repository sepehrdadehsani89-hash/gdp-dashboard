package com.jarvis.assistant.google

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class GoogleWidgetBoundaryTest(private val product:GoogleProduct,private val scenario:String){
    @Test fun onlyCurrentAuthorizedAccountDataCanBeDisplayed(){
        val now=1700000000000L;val other=if(product==GoogleProduct.CALENDAR)GoogleProduct.TASKS else GoogleProduct.CALENDAR
        val source="Actual private ${product.name} title"
        var value:GoogleWidgetObservation?=GoogleWidgetObservation("account-one",3,product,source,now)
        var account:String?="account-one";var epoch=3L;var grant=true;var privateAllowed=true;var at=now
        when(scenario){"absent"->value=null;"account"->account="other";"removed"->account=null;"epoch"->epoch=4;"revoked"->grant=false;"hidden"->privateAllowed=false;"expired"->at+=300001;"backward"->at--;"product"->value=value!!.copy(product=other)}
        val result=GoogleWidgetPolicy.text(value,product,account,epoch,grant,at,privateAllowed)
        assertEquals(scenario=="actual",result.contains(source))
        if(scenario=="actual")assertTrue(result.contains("Source read:"))
    }
    companion object{@JvmStatic @Parameterized.Parameters(name="{0} {1}")fun cases()=listOf(GoogleProduct.CALENDAR,GoogleProduct.TASKS).flatMap{p->listOf("actual","absent","account","removed","epoch","revoked","hidden","expired","backward","product").map{arrayOf(p,it)}}}
}
