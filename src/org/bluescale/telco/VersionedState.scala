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
package org.bluescale.telco

import org.bluescale.telco.api._

trait VersionedState {
    val version:String
	def getState: ConnectionState
}

class VERSIONED_HASMEDIA(val version:String) extends VersionedState {
    override def getState = new HASMEDIA()
}

case class VERSIONED_CONNECTED(v:String) extends VERSIONED_HASMEDIA(v) {
	override def getState = CONNECTED()
}

case class VERSIONED_HOLD(val version:String) extends VersionedState {
    override def getState = HOLD()
}

case class VERSIONED_UNCONNECTED(val version:String) extends VersionedState {
	override def getState = UNCONNECTED()
}

case class VERSIONED_PROGRESSING(val version:String) extends VersionedState {
	override def getState = PROGRESSING()
}

case class VERSIONED_CANCELED(val version:String) extends VersionedState {
    override def getState = CANCELED()
}

case class VERSIONED_RINGING(v:String) extends VERSIONED_HASMEDIA(v) {
   override def getState = RINGING()
}



