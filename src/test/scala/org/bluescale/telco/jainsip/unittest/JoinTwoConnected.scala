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
* Please contact us at www.BlueScale.org
*
*/
package org.bluescale.telco.jainsip.unittest

import org.bluescale.telco.jainsip._
import org.bluescale.telco.api._
import java.util.concurrent.CountDownLatch
import org.bluescale.telco._

class JoinTwoConnected extends FunTestHelper {
	
	test("join two connections that were previously connected to other endpoints") {
		 runConn()
		 getLatch.await()
 	 
	}
	
	val latch = new CountDownLatch(1)
 
	def getLatch = latch
	
  
 	def handleDisconnect(conn:SipConnection) = println("disconnected!")

 	var cell:SipConnection = null 
 
	var desk:SipConnection = null

	var moviePhone1:SipConnection = null
                   
	def firstJoined() {
		println(" FIRST joined, now waiting for moviepHONE!!")
		println("cell joined to = " + cell.joinedTo )
		println("desk joined to = " + desk.joinedTo )
        assert(telcoServer.areTwoConnected(cell.asInstanceOf[SipConnectionImpl], desk.asInstanceOf[SipConnectionImpl]))
        //Thread.sleep(30)
		moviePhone1.connect().run{ 
								println("this should implicitly put desk on hold.")
                                moviePhone1.join(cell).run { joined(moviePhone1, cell) }
                                
                         	 }
	 }
	
	def disconnected(c:SipConnection): Unit  = {
		 println("i've been disconnected for " + c);
	}
 
	def joined(c1:SipConnection, c2:SipConnection) : Unit = {
	    println("OK now we should hear moviephone and not each other....")
        assert(telcoServer.areTwoConnected(c1.asInstanceOf[SipConnectionImpl], c2.asInstanceOf[SipConnectionImpl]))
	    System.err.println("are both connected = ? " + telcoServer.areTwoConnected(c1.asInstanceOf[SipConnectionImpl], c2.asInstanceOf[SipConnectionImpl]))
        
        //assertTrue(SdpHelper.isBlankSdp(desk.joinedTo.get.sdp))
        println(" desk is = " + desk )
	    System.err.println("desk should be disconnected" + SdpHelper.isBlankSdp(desk.asInstanceOf[SipConnectionImpl].sdp) ) //b2bServer.areTwoConnected(desk.asInstanceOf[SipConnection], )
	    latch.countDown
	}
 
	def runConn() = {
	    telcoServer.setDisconnectedCallback(disconnected)
	    cell 			= telcoServer.createConnection("9495550982", "7147579999")
	    desk   		    = telcoServer.createConnection("7147579999", "9495550982")
	    moviePhone1 	= telcoServer.createConnection("9497773456", "9495550982")
		cell.connect().run {
			 			println("cellphoneconnected, = " + cell)
                    	desk.connect().run {
                    						println("desk connecte, = " + desk)
                                            cell.join(desk).run { firstJoined() }
                                          }
                  }
	}

	def tryCall {
		val call1 = telcoServer.createConnection("", "")
		call1.connect().run { println("connected")}
	}
}

 


