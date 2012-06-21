package org.bluescale.util.test

import java.util.concurrent.CountDownLatch
import org.scalatest.FunSuite
import scala.actors.Actor
import scala.actors.Actor._
import org.bluescale._
import org.bluescale.util._
import org.bluescale.util.DoAsync._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DoAsyncTest extends FunSuite {


	protected def delay(f: =>Unit) = act ! DelayedFunction(()=>f) 
  
	private val act:Actor = actor {
		loop { 
			react {
				case delayedfunction:DelayedFunction =>
					try {
						println(" OK RUNNING IN THE ACTOR")
						delayedfunction.f()
						println("DONE HERE")
					
					} catch {
					  case ex:Exception => println("error running ordredfunction = " + ex)
					}
			}
	  
		}
	}
    
	test("BlueFuture and DoAsync Test") {
	  /*
		val seqLatch = new CountDownLatch(1)
		val finishLatch = new CountDownLatch(1)
		var test = ""
        //the idea is here the callbacks woiuld occur in a separate thread, happening when network events return. We could start downlaoding something,
        //asynchronously, in anohter thread, execute the callback...
        def connect1() = BlueFuture(callback => { println("yay"); delay(callback())})
        def connect2() = BlueFuture(callback => { seqLatch.await(); callback()})  //this happens in another thread so there isn't a deadlock here!
        def connect3() = BlueFuture(callback => { test = "success"; delay(callback())})
        
        connect1() ~>
		println("ok i'm here")
        connect2() ~> 
        println("the first to futures have executed...") ~>
        connect3() ~>
        finishLatch.countDown() run()
        
        Thread.sleep(1000)
        seqLatch.countDown();
        finishLatch.await()
        assert(test === "success")
        */
    }
	
	test("Test for comprehension") {
		/*
		val seqLatch = new CountDownLatch(1)
		val finishLatch = new CountDownLatch(1)
		var test = ""
		def connect1() = BlueFuture(callback => { println("yay"); delay(callback())})
        def connect2() = BlueFuture(callback => { seqLatch.await(); callback()})  //this happens in another thread so there isn't a deadlock here!
        def connect3() = BlueFuture(callback => { test = "success"; delay(callback())})
        
        
        for(_ <- connect1();
        	//_ = println("yay");
        	_ <- connect2()) {
        	println("finished with the connects")
        }	
        */
	  
	}
	

}

case class DelayedFunction(f:()=>Unit)
