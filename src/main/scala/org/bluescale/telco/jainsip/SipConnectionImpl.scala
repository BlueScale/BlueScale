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

package org.bluescale.telco.jainsip

import org.bluescale.telco.jainsip._
import org.bluescale.telco._
import org.bluescale.telco.api._
import scala.collection.immutable.Map
import javax.sip.header._
import javax.sip._
import javax.sdp.SessionDescription
import javax.sip.message._
import org.bluescale.util._
import org.bluescale.telco.Types

class SipConnectionImpl(var connid:String,
                        val to:String,
                        val from:String,
                        val dir:DIRECTION,
                        val telco:SipTelcoServer,
                        val disconnectOnUnjoin:Boolean)
    extends UACJainSipConnection 
    with UASJainSipConnection {

    def addConnection() =
        telco.addConnection(this)

    def removeConnection() =
        telco.removeConnection(this)

    def origin = from

    def destination = to

    def direction = dir

    override def toString() = 
	    "SipConnectionImpl" + direction + " TO:"+destination + " State = " + _state + " Hashcode = " + hashCode
    
}
