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
package com.bss.ccxml

import scala.actors.Actor
import scala.xml._
import scala.actors.Actor._
import com.bss.ccxml.event._
import com.bss.server.Launcher
import com.bss.ccxml.tags._ 

class FetchLoader(server:Server, session:Session) extends Actor {

  start()
  
  var xml = null
  
  def act() {
      loop {
		  react {
		  
		  case fetch:FetchAction =>
		    	//check the cache, if its there, we're done!
		    	if ( server.ccxmlMap.contains(fetch.next) ) { 
		    		val doc = server.ccxmlMap(fetch.next)
		    		session.fetchedDocMap+= fetch.fetchId->doc
		    	} else {
		    		//if not, load it, add to cache, add to session association, then we're done!
		    		val doc = Launcher.getCCXML(fetch.next)
		    		server.ccxmlMap+= fetch.next->doc
		    		session.fetchedDocMap+=fetch.fetchId->doc		    		
		    	}
		    	fireLoadedEvent(fetch)
		    	exit()//kept around just long enough to asyncronoushly fetch a doc. 
		  
		  case _ => System.err.println("ERROR") 
		} 
	  }
  }
  
  private def fireLoadedEvent(fetch:FetchAction) {
	  fetch.fetchId
	  val fetchDone = new FetchDone(fetch.fetchId, 
                                  fetch.next, 
                                  "", 
                                  "???FetchLoader.scala?", 
                                  "ccxml" )
	  session ! fetchDone
  }
 
}
