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
package com.bss.telco.jainsip

import javax.sip._
import javax.sip.address._
import javax.sip.header._
import javax.sip.message._
import javax.sdp.MediaDescription
import javax.sdp.SdpException
import javax.sdp.SdpFactory
import javax.sdp.SdpParseException
import javax.sdp.SessionDescription
import javax.sdp.MediaDescription
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.stack.ServerLog;

import java.util._
import java.net.InetAddress
import scala.actors.Actor 
import com.bss.telco._
import com.bss.telco.jainsip._ 
import com.bss.telco.api._  
import com.bss.util._

protected[jainsip] class JainSipInternal(telco:SipTelcoServer,
										val listeningIp:String,
										val contactIp:String,
										val port:Int,
										val destIp:String, 
										val destPort:Int) extends SipListener 
														 //with LogHelper
														 {

	println("listeningIp = " + listeningIp)
	println("contactIp = " + contactIp)
    val sipFactory = SipFactory.getInstance()
	sipFactory.setPathName("gov.nist")
	val properties = new Properties()
	val transport = "udp"

	def debug(s:String) {
		println(s)
	}
	def log(s:String) {
			println(s)
	}

	def error(s:String) {
		//println(s)
	}
	
 
    properties.setProperty("javax.sip.STACK_NAME", "BSSJainSip"+this.hashCode())//name with hashcode so we can have multiple instances started in one VM
	properties.setProperty("javax.sip.OUTBOUND_PROXY", destIp +  ":" + destPort + "/"+ transport)
	properties.setProperty("gov.nist.javax.sip.DEBUG_LOG","log/debug_log" + port + ".log.txt") //FIXME
	properties.setProperty("gov.nist.javax.sip.SERVER_LOG","log/server_log" + port + ".log.txt")
	properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS","false")
	properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "ERROR")
	properties.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", "10")	
    properties.setProperty("gov.nist.javax.sip.LOG_MESSAGE_CONTENT", "false") 
 
	val sipStack = sipFactory.createSipStack(properties)
	//sipStack.asInstanceOf[SipStackImpl].getServerLog().setTraceLevel(ServerLog.TRACE_NONE)
    debug("createSipStack " + sipStack)
	val headerFactory 	= sipFactory.createHeaderFactory()
	val addressFactory = sipFactory.createAddressFactory()
	val messageFactory = sipFactory.createMessageFactory()
	
	var udpListeningPoint:Option[ListeningPoint] = None //
	var sipProvider:Option[SipProvider] = None
	val inviteCreator = new InviteCreator(this)

	def start() {
		sipStack.start()
		udpListeningPoint = Some( sipStack.createListeningPoint(listeningIp, port, transport) )
        sipProvider = Some( sipStack.createSipProvider(udpListeningPoint.get) )
	    sipProvider.get.addSipListener(this)
	}

	def stop() {
		sipStack.stop()
		udpListeningPoint.foreach( sipStack.deleteListeningPoint(_) )
	}
	 
	override def processRequest(requestEvent:RequestEvent) {
		val request = requestEvent.getRequest()
		val serverTransactionId = requestEvent.getServerTransaction()

		 request.getMethod() match {
		  case Request.INVITE 	=> processInvite(requestEvent)
		  case Request.ACK 		=> processAck(requestEvent, requestEvent.getRequest())
		  case Request.BYE 		=> processBye(requestEvent, requestEvent.getRequest(), requestEvent.getServerTransaction())
		  case Request.REGISTER => processRegister(requestEvent)
		  case Request.CANCEL 	=> println("processCancel(requestEvent, serverTransactionId)") //TODO: handle cancels...may be too late for most things but we can try to cancel.
		  case _ =>/*serverTransactionId.sendResponse( messageFactory.createResponse( 202, request ) )
					// send one back
					val prov = requestEvent.getSource()
					val refer = requestEvent.getDialog().createRequest("REFER")
					requestEvent.getDialog().sendRequest( prov.getNewClientTransaction(refer) )
					*/
		 }		 
	}
	
	def processRegister(requestEvent:RequestEvent) {
		val r = requestEvent.getRequest()
		printHeaders(requestEvent.getRequest())
		sendRegisterResponse(200, requestEvent)
    }
    /* 
	def processNewRequest(requestEvent: RequestEvent, f:(RequestEvent)=>Unit) =
		requestEvent.getServerTransaction() match {
		  case null => { val transaction = requestEvent.getSource().asInstanceOf[SipProvider].getNewServerTransaction(requestEvent.getRequest())
					 
		  				}
		 
		  case _ => error("request event that's not null...this shouldn't happen")
			  }
	*/
  
	def processInvite(requestEvent: RequestEvent) {
		debug("request for " + requestEvent.getRequest())	
		val request = requestEvent.getRequest()
		Option(requestEvent.getServerTransaction) match {

			case Some(transaction) => 	val conn = telco.getConnection(getCallId(request))
			                            conn.execute( ()=>{
											conn.serverTx = Some(transaction)
											SdpHelper.addMediaTo(conn.localSdp, SdpHelper.getSdp(request.getRawContent()) )					
											sendResponse(200, conn, request.getRawContent())
											//fixme: do we need to notify other things listening to this SDP session?
										})
			
			case None => 	    val transaction = requestEvent.getSource().asInstanceOf[SipProvider].getNewServerTransaction(request)
								transaction.sendResponse(messageFactory.createResponse(Response.RINGING,request) )
								val destination = parseToHeader(request.getRequestURI().toString())
								val conn = new JainSipConnection(getCallId(request), destination, "", INCOMING(), telco, true)
                                conn.execute( ()=>{
                                	conn.contactHeader = Some(request.getHeader("contact").asInstanceOf[ContactHeader]) 
                                    conn.serverTx = Some(transaction)
								    conn.dialog = Some( transaction.getDialog() ) //FIXME: we may not need to store this...
                                    //TOOD: make sure it defaults to alerting
									conn.setConnectionid(getCallId(request))
									telco.addConnection(conn) //(getCallId(request), conn)
									SdpHelper.addMediaTo(conn.localSdp, SdpHelper.getSdp(request.getRawContent()))
									telco.fireIncoming(conn) 
								})
		}
		
	}
	
	def sendRegisterResponse(responseCode:Int, requestEvent:RequestEvent) = {
    	//LETS DO THIS MORE SCALA STYLE
		val response = messageFactory.createResponse(responseCode, requestEvent.getRequest() ) //transaction.getRequest())
		response.addHeader( requestEvent.getRequest().getHeader("Contact"))
		requestEvent.getServerTransaction().sendResponse(response)
	}
 
	//TODO: deal with dead transactions...
	def sendResponse(responseCode:Int, conn:JainSipConnection, content:Array[Byte] ) = 
	    conn.serverTx match {
            case Some(tx)=> 	val response = messageFactory.createResponse(responseCode,conn.serverTx.get.getRequest)
		                        //response.getHeader(ToHeader.NAME).asInstanceOf[ToHeader].setTag("4321")  //FIXME
		                        response.addHeader(headerFactory.createContactHeader(addressFactory.createAddress("sip:" + contactIp + ":"+port)))
                            	if ( null != content ) response.setContent(content,headerFactory.createContentTypeHeader("application", "sdp"))
		                        tx.sendResponse(response)
            case None => println("Error, ServerTX not found for conn " + conn)
	    }
	 
	
	
    
	private def processBye(requestEvent: RequestEvent, request:Request, transaction:ServerTransaction) {
		transaction.sendResponse(messageFactory.createResponse(200, request))
		val conn = telco.getConnection(getCallId(request))
		conn.execute(()=>{
			conn.setState(VERSIONED_UNCONNECTED( transaction.getBranchId() ))
			telco.removeConnection(conn)
		})
	}
 
	
  	private def processAck(requestEvent:RequestEvent, request:Request) { 
		val request = requestEvent.getRequest()
      	val conn = telco.getConnection(getCallId(request))
		
		conn.execute(()=>conn.setState( VERSIONED_CONNECTED(conn.serverTx.get.getBranchId() )))
	}  		
   
   //TODO: handle re-tries... we can't just let another setState happen, could fuck things up if something else had been set already...
	override def processResponse(re:ResponseEvent) {
		var transaction = re.getClientTransaction()
		//FIXME: Highly concurrent JAIN-SIP txs have an error with transactions occasionally being null
		if ( null == transaction) {
			debug("                                      transaction is null right away!?!?!? re = " + re.getDialog())
		}
		val cseq = asResponse(re).getHeader(CSeqHeader.NAME).asInstanceOf[CSeqHeader]
		val conn = telco.getConnection(getCallId(re))
		conn.execute(()=>{
		asResponse(re).getStatusCode() match {
			case Response.SESSION_PROGRESS => conn.setState(VERSIONED_PROGRESSING("") )
		    		 				
			case Response.RINGING =>  //println("ringing")
			                           
			case Response.OK => 
		    			cseq.getMethod() match {
		    				case Request.INVITE =>
		    				            
		    				  			val ackRequest = transaction.getDialog().createAck( cseq.getSeqNumber() )
		    							transaction.getDialog().sendAck(ackRequest)
				  						SdpHelper.addMediaTo(conn.localSdp, SdpHelper.getSdp(asResponse(re).getRawContent()) )
				  						conn.dialog = Some( re.getDialog() )
				  					    if ( conn.connectionState == CONNECTED() && SdpHelper.isBlankSdp( conn.localSdp ) ) {
				  					        conn.setState(VERSIONED_HOLD( transaction.getBranchId() ))
				  						} else {
				  						    conn.setState(VERSIONED_CONNECTED( transaction.getBranchId() )) //Is a joined state worth it?
				  						}
				  						            
		    				case Request.CANCEL =>
		    					log("	cancel request ")
		    				case Request.BYE =>
		    					telco.removeConnection(conn)
		    					conn.setState( VERSIONED_UNCONNECTED(transaction.getBranchId() ) )
		    				case _ => 
			    			  	error("	wtf ")
		    			}
			case _ => error("Unexpected Response = " + asResponse(re).getStatusCode())
		}
		})
	}
	
	def sendInvite(conn:JainSipConnection, sdp:SessionDescription) : String = {
		val request = inviteCreator.getInviteRequest(conn.origin, conn.destination, sdp.toString().getBytes())
		conn.contactHeader = Some(request.getHeader("contact").asInstanceOf[ContactHeader])
		conn.clientTx = Some( sipProvider.get.getNewClientTransaction(request) )
		conn.clientTx.get.sendRequest()
        return getCallId(request)
	}
 
	def sendReinvite(conn:JainSipConnection, sdp:SessionDescription) : Unit = {
		val request = conn.dialog.get.createRequest(Request.INVITE)
		conn.contactHeader.foreach( request.addHeader(_) )//neccessary?
		val contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp")
		request.setContent(sdp.toString().getBytes(), contentTypeHeader)
		conn.clientTx = Some( sipProvider.get.getNewClientTransaction(request) )
   		conn.dialog.get.sendRequest(conn.clientTx.get)
	}
 
	def sendByeRequest(conn:JainSipConnection) = {
        val byeRequest = conn.dialog.get.createRequest(Request.BYE)
        conn.clientTx =	Some(this.sipProvider.get.getNewClientTransaction(byeRequest))
        conn.dialog.get.sendRequest(conn.clientTx.get)
	} 
    
	override def processTimeout(timeout:TimeoutEvent) {
	    error("==================================================r processTimeout!")
	}
 
	override def processIOException(ioException:IOExceptionEvent) {
	    error("processIOException!")
	}
 
	override def processTransactionTerminated(transactionTerminated:TransactionTerminatedEvent) {
	    error("processTransactionTerminated")
	}
 
	override def processDialogTerminated(dialogTerminated:DialogTerminatedEvent) {
	    error("processDialogTerminated")
	}
    
  	protected[jainsip] def getCallId(request:Request) = request.getHeader(CallIdHeader.NAME).asInstanceOf[CallIdHeader].getCallId()
	
   	protected[jainsip] def asResponse(r:ResponseEvent) = r.getResponse().asInstanceOf[Response]
       
  	protected[jainsip] def getCallId(r:ResponseEvent) = asResponse(r).
  														getHeader(CallIdHeader.NAME).asInstanceOf[CallIdHeader].getCallId()
  	   
  	protected[jainsip] def asRequest(r:RequestEvent) = r.getRequest()
  	 
  	private def parseToHeader(to:String): String = {
        //TODO: fix case where there is no callerID
        to.toString().split("@")(0).split(":")(1)
  	}
   
  	
  	private def printHeaders(request:Request) = { 
  		val iter = request.getHeaderNames()
		while (iter.hasNext()) {
			val headerName = iter.next().toString()
			println("  h = " + headerName + "=" + request.getHeader(headerName))
		}
  	} 


}
