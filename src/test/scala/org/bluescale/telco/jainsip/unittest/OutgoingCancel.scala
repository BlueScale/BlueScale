package org.bluescale.telco.jainsip.unittest

import org.bluescale.telco.jainsip._

import org.bluescale.telco.api._
import org.bluescale.telco._
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.bluescale.util.ForUnitWrap._

@RunWith(classOf[JUnitRunner])
class OutgoingCancel extends FunTestHelper {
	
	
	var latch:CountDownLatch = null
  
	def getLatch = latch
	
	val destNumber = "9495557777"
	
	test("Simple outgoing cancel") {
		latch = new CountDownLatch(1)
		//runConn()
		b2bServer.addIgnore(destNumber)
		connectAndCancel()
		println("finished simple outgoing cancel")
	}
	
	test("Simple outoging Cancel with rejected response") {
		latch = new CountDownLatch(1)
		
		b2bServer.addReject(destNumber)
		connectAndCancel()
		println("finished cancel rejected")
	}
	
	def connectAndCancel() {
	 	
 		val alice = telcoServer.createConnection(destNumber, "4445556666")

 		alice.connect().foreach( alice => {
 			println("HOW DID THIS HAPPEN")
			assert(false) //shouldn't happen, we're ignoring this!
		})
		Thread.sleep(500)
		alice.cancel().foreach( alice => {
			println("cancelled!")
			latch.countDown()
		})
		
		val result = getLatch.await(5,TimeUnit.SECONDS)
		assert(result) 
	}
}