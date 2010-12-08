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
package com.bss.ccxml.tags

import scala.xml._
import scala.collection.mutable.ListBuffer 
import scala.collection.immutable._
import org.mozilla.javascript.Context

object Parser { 
	
	def parseVars(nodes: NodeSeq) : Map[String, String] = {
		var map = Map[String,String]()
		val cx = Context.enter()
		//FIXME: make this faster, i'm sure theres a way to copy the scope data from one to another
        val scope = cx.initStandardObjects()
 
	    for (n <- nodes ) {
	    	val varName = (n \ "@name").text
	    	val expr = (n \ "@expr").text
	    	
	    	map +=varName->( if(expr!="") expr else null)
       } 
	   return map
	}
  
    def parseEventProcessor(node: NodeSeq): EventProcessor = {
		return new EventProcessor( parseTransitions(node), (node\"@statevariable").text)
    }
  
	private def parseTransitions(nodes: NodeSeq): Map[String,List[TransitionTag]] = {
		println("parseTransitions")

		var map = Map[String,List[TransitionTag]]()
		for (n <-(nodes \"transition")) {
			
		    val eventname = (n \ "@event").text
		    val iter = (n \ "_").elements
		    val transition = TransitionTag( (n \"@event").text, 
                                          		(n \ "@name").text, 
                                          		(n \"@state").text, 
                                          		parseActions( iter ) )
      
		   
		    if ( !map.contains(eventname) ) {
		    	 map+= eventname->List[TransitionTag](transition)
            }else {
		        val oldList = map(eventname)
            	map+=eventname->(oldList ::: List[TransitionTag](transition))
            }
		}
		return map 
	 }
	
    //some damn fancy recursive magic to happen here
  
  	private def parseActions(nodes:Iterator[Node]) : List[ActionTag] = {
	  var list = new ListBuffer[ActionTag]
	  
	  for (actionNode <- nodes) { 
		actionNode.label match {
	    	case "accept" => list+= Accept( (actionNode \ "@connectionid").text, 
                                          		(actionNode \"@hints" ).text)
	    
	    	case "createcall" => list+= parseCreateCall(actionNode)
	    	 
	    	case "disconnect" => list+= Disconnect((actionNode \ "@connectionid").text,
	    											   (actionNode \ "@reason").text,
	    											   (actionNode \ "@hints").text)
      
	    	case "join" => list+= parseJoin(actionNode)
	    	
	    	case "log" => list+= new LogAction( (actionNode \ "@expr").text )
      
	    	case "fetch" => list+= parseFetch(actionNode)
	    	
	    	case "goto" => list+= new GotoAction( (actionNode \"@fetchid").text)
      
	    	case "exit" =>list+= new Exit() 
      
	    	case "reject" => list+= new Reject((actionNode \ "@connectionid").text,
                                         		(actionNode \ "@dest").text,
	    										(actionNode \ "@reason").text,
	    										(actionNode \ "@hints").text)
      
	    	case "if" => list+= handleIf(actionNode, nodes) 
	    	
	    	case "elseif" => return list.toList
	    	
	    	case "else" => return list.toList
      
	    	case "assign" => list+= new AssignAction((actionNode \ "@name").text, (actionNode \"@expr").text ) 
		}
	  }
	  return list.toList
	}
   
   private def handleIf(ifNode:Node, nodes:Iterator[Node]): IfAction = {
     val ifIter = (ifNode \ "_").elements 
     val ifConditional = new ConditionalChunk((ifNode \"@cond").text, parseActions(ifIter) )
     val ifAction = new IfAction( ifConditional, handleElseIf(ifIter), parseActions(ifIter) )
     return ifAction
   }
  
   //TODO: implement ElseIFf....
   private def handleElseIf(nodes:Iterator[Node]):List[ConditionalChunk] = {
     return null
   }
   
   private def parseCreateCall(node:Node):CreateCall = {
		return new CreateCall( ( node \ "@dest").text,
                               ( node \ "@connectionid").text, 
                               (node \ "@aai").text,
                               (node \ "@callerid").text,
                               (node \ "@hints").text,
                               (node \ "@timeout").text,
                               (node \ "@joinid").text,
                               (node \ "@joindirection").text)
   }
   
   
   def parseFetch(n: Node):FetchAction = {
    return new FetchAction((n\"@next").text,
                           (n\"@type").text,
                           (n\"@namelist").text.split(" "),
                           (n\"@method").text,
                           (n\"@fetchid").text)
                               
    }
   
   def parseJoin(n: Node):Join = {
	 return new Join((n \"@id1").text, 
     				 (n \"@id2").text,
     				 (n \ "@duplex").text )
   }

	
}
