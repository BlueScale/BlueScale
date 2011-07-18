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
import scala.actors.Actor._
import scala.collection.mutable._

import org.mozilla.javascript.Context
import org.mozilla.javascript.EvaluatorException
import org.mozilla.javascript.JavaScriptException
import org.mozilla.javascript.EcmaError
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.NativeObject
 
import com.bss.ccxml.event._ 
import com.bss.telco.api._
import com.bss.ccxml.tags._

class Session(val server:Server, val id: Int) extends Actor { 
    
  	val fetchedDocMap = new HashMap[String, CCXMLDoc]
  
  	val varMap = new HashMap[String,String]
  
  	//this might be a hack since we're ditching the OG context
    val scope =  Context.enter().initStandardObjects() 
    
  	var activeDoc:Option[CCXMLDoc] = None 
   
    var context:Option[Context]    = None
    
   	start()     
      
    def act() {
    	loop { 
    	  react {		 
    		  case event:CCXMLEvent => runActions(getTransitionActions(event), event)
       		
    		  case _ => System.err.println("ERRROR didn't get a controlEvent?")
       	  }
    	}
    }
    
    def setActiveDoc(doc:CCXMLDoc ) {
    	this.activeDoc = Some(doc)
    	setInitialScopeData()
    	this ! (new Loaded(id.toString(), "","", "", "ccxml"))
    	//fire doc loaded!
    }
    
    def setInitialScopeData() {
    	context = Some(Context.enter())
    	activeDoc.foreach( doc =>     			
    				for ( (key, varval) <-doc.vars) {
    					var str = "var " + key 
    					if ( varval != null ) str += " = " + varval
    					evalScript(str)
    				}
    		)			
    }	
  
    
    private def runActions(actions:Option[List[ActionTag]],event:CCXMLEvent) {
        actions.foreach( 
    	for (action <- _ ) {
    		action match{
    		  case t:TelecomAction => handleTelecomAction(t, event)
    		  case c:ActionTag => handleControlAction(c, event)
    		}
    	})
    }
        
    private def getTransitionActions(event:CCXMLEvent): Option[List[ActionTag]] = {
    	val ad = activeDoc.get
    
       	if ( !((activeDoc.get).eventProcessor.transitions.contains(event.name)) ) 
       		return None
        
    	context = Some(Context.enter()) //FIRST TIME THIS IS CALLED FROM AN EVENT! don't need to do it again
    
    	val transitions = activeDoc.get.eventProcessor.transitions(event.name)
    	for (transition <- transitions) {

    		val varName = activeDoc.get.eventProcessor.statevar 
    		if ( transition.state == null || transition.state == "") { 
    			 setEventObject(event, transition)
    			 return Some(transition.actions) 
    			 
    		}
      
    		val appstate= context.get.evaluateString(scope, varName, "<cmd>", 1, null)
    		//println("		not null and now matching! varName =" + varName + " | state = " + transition.state + " | value = "+appstate)
    		    		
    		val stateMatch = context.get.evaluateString(scope, varName + " == " + transition.state, "<cmd>", 1, null)
    		if ( stateMatch.toString() == "true") { 
    			setEventObject(event, transition)
    			return Some(transition.actions)
    		}  
    	}
    	return None  
     }
    
    
    private def evalScript(str:String):String = { 
    	if (str == null || str == "") return null 
    	val c = Context.enter
    	return c.evaluateString(scope, str+"", "<cmd>", 1, null).toString()
     	//return context.get.evaluateString(scope, str+"", "<cmd>", 1, null).toString()		
    }
      
    private def setEventObject(event:CCXMLEvent, transition:TransitionTag) {
    	if ( transition.name == null || transition.name == "" ) return
    	event match {
    	  case connEvent:ConnectionEvent =>
    	    evalScript( transition.name + " = new Object()")
    	    evalScript(transition.name + ".connectionid = '"+connEvent.connection.connectionid+"'")
    	    evalScript(transition.name + ".state = '"+connEvent.connection.connectionState +"'")
    	    
    	  case _ => System.err.println("got an event we don't know about in setEventObject")
    	}
    }
   
    private def setVars(a:String, b:String) = 
       	StrOption(a).foreach( (s:String)=> {
       				println("var " + a + " = " + b)
       				evalScript("var " + a + " = " + b) })
    
    
      
    private def handleIf(ifAction:IfAction, event:CCXMLEvent) { 
    	val v =  evalScript(ifAction.ifActions.condition) 
    	if ( v.toString() == "true" ) {
	      		runActions(Some(ifAction.ifActions.successActions), event)
	      		return
       	} else if (ifAction.elseIfActions != null) {
	    
       		for ( val actions <- ifAction.elseIfActions ) {
       			//FIXME HERE
       		}
       		return
       	} 
    	runActions(Some(ifAction.elseActions), event)
    	 	  
  	}
    
    private def findConnId(id:Option[String], event:CCXMLEvent) = 
    	id match {
	   		case None => event match {
	   			case connEvent:ConnectionEvent => connEvent.connectionid
	   			case _ => "" //System.err.println("ERROR") //TODO: throw an exception?	 
	   		}
	   		case Some(id) => evalScript(id) 
      	} 
    
    
   private def StrOption(s:String) =
	  if ( s.equals("") ) 
	  	   None
	   else
		   Option(s)
  
  	private def handleTelecomAction(action:TelecomAction, event:CCXMLEvent) =  		 
  	  	action match {
      		case accept:Accept =>   		val connId = findConnId(StrOption(accept.connectionid), event)
      									    val conn = server.findConnection( connId )
      										conn.accept(()=> this ! new ConnectionConnected(conn))
      		  					 
      	
      		case reject:Reject =>   		val conn = server.findConnection( evalScript(reject.connectionid) )
      										conn.reject( ()=> this ! new ConnectionDisconnected("Rejected!", conn) )
      	  							      	  
      		case disconnect:Disconnect => 	val connId = findConnId(StrOption(disconnect.connectionid), event)
      										val conn = server.findConnection( connId )
      									  	conn.disconnect(()=>{ this ! new ConnectionDisconnected("requested", conn)})
      			
      		case createCall:CreateCall => 	val conn = server.createConnection( createCall.dest,
      																			createCall.callerid, 
    																			(c:SipConnection)=> this ! new ConnectionDisconnected("hangup", c)  )
      										conn.connect(()=>{  
      															System.out.println("connId = " + createCall.connectionId + " | conn.connectinoid = " + conn.connectionid)
      															setVars(createCall.connectionId, "'"+ conn.connectionid + "'")
      															this ! new ConnectionConnected(conn) 
      														})
      										               
      		case join:Join => 				handleJoin(join)
      		  					
        	
  	  	}
  	
  
  	private def handleJoin(join:Join) = {
  		val call1 = server.findConnection(evalScript(join.id1))
  		val call2 = server.findConnection(evalScript(join.id2))
  		join.duplex match { 
  			case "" | "full" => call1.join(call2, ()=>{this ! new ConferenceJoined(call1.connectionid, call2.connectionid) })
  			
  			case "half" => println("NOT YET SUPPORTED")
  		}
  		
  	}
  	private def handleControlAction(action:ActionTag, event:CCXMLEvent) =
  		action match {
	     
	    	case logAction: LogAction =>  	server.log(logAction.expr)  
	    
	    	case fetch: FetchAction  => 	new FetchLoader(server, this) ! fetch
      
	    	case goto: GotoAction 	 => 	val doc  = fetchedDocMap( goto.fetchId )
	    	  								if ( doc == null ) System.err.println("blah its null")
	    	  								setActiveDoc(doc)
             
	    	case ifAction:IfAction   => 	handleIf(ifAction, event)						
             
	    	case exit: Exit 		 => 	this.exit("exit tag")
      
	    	case assign: AssignAction=> 	setVars( assign.name, assign.expr)
      
		}

}
