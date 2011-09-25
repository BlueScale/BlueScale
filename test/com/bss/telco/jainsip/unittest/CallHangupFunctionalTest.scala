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
import org.bluescale.telco.jainsip._
import org.bluescale.telco.api._

class CallHangupFunctionalTest extends TestHelper with SimpleCall  {

 
	def getCounter = None
	
	@Override 
	def getTelcoServer() = telcoServer.asInstanceOf[TelcoServer];

	@Override
	def getB2BServer() = b2bServer
 
	@Test
	def testSimpleConn() = {
		println("running");
		assertEquals("blah", "blah")
		runConn()

		getLatch.await()
	}
}

object CallHangupFunctionalTest {
	def main(args:Array[String]) {
		println("running")
		val ch = new CallHangupFunctionalTest()
		ch.setUp()
		ch.runConn()
		ch.getLatch.await()
	}
}

 


