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
package com.bss.ccxml.tags;
 
import scala.collection.immutable.Map 
import scala.xml.{MetaData, Node}
import scala.xml._


class CCXMLDoc(val filename:String, val data: Elem ) {
               
	val vars = Parser.parseVars(data \ "var")
	val eventProcessor : EventProcessor =  Parser.parseEventProcessor(data \ "eventprocessor")
} 

case class EventProcessor(val transitions : Map[String, List[TransitionTag]], 
						  val statevar:String)
		

