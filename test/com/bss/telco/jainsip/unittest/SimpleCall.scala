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
package org.bluescale.telco.jainsip.unittest

import org.junit._
import Assert._
import java.util.concurrent.CountDownLatch
import org.bluescale.telco._

import org.bluescale.telco.jainsip._
import org.bluescale.telco.api._
import java.util.concurrent.atomic.AtomicInteger

trait SimpleCall {

	var latch:CountDownLatch = null
  
	def getLatch = latch
	
	def getCounter:Option[AtomicInteger]
		
 	def getTelcoServer() : TelcoServer
   
    def getB2BServer() : B2BServer

 	def runConn() {
 	  runConn("9495557777")
 	}
  
	def runConn(destNumber:String) {
 		latch = new CountDownLatch(1)
	  
 		val alice = getTelcoServer().createConnection(destNumber, "4445556666")
 		 
 		alice.connect(()=>{ 
		  	assertEquals(alice.connectionState, CONNECTED())
		  	println("OK i'm Connected now...how did that happen?")
		  	getB2BServer().findConnByDest(destNumber).foreach( _.disconnect( ()=> {
                        Thread.sleep(50)
                        println("Is alice disconnected alice = " + alice.connectionState)
                        assertEquals(alice.connectionState,UNCONNECTED())
                        latch.countDown()
			        }))
/*
		  	alice.disconnect(()=>{
		  			//assertEquals(alice.connectionState, UNCONNECTED())
		  			println("disconnected")
		  			getCounter.map( x => x.getAndIncrement );
		  			latch.countDown
		  		})
		  		*/
 		})
	}
 }
 
