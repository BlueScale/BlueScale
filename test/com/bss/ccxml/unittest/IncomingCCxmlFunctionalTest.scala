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
package com.bss.ccxml.unittest


import org.junit._
import Assert._
import com.bss.telco.jainsip.test.B2BServer
import com.bss.telco.jainsip._
import com.bss.telco.api._
import com.bss.ccxml.Server
import com.bss.ccxml.event._
import com.bss.ccxml.tags._
import scala.xml._
import com.bss.telco._
import java.util.concurrent.CountDownLatch
import scala.collection.mutable.ListBuffer


class IncomingCCxmlFunctionalTest extends CCxmlHelper {

	val logList = new ListBuffer[String]
 	val latch = new CountDownLatch(1)

  	server.log = (s:String) => {
		logList+=s
		println("+++++++" + s + "+++++++++")
		if (logList.size > 2) latch.countDown() //number of print statements
  	}
  			
 
	override def getRootDoc()=
		new CCXMLDoc("name", XML.loadFile("resources/ccxmlTests/pickupHangup.xml"));
				
	
	def testIncoming() = {
		
		val conn = b2bServer.b2bTelcoServer.createConnection("7147773456", "7145555555")
		conn.connect( ()=>
						println("Connected!")
						)
	
		latch.await()	
		assertTrue( conn.connectionState == UNCONNECTED())
	}
}
object IncomingCCxmlFunctionalTest {
	def main(args:Array[String]) {
	val test = new IncomingCCxmlFunctionalTest()
		test.setUp()

		test.testIncoming()
		test.latch.await()
		test.tearDown()
	}	 
}

 
