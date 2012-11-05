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
import org.bluescale._
import org.bluescale.util.BlueFuture
import akka.dispatch.Future

//typed so our callback can return a concrete class
trait Joinable[T <: Joinable[T]] {
	
	var _joinedTo:Option[Joinable[_]] = None
  
	def joinedTo = _joinedTo
  	
    var unjoinCallback:Option[(Joinable[_],T)=>Unit] = None
	
	def join[J <:Joinable[J]](connection:J): Future[(T,J)]

    def sdp:SessionDescription

    def connectionState:ConnectionState //Possibly not needed here...

    protected[telco] def connect[J <: Joinable[J]](join:J): Future[T]

    protected[telco] def connect[J <: Joinable[J]](join:J, connectAnyMedia:Boolean): Future[T]//when do ew not want to connect with any media?
    
    //protected[telco] def onConnect(f:()=>Unit)

    protected[telco] def unjoin(): Future[T] //TODO: find out why protected isn't working here?  I'm accessing it from a subclass...

    def joinedMediaChange() // called by the joined class when media changes. 

}

