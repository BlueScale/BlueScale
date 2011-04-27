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
package com.bss.telco.jainsip.unittest.load
 

import org.junit._
import Assert._
import java.util._

import com.bss.telco.jainsip._
import scala.actors.Actor
import com.bss.telco.jainsip.unittest._
import scala.actors.Actor._
import com.bss.ccxml.event._
import java.util.concurrent.atomic.AtomicInteger

import java.util.concurrent.CountDownLatch

class ConccurentSimpleCall  extends junit.framework.TestCase  {
	
	val telcoServer  = new SipTelcoServer( "127.0.0.1", 4000, "127.0.0.1", 4001) 
	val b2bServer = new B2BServer( "127.0.0.1", 4001, "127.0.0.1", 4000)
 
	b2bServer.start()
	telcoServer.start()
	
	val iterations = 1000
	val finalLatch =  new CountDownLatch(iterations)

	val counter = Some(new AtomicInteger)
 
	@Test
	def testCPS() = {
	  	for (i <- 1 to iterations) {
	  		new ParallelCaller() ! ""+i
	   	}
	  	Thread.sleep(5000*4)
	  	//finalLatch.await()
	  	System.out.println( counter.get);
	}
 
	class ParallelCaller extends Actor with SimpleCall {
	  
	  override def getB2BServer() = b2bServer
	  override def getTelcoServer() = telcoServer;
	  
	  override def getCounter = counter
	   
	   start()
   
	  
	   def act() {
		  loop {  react {
		    case str:String => 
		    				   try {
		    					   runConn(str)
		    					   //getLatch.await() 
		    				   } catch {
		    				     case ex:Exception => println(ex) 
		    				   }
		    				     
		    				   
		    				   finalLatch.countDown()
		    				   this.exit
			  }
            }
	  	}
	}
}

 
