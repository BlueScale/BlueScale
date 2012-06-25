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

class BlueFuture[T](myval:T,callback:(()=>_)=>Unit) {
  
	def foreach[U](f:T=>U): Unit = {
		callback(()=>f(myval))
	}
	
	def run(f: =>Any): Unit = 
		callback( { ()=> f})
	
}

object BlueFuture {
	import org.bluescale._
	
	
    //new { def foreach[String](callback: String=>Unit) = { f;println("")}
	
	def apply(callback:(()=>_)=>Unit) =
	  new BlueFuture[Unit](Unit,callback)
}

