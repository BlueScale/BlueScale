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
package com.bss.telco.api

import com.bss.telco._


trait TelcoServer {

   	def createConnection(destPhone:String, callerId:String) : SipConnection //should switch this to take an outoging.

   	def createConnection(destPhone:String, callerId:String, disconnectOnUnjoin:Boolean) : SipConnection
  	  
   	def findConnection(connectionId:String) : SipConnection; 
  
  	def setFailureCallback(f:(SipConnection) => Unit)
  	
	def setIncomingCallback(f:(SipConnection) => Unit)
  	
  	def setDisconnectedCallback(f:(SipConnection) => Unit)

  	def setUnjoinCallback(f:(Joinable[_],SipConnection) => Unit)
  
  	def start()

  	def stop()

  	def areTwoConnected(conn1:SipConnection, conn2:SipConnection) : Boolean
  
}
