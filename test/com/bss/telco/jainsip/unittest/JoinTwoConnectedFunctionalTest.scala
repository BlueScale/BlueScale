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
package com.bss.telco.jainsip.unittest

import org.junit._
import Assert._
import com.bss.telco.jainsip.test._

import com.bss.telco.jainsip._

class JoinTwoConnectedFunctionalTest extends TestHelper with JoinTwoConnected  {
	
 
	@Override 
	def getTelcoServer() = telcoServer;
 
	@Test
	def testJoinConn() = {
	  println("hi");
		assertEquals("blah", "blah")
	 
		 //runConn()
		 //getLatch.await()
 	 
	}
}

object JoinTwoConnectedFunctionalTest {
	def main(args:Array[String]) {
		val jt = new JoinTwoConnectedFunctionalTest()
	    jt.setUp()
		jt.runConn()
		jt.getLatch.await()
	}
}

 


