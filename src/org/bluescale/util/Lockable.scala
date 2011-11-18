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

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.TimeUnit

trait Lockable {
	protected val myLock = new ReentrantLock()
  
	//def tryLock():Boolean = myLock.tryLock()
		
	def lock() = myLock.lock()
	
	def unlock() = myLock.unlock()
 
	def wrapLock(f: =>Unit) : Unit = {
		if ( myLock.tryLock(500,TimeUnit.MILLISECONDS) ) {
			f
			unlock()
		} else {
		    println(" CONCURRENCY EXCEPTION FOR " + this )
			throw new ConcurrencyException()
		}
    }

    def tryLock = if ( !myLock.tryLock() ) throw new ConcurrencyException()
}

class ConcurrencyException extends Exception
