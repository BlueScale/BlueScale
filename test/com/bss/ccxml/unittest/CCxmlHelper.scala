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

import com.bss.ccxml._
import org.junit._
import Assert._
import com.bss.telco.jainsip._
import com.bss.telco.api._
import com.bss.ccxml.Server
import com.bss.ccxml.tags._
import org.apache.log4j.BasicConfigurator
import com.bss.server.ConfigParser

trait CCxmlHelper extends junit.framework.TestCase {
 	val config = new ConfigParser("resources/BlueScaleConfig.Sample.xml")
		
 	val server = new Server(getRootDoc(), config.localIp(), config.localPort(), config.destIp(), config.destPort() )
	
	val b2bServer = new B2BServer( "127.0.0.1", 4001, "127.0.0.1", 4000)

	@Before
   	override def setUp() {
   	
   		b2bServer.start()
		server.start()
	}	
	
    @After
	override def tearDown() {
        server.stop()
        b2bServer.stop()
    }

    def getRootDoc() : CCXMLDoc
	

}
