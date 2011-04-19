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

/*
package com.bss.telco.media.jmf

import com.bss.telco.api._
import com.bss.telco._

class JmfMediaServer(listeningIp:String,
					 port:String) extends MediaServer {

	var failureCallback:Option[MediaConnection=>Unit] = None

	override def ip = listeningIp

	override def createMediaConnection(file:String) :MediaConnection = new JmfMediaConnection(file, this) 
	
	override def createMediaConnection(file:String, f:(MediaConnection=>Unit) ) : MediaConnection  = {
		this.failureCallback = Some(f)
		return createMediaConnection(file)
    }

	override def findConnection(id:String) = {
		throw new Exception("NOT YET SUPPORTED")
	}
}
*/
