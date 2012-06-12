package org.bluescale.telco.jainsip.unittest

import org.bluescale.telco.jainsip._
import org.bluescale.telco.api._
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter

class FunTestHelper extends FunSuite with BeforeAndAfter {
	
	val telcoServer  = new SipTelcoServer( "127.0.0.1", 4000, "127.0.0.1", 4001) 
	val b2bServer = new B2BServer( "127.0.0.1", 4001, "127.0.0.1", 4000)
  
	
	def tryAssertEq(a:Any, b:Any): Unit = {
		for (x <- 1 to 5) 
			(a == b) match {
				case true => 
					assert(a === b)
				  	return
				case false => 
				  Thread.sleep(500)
			}
		throw new Exception("possible error here, "+ a +" should be equal to " + b)
	}
	
	before {
		b2bServer.start()
		telcoServer.start()
	}
  
	after {
		telcoServer.stop()
		b2bServer.stop()
	}
}
