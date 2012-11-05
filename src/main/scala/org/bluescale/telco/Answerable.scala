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

import org.bluescale.telco.api.Joinable
import org.bluescale._
import org.bluescale.util.BlueFuture
import akka.dispatch.Future
class DIRECTION

case class INCOMING extends DIRECTION

case class OUTGOING extends DIRECTION

protected[telco] trait Answerable[T] {
    
  	def direction: DIRECTION //should probably be in SipConnection

 	def accept(): Future[T] 
  
    def accept[J <: Joinable[J]](toJoin: J): Future[T] 

 	def reject() : Future[T]

    def ring(): Unit

 	def ring(toJoin:Joinable[_]) : Unit //sends a 180 to the phone
 	
 	var incomingCancelCallback: Option[(T)=>Unit] = None
    
}


