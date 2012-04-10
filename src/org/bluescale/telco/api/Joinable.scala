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
package org.bluescale.telco.api

import javax.sdp.SessionDescription
import org.bluescale.telco.Connectable
import org.bluescale.util.BlueFuture


//typed so our callback can return a concrete class
trait Joinable[T] {

	def joinedTo:Option[Joinable[_]]
  	
    var unjoinCallback:Option[(Joinable[_],T)=>Unit] = None
	
	def join(connection:Joinable[_]): BlueFuture[String]

    def sdp:SessionDescription

    def connectionState:ConnectionState //Possibly not needed here...

    protected[telco] def connect(join:Joinable[_]): BlueFuture[String]

    protected[telco] def connect(join:Joinable[_], connectAnyMedia:Boolean): BlueFuture[String]//when do ew not want to connect with any media?
    
    //protected[telco] def onConnect(f:()=>Unit)

    protected[telco] def unjoin(): BlueFuture[String] //TODO: find out why protected isn't working here?  I'm accessing it from a subclass...

    def joinedMediaChange() // called by the joined class when media changes. 

}


trait UnjoinReason

case class HoldUnjoin extends UnjoinReason

case class DisconnectUnjoin extends UnjoinReason
