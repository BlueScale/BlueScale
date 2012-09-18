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


package org.bluescale.telco

import javax.sdp.MediaDescription
import javax.sdp.SdpException
import javax.sdp.SdpFactory
import javax.sdp.SdpParseException
import javax.sdp.SessionDescription
import javax.sdp.MediaDescription
 
import java.net.InetAddress
import javax.sdp.MediaDescription
import javax.sdp.SdpException
import javax.sdp.SdpFactory
import javax.sdp.SdpParseException
import javax.sdp.SessionDescription
import javax.sdp.MediaDescription
import org.bluescale.util.BlueFuture
import org.bluescale.telco.api._

class SdpJoinable(sdp:Option[SessionDescription]) extends Joinable[SdpJoinable] {

    override def connectionState = CONNECTED() 
    
    override def joinedTo = None
    
    var mySdp:Option[SessionDescription] = sdp
    
    override def join(c:Joinable[_]) = BlueFuture(callback => callback()) 

    def reconnect(sdp:SessionDescription) = BlueFuture(callback => Unit) // should we execute callback

    override def sdp : SessionDescription = { 
        mySdp match {
            case Some(x) => return x
            case None => throw new Exception("no SDP here")
        }
    }

    override def connect(join:Joinable[_], connectAnyMedia:Boolean) = BlueFuture(callback => callback())

    override def connect(join:Joinable[_]) = BlueFuture(callback => callback())

    override def joinedMediaChange() = Unit
    
    def silence(f:()=>Unit) : Unit = f()

    override def unjoin() = BlueFuture( callback => callback() ) 

    override def toString() =
        "SdpJoinable ......"
}

