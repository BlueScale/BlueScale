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

trait ConnectionState {}

class HASMEDIA extends ConnectionState

case class CONNECTED() extends HASMEDIA

case class SILENCED() extends ConnectionState

case class UNCONNECTED() extends ConnectionState

case class PROGRESSING() extends ConnectionState

case class ALERTING() extends ConnectionState

case class CANCELED() extends ConnectionState

case class RINGING() extends HASMEDIA 

//DO WE WANT A JOINED STATE?
