/*
*  
* This file is part of BlueScale.
*
* BlueScale is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* BlueScale is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
* 
* You should have received a copy of the GNU Affero General Public License
* along with BlueScale.  If not, see <http://www.gnu.org/licenses/>.
* 
* Copyright Vincent Marquez 2010
* 
* 
* Please contact us at www.BlueScaleSoftware.com
*
*/
package com.bss.telco.jainsip.sample

import com.bss.telco.api._
import com.bss.telco.jainsip._
import com.bss.telco._

import org.junit._
import Assert._
import java.util.concurrent.CountDownLatch


class SampleJoinTwo {
	
	var latch:CountDownLatch = null
 
	val telcoServer = new SipTelcoServer("10.112.53.94", 5080, "209.133.53.210", 5080 )
	telcoServer.start()
 	  
 	def handleDisconnect(conn:SipConnection) = {
 	    //this should be bob!
		System.err.println("assert that alice is disconnected = " + alice.connectionState)
		System.err.println("assert that bob is disconnected = " + bob.connectionState)
		latch.countDown()
 	}

     	 
    telcoServer.setDisconnectedCallback(handleDisconnect)
 	val alice   = telcoServer.createConnection("+17147570982", "9495557777")
 	val bob =   telcoServer.createConnection("+19497773456", "7147773333")

 
	def runConn() {
 		latch = new CountDownLatch(1)
	          
 		alice.connect(()=>{ 
		  	bob.connect(()=>{
				alice.join(bob, ()=>{
				  System.err.println("are both connected = ? " + telcoServer.areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]))
				  
				  	alice.disconnect( ()=>{
				  	  println("alice connectionstate = "+ alice.connectionState)
				  		//make sure bob is on hold now!
				  		//assertFalse(getTelcoServer.areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]))
				  		val b = SdpHelper.isBlankSdp(bob.asInstanceOf[JainSipConnection].sdp)
				  		System.err.println("b = " + b)
				  		//Now bob should be disconnected
					})
				})
			})
		})
	}
}

object SampleJoinTwo {

    def main(args:Array[String]) = {
        val sample = new SampleJoinTwo()
        val a = readLine()
        sample.runConn()
    }
}


