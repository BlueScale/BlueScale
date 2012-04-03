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

package org.bluescale.util

import scala.actors.Actor
import scala.actors.Actor._ 

trait Actorable {

	protected def orderedexec(f: =>Unit) = act ! OrderedFunction(()=>f) 
  
  
	private val act:Actor = actor {
		loop { 
			react {
				case orderedfunction:OrderedFunction =>
					try {
						orderedfunction.f()
					
					} catch {
					  case ex:Exception => println("error running ordredfunction = " + ex)
					}
			}
	  
		}
	}
}

case class OrderedFunction(f:()=>Unit)