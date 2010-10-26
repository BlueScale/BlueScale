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


 
// Tags related to the control stucture of the document, non-telco related etc.						  
 



abstract case class ActionTag

abstract case class Tags

case class TransitionTag(
	val event:  String, 
	val name: 	 String,
	val state:  String,
	val actions:List[ActionTag])  
						 
	
case class LogAction(
	val expr: String) 
	extends ActionTag  

class Exit() 
	extends ActionTag


case class FetchAction(
	val next: String, 
	val tipe: String, 
	val namelist:Array[String], 
	val method:String, 
	val fetchId:String) 
	extends ActionTag  

	
case class IfAction(
	val ifActions:ConditionalChunk,
	val elseIfActions:List[ConditionalChunk], 
	val elseActions: List[ActionTag]) 
	extends ActionTag  	

case class ConditionalChunk( 
	val condition:String, 
	val successActions: List[ActionTag])
 
case class AssignAction(
	val name:String, 
	val expr:String ) 
	extends ActionTag

case class GotoAction(val fetchId:String) extends ActionTag 


/*
 * Telecom related actions here for actual network related stuff... 
 * 
 */
 

abstract case class TelecomAction 
	extends ActionTag   

abstract case class CallAction(
	val connectionid:String) 
	extends TelecomAction  
 
case class CreateCall(
	val dest:String,
	val connectionId:String,
	val aai:String,
	val callerid:String,
	val hints:String,
	val timeout:String,
	val joinid:String,
	val joindirection:String)
	extends TelecomAction
				 
case class Disconnect(
	connectionId:String, 
	val reason:String,
	val hints:String ) 
	extends CallAction(connectionId) 

case class Accept(
	connectionId:String, 
	val hints:String) 
	extends CallAction(connectionId)

case class Reject(
	connectionId:String,
	dest:String,
	reason:String,
	hints:String)
	extends CallAction(connectionId)
 
case class Cancel(
	connectionId:String)
	extends CallAction(connectionId)

case class Join(
		val id1:String, 
		val id2:String, 
		val duplex:String) extends TelecomAction 


		  /*
   //CONTROL TAGS
    assign
    createccxml
    if
    else
    elseif	
    exit
    fetch
    goto
    var
    script
 
   */

		/*
	 accept							
	 authenticate					ccxml
	 createcall						
	 createconference				destroyconference
	 dialogstart					dialogterminate
	 disconnect						
		  							eventhandler
		 							
		 							
	 join							
	 move							redirect
	 reject							
	 send							transition
	 unjoin							 
		 */
		
