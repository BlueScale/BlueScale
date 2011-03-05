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
package com.bss.util

import scala.actors.Actor
import scala.actors.Actor._ 


trait OrderedExecutable
    extends LogHelper {

  
   /*
    * Originally written to multithread the network stack a bit better
    * 
    */
    private val act:Actor = actor {
		loop { react {
    	   case f:( ()=>Unit ) => try {
    	            				f()
    	   						  } catch {
    	   						  	case ex:Exception => debug("damn, exception, ex = " + ex)
    	   						  						 ex.printStackTrace()
    	   						  						 //telco.removeConnection(this) CAN ERRORS HAPPEN like this? DO WE NEED THIS FUNCTIONALIT?
    	   						  						 //telco.fireFailure(this)
    	   						  }
            
         	  case _ => error("Something was sent to the execute method of " + this + " That shouldn't have been!") 
    	 }}  
     }
 
    //typesafe messaging? 
    def execute(f:()=>Unit) = act ! f

}
