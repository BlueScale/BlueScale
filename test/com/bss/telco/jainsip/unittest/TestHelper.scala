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
import org.bluescale.telco.jainsip._
import org.bluescale.telco.api._
import org.apache.log4j.BasicConfigurator

trait TestHelper extends junit.framework.TestCase {
    //println(".......")	
	val telcoServer  = new SipTelcoServer( "127.0.0.1", 4000, "127.0.0.1", 4001) 
	val b2bServer = new B2BServer( "127.0.0.1", 4001, "127.0.0.1", 4000)
 
 	//BasicConfigurator.configure()

	@Before
   	override def setUp() {
   		b2bServer.start()
		telcoServer.start()
	}	
	
    @After
	override def tearDown() {
        telcoServer.stop()
        b2bServer.stop()
    }
	
}
