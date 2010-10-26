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
package com.bss.ccxml.event

import com.bss.ccxml.tags.CCXMLDoc;

abstract class CCXMLEvent(val name:String)

abstract class ControlEvent(name:String) extends CCXMLEvent(name)


class EcmaObject()
 

class LoadDocument(val doc:CCXMLDoc) extends ControlEvent("bss.loaddocevent")

class Loaded( val sessionId:String, 
              val parent:String, 
              val eventId:String,
			  val eventSource:String,
			  val eventSourceType:String) 
	  extends ControlEvent("ccxml.loaded") {
}
class FetchDone(val fetchId:String,
                 val uri:String,
                 val eventId:String,
                 val eventSource:String,
                 val eventSourceType:String) 
       extends ControlEvent("fetch.done") {}
   
