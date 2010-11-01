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
  
    def execute(f:()=>Unit) = act ! f

}
