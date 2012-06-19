package org.bluescale.util
import org.bluescale._

object DoAsync {
	implicit def bluefuture_to_doasync(f:BlueFuture[_]): DoAsync = 
	  new DoAsync(List[Foreachable[_]](f))
}
class DoAsync(val futures:List[Foreachable[_]]) {
  
	def ~> (future: Foreachable[_]): DoAsync = 
		new DoAsync(futures:+future)
    
	def ~> (f: =>Any): DoAsync =
		new DoAsync(futures:+ new { def foreach[Unit](callback: Unit=>Unit) = {f; callback; println("")}})
	
	/*
	def ~> (f: =>Unit): DoAsync =												  //or should this be callback()? passing something in? 
		new DoAsync(futures:+new { def foreach[Unit](callback: Unit=>Unit) = { f; callback; println("")}})  //passing in empty to the foreach since it doesn't mean anyhting
	*/
	def runLoop(li:List[Foreachable[_]]): ()=>Unit = 
        	return li.headOption match {
            	case Some(head) =>
            		()=> {
            			println("IN RUNLOOP")
            			head.foreach(t=> {
            				println("RECURSING HERE")
            				runLoop(li.drop(1))()
            			})
            		}
            	case None =>
            		()=> {}
        	}
    
	def run(): Unit = {
		println("in run, futures size = " + futures.size)
        runLoop(futures)()         
    }
	
	def ! : Unit = run()
    
}