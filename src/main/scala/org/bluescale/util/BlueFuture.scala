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
* Please contact us at www.BlueScale.org
*
*/

package org.bluescale.util
import org.bluescale._

class BlueFuture[T](callback:(T=>_)=>Unit) {
 
	def foreach[U](f:T=>U): Unit = {
		println("FOREACH HERE for " + this)
		callback(f)
	}

	def run(f:T=>Unit) = foreach(f)

}

object BlueFuture {
	def apply[T](callback:(T=>_)=>Unit) =
	  new BlueFuture[T](callback)
}



object DoAsync {
	implicit def bluefuture_to_doasync(f:BlueFuture[_]): DoAsync = new DoAsync(List[Foreachable[_]](f))
}
class DoAsync(val futures:List[Foreachable[_]]) {
  
	def ~> (future: Foreachable[_]): DoAsync = 
		new DoAsync(futures:+future)
    
	def ~> (f: =>Unit): DoAsync = 
		//val foreachable:Foreachable[_] = new { def foreach[Unit](callback: String=>Unit) = { f; callback; println("")}}
		new DoAsync(futures:+new { def foreach[Unit](callback: String=>Unit) = { f; callback(""); println("")}})  //passing in empty to the foreach since it doesn't mean anyhting
	
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
    
}

