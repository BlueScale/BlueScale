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
package com.bss.telco

import javax.sdp.SessionDescription

protected[telco] trait Joinable[T] {

  	var joinedTo:Option[Joinable[_]] = None
  	
	def join(connection:Joinable[_], f:()=>Unit)

    protected[telco] def reconnect(sdp:SessionDescription, f:()=>Unit)

    def sdp:SessionDescription

    protected[telco] def silence(f:()=>Unit)

    protected[telco] def unjoin(f:()=>Unit) //TODO: find out why protected isn't working here?  I'm accessing it from a subclass...

    var unjoinCallback:Option[(T)=>Unit] = None
}


trait UnjoinReason

case class HoldUnjoin extends UnjoinReason

case class DisconnectUnjoin extends UnjoinReason
