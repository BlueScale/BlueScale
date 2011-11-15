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
package org.bluescale.blueml

import scala.actors.Actor
import org.bluescale.util._
import java.util.concurrent.ConcurrentHashMap

protected class SequentialWebPoster(url:String) extends Actor {

    start()
    
    def post(map:Map[String,String]) : String = 
        (this !? map).toString()
    
  
    def act() {
        loop {
		    react {
		  
		        case map:Map[String,String] =>
		          	val s = WebUtil.postToUrl(url, map) 
		            reply(s)
		  
		        case _ => 
		            println("Error")
		            reply("") 
		    } 
	    }
    }
}

object SequentialWebPoster {
    val urlMap = new ConcurrentHashMap[String,SequentialWebPoster]()

    def postToUrl(url:String, map:Map[String,String]) : String = {
        if (!urlMap.contains(url))
            urlMap.putIfAbsent(url, new SequentialWebPoster(url))
        return  urlMap.get(url).post(map)
    }
}
