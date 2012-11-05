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
import akka.dispatch.Future
import akka.dispatch.ExecutionContext
import java.util.concurrent.Executors

class SdpJoinable(sdp:Option[SessionDescription]) extends Joinable[SdpJoinable] {
	
	implicit val ec = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool)
	
    override def connectionState = CONNECTED() 
    
    override def joinedTo = None
    
    var mySdp:Option[SessionDescription] = sdp
    
    override def join[J <: Joinable[J]](c:J) = Future { (this,c) }

    def reconnect(sdp:SessionDescription) = Future { this }

    override def sdp : SessionDescription = { 
        mySdp match {
            case Some(x) => return x
            case None => throw new Exception("no SDP here")
        }
    }

    override def connect[J <: Joinable[_]](join:J, connectAnyMedia:Boolean) = Future { this }

    override def connect[J <: Joinable[_]](join:J) = Future { this }

    override def joinedMediaChange() = Unit
    
    def silence(f:()=>Unit) : Unit = f()

    override def unjoin() = Future { this }

    override def toString() =
        "SdpJoinable ......"
}

