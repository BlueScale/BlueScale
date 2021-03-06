/*
* This file is part of BlueScale.
* 
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
* Please contact us at www.BlueScale.org
*
*/
package org.bluescale.telco.jainsip

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
import gov.nist.javax.sip.SipStackImpl
import gov.nist.javax.sip.stack.ServerLog
import gov.nist.javax.sip.clientauthutils._
import gov.nist.javax.sip.SipStackExt
import gov.nist.javax.sip.clientauthutils.AuthenticationHelper
import java.net.InetAddress
import scala.actors.Actor
import java.util.Properties
import scala.reflect.BeanProperty


import org.bluescale.telco._
import org.bluescale.telco.jainsip._ 
import org.bluescale.telco.api._  
import org.bluescale.util._

protected[jainsip] case class JainSipInternal(telco:SipTelcoServer,
										val listeningIp:String,
										val contactIp:String,
										val port:Int,
										val destIp:String, 
										val destPort:Int) extends SipListener 
														 with LogHelper
														 {
    log("listeningIp = " + listeningIp)
	log("contactIp = " + contactIp)
    
	val sipFactory = SipFactory.getInstance()
	sipFactory.setPathName("gov.nist")
	
	
	val transport = "udp"

	val properties = new Properties()
    properties.setProperty("javax.sip.STACK_NAME", "BSSJainSip"+this.hashCode())//name with hashcode so we can have multiple instances started in one VM
	//properties.setProperty("javax.sip.OUTBOUND_PROXY", destIp +  ":" + destPort + "/"+ transport)
	properties.setProperty("gov.nist.javax.sip.DEBUG_LOG","log/debug_log" + port + ".log.txt") //FIXME
	properties.setProperty("gov.nist.javax.sip.SERVER_LOG","log/server_log" + port + ".log.txt")
	properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS","false")
	properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "ERROR")
	properties.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", "1")	
    properties.setProperty("gov.nist.javax.sip.LOG_MESSAGE_CONTENT", "false") 

	val sipStack = sipFactory.createSipStack(properties)
	//sipStack.asInstanceOf[SipStackImpl].getServerLog().setTraceLevel(ServerLog.TRACE_NONE)
    debug("createSipStack " + sipStack)
	val headerFactory 	= sipFactory.createHeaderFactory()
	val addressFactory = sipFactory.createAddressFactory()
	val messageFactory = sipFactory.createMessageFactory()
	
	var udpListeningPoint:Option[ListeningPoint] = None 
	var sipProvider:Option[SipProvider] = None
	//val inviteCreator = new InviteCreator(this)
	implicit val jainSipInternal = this
	
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
		val method = request.getMethod()
		log("Processing Request = " + method)
		request.getMethod() match {
			case Request.REGISTER => processRegister(requestEvent)
			case Request.INVITE 	=> processInvite(requestEvent)
			case Request.ACK 		=> processAck(requestEvent, requestEvent.getRequest())
			case Request.BYE 		=> processBye(requestEvent, requestEvent.getRequest())
			case Request.CANCEL 	=> processCancel(requestEvent)
			case Request.OPTIONS  => processOptions(requestEvent)
		    //TODO: handle cancels...may be too late for most things but we can try to cancel.
			case _ => error("err, what?, method = " + request.getMethod())
		            //serverTransactionId.sendResponse( messageFactory.createResponse( 202, request ) )
					// send one back
					//val prov = requestEvent.getSource()
					//val refer = requestEvent.getDialog().createRequest("REFER")
					//requestEvent.getDialog().sendRequest( prov.getNewClientTransaction(refer) )
		 }		 
	}
	
	private def getServerTx(request:RequestEvent): ServerTransaction = 
		Option(request.getServerTransaction) match {
			case Some(tx) => tx
			case None => sipProvider.get.getNewServerTransaction(request.getRequest())
		}
	

	private def processOptions(requestEvent: RequestEvent) {
        val response = messageFactory.createResponse(200, requestEvent.getRequest())
        val allowHeader = headerFactory.createAllowHeader("INVITE")
        val encodingHeader = headerFactory.createAcceptEncodingHeader("application/sdp")
        response.addHeader(allowHeader)
        requestEvent.getServerTransaction().sendResponse(response) 
	}
    
    private def processBye(requestEvent: RequestEvent, request:Request): Unit = {
		println("process BYE")
    	val tx = getServerTx(requestEvent)
		tx.sendResponse(messageFactory.createResponse(200, request))
		val conn = telco.getConnection(getCallId(request))
		conn.bye(tx)
	}
        
	private def processRegister(requestEvent:RequestEvent) {
		val request = requestEvent.getRequest()
	    val tx = getServerTx(requestEvent)
		tx.sendResponse(messageFactory.createResponse(Response.TRYING, request))
		printHeaders(request)
		//val sipname = getDest(request.getRequestURI().toString())
		val sipname = getDest(request.getHeader("To").asInstanceOf[ToHeader].getAddress().getURI().toString())
		val contact = request.getHeader("Contact").asInstanceOf[ContactHeader].getAddress().getURI().toString.split(";")(0)
		val authFunction = (pass:String) => {
			println("trying for pass!" + pass)
		  	new DigestServerAuthenticationHelper().doAuthenticatePlainTextPassword(request,pass) match {
				case true => 
			    	sendRegisterResponse(200, requestEvent, tx)
			    	telco.addSipBinding(sipname, contact) 
			    	telco.sentPasswordPrompt.remove(tx.getBranchId())
					true
				case false => 
			    	//generate challenge for aunauthorized requests
			    	val dsam = new DigestServerAuthenticationHelper()
			    	val challengeResponse = messageFactory.createResponse(Response.PROXY_AUTHENTICATION_REQUIRED, request)
			    	dsam.generateChallenge(headerFactory, challengeResponse, "nist.gov")
			    	tx.sendResponse(challengeResponse) 
			    	false
			}
		}
		  
		val rejectFunction = ()=>sendRegisterResponse(401, requestEvent, tx)
		
		val registerRequest = new IncomingRegisterRequest(sipname, contact, authFunction, rejectFunction)
		
		telco.sentPasswordPrompt.putIfAbsent(sipname+contact, true) match {
		  	case true =>
		  		telco.fireRegister(registerRequest)
		  	case false =>
		  		authFunction(null)
		}
		//TODO: removing of expired addscreateSipUR,
    }

    private def processCancel(requestEvent:RequestEvent) {
    	//println("processCANCEL called!!!!")
	    val request = requestEvent.getRequest()
	    val conn = telco.getConnection(getCallId(request))
	    conn.cancel(requestEvent.getServerTransaction()) //fixme, shouldn't be setting here!v
	}
  
	private def processInvite(requestEvent:RequestEvent) {
		println("PROCESS INVITE")
		val request = requestEvent.getRequest()
		Option(requestEvent.getServerTransaction) match {
			case Some(transaction) =>
			    val conn = telco.getConnection(getCallId(request))
                val sdp = SdpHelper.getSdp(request.getRawContent())
			    conn.reinvite(transaction, sdp)
			case None => 	    
			    val transaction = requestEvent.getSource().asInstanceOf[SipProvider].getNewServerTransaction(request)
			    //TODO: should we respond with progressing, and only ringing if the user does something? 
				transaction.sendResponse(messageFactory.createResponse(Response.RINGING,request) )
				val requestURI = request.getRequestURI().toString
				val destination = getDest(requestURI)
				val origin      = parseFromHeader(request)
				val conn = new SipConnectionImpl(getCallId(request), destination, origin, INCOMING(), telco, true)
                val sdp = SdpHelper.getSdp(request.getRawContent())
                telco.addConnection(conn)
                conn.invite(transaction,sdp)
                telco.fireIncoming(conn)//todo: move to connection?
    	}
	}
  	
  	private def processAck(requestEvent:RequestEvent, request:Request) { 
		val request = requestEvent.getRequest()
  	    val conn = telco.getConnection(getCallId(request))//FIXME: return an option and foreach on it... prevent NPE
  	    conn.ack(requestEvent.getServerTransaction())
    }  	
   
   //TODO: handle re-tries... we can't just let another setState happen, could fuck things up if something else had been set already...
	override def processResponse(re:ResponseEvent) {
		try { 
		var transaction = re.getClientTransaction()
			
		val conn = Option(telco.getConnection(getCallId(re)))
		if ( null == transaction ) { //we already got a 200OK and the TX was terminated...
			debug("Something was null, tx = " + transaction + " | conn = " + conn + " callId = " + getCallId(re) + "statusCOde = " + asResponse(re).getStatusCode())
			return 
		}
		val cseq = asResponse(re).getHeader(CSeqHeader.NAME).asInstanceOf[CSeqHeader]
		val statusCode = asResponse(re).getStatusCode()
		statusCode match {
			case Response.SESSION_PROGRESS => //conn.setState(VERSIONED_PROGRESSING("") )

			case Response.RINGING =>
	 		                Option(asResponse(re).getRawContent()).foreach( content=> {
    			                val sdp = SdpHelper.getSdp(content)
			                    if (!SdpHelper.isBlankSdp(sdp)) {
	    		                    conn.foreach(_.setUAC(transaction,statusCode, sdp))       
	    		                }
                            }) 
			                           
			case Response.OK =>
	 		            for (c <- conn)
	 		            	cseq.getMethod() match {
	 		            	case Request.INVITE =>
	 		            		val inviteRequset = transaction.getRequest()
	 		            		val ackRequest = transaction.getDialog().createAck( cseq.getSeqNumber() )
	 		            		transaction.getDialog().sendAck(ackRequest)//should be done in the call? 
	 		            		val sdp = SdpHelper.getSdp(asResponse(re).getRawContent())
	 		            		
	 		            		c.setUAC(transaction, statusCode, sdp)	
				  					    				  						    	
		    				case Request.CANCEL =>
		    				    //cancel, removeConnection,
		    				    c.setUAC(transaction, statusCode, c.sdp) 
		    					log("	cancel request ")
		    				case Request.BYE =>
		    					telco.removeConnection(c)
		    					c.setUAC(transaction, statusCode, c.sdp)
    	    				case _ => 
			    			  	error("	wtf ")
		    			}
			case Response.UNAUTHORIZED | Response.PROXY_AUTHENTICATION_REQUIRED =>
				cseq.getMethod() match {
					case Request.REGISTER =>
					  	val authinfo = telco.getRegistAuthInfo(transaction.getBranchId())
						val credentialHelper = new AccountManager { 
								def getCredentials(challengeTx:ClientTransaction,realm:String) =
					  		    new UserCredentials {
					  	  			def getUserName() = authinfo.user 
					  	  			def getPassword() = authinfo.pass
					  	  			def getSipDomain() = authinfo.domain
					  		   }
					  	}
					  	val req = transaction.getRequest()
						val authenticationHelper = 
						       		sipStack.asInstanceOf[SipStackExt].getAuthenticationHelper(credentialHelper, headerFactory)
						val inviteTid = authenticationHelper.handleChallenge(asResponse(re), transaction, sipProvider.get, 30)
						inviteTid.sendRequest()
					case _ => error(" unaothorized for non register")
				}
		    case Response.REQUEST_TERMINATED =>
		        log("TERMINATED")
			case _ => 
		        //something went wrong, lets consider it in a fucked up state
		        conn.foreach(c => c.setUAC(transaction, statusCode, c.sdp)) 
		        error("Unexpected Response = " + asResponse(re).getStatusCode())
		}
		} catch {
			//note: we don't usually care about errors here, so many times its caused by clients resending data and Txs being null
			case ex:Exception => ex.printStackTrace()
		}
	}
	
	def sendRegisterRequest(from:String, dest:String): String = {
		val request = InviteCreator.createRegister(from, dest,SdpHelper.getBlankSdp(this.contactIp).toString.getBytes())	
    	val tx = sipProvider.get.getNewClientTransaction(request)
    	tx.sendRequest()
    	val req = tx.getRequest()
    	println(" RegisterRequest TX id = " + tx.hashCode())
    	return tx.getBranchId()
    }

	def sendRegisterResponse(responseCode:Int, requestEvent:RequestEvent, tx:ServerTransaction) = {
        val response = messageFactory.createResponse(responseCode, requestEvent.getRequest() ) //transaction.getRequest())
		response.addHeader( requestEvent.getRequest().getHeader("Contact"))
        val contact =  headerFactory.createContactHeader(addressFactory.createAddress("sip:" + contactIp + ":"+port))
        contact.setExpires(999999) //TODO: fix this...
        response.addHeader(contact)
		tx.sendResponse(response)
	}
 
	//TODO: deal with dead transactions...
	def sendResponse(responseCode:Int, tx:ServerTransaction, content:Array[Byte] ) {
	    val response = messageFactory.createResponse(responseCode,tx.getRequest)
		//response.getHeader(ToHeader.NAME).asInstanceOf[ToHeader].setTag("4321")  //FIXME
        response.addHeader(headerFactory.createContactHeader(addressFactory.createAddress("sip:" + contactIp + ":"+port)))
        if ( null != content ) response.setContent(content,headerFactory.createContentTypeHeader("application", "sdp"))
		tx.sendResponse(response)
    }

    /*
	def handleMedia(conn:JainSipConnection, re:ResponseEvent) {
        SdpHelper.addMediaTo( conn.sdp, SdpHelper.getSdp(asResponse(re).getRawContent()) )
	}
	*/

    def sendCancel(clientTx:ClientTransaction) : ClientTransaction = {
        val request = clientTx.createCancel()
        val tx = sipProvider.get.getNewClientTransaction(request)
        tx.sendRequest()
        return tx
    }
	
	def sendInvite(from:String, to:String, sdp:SessionDescription) : (String,ClientTransaction) = {
		val request = InviteCreator.createInviteRequest(from, to, sdp.toString().getBytes())
		//FIXME: add FROM
		request.addHeader(InviteCreator.getViaHeader(this).get(0))
		//conn.contactHeader = Some(request.getHeader("contact").asInstanceOf[ContactHeader])
		val tx =  sipProvider.get.getNewClientTransaction(request)
		val id = getCallId(request)
		tx.sendRequest()
		return (id, tx)
	}

    def sendReinvite(tx:Transaction, sdp:SessionDescription): ClientTransaction = {
		///how to get the dialog? from the transaction?
        val request = tx.getDialog().createRequest(Request.INVITE)
        request.removeHeader("contact")//The one from the createRequest is the listeningIP..., same with the via
        request.removeHeader("via")
        request.addHeader(InviteCreator.getViaHeader(this).get(0))
		request.addHeader(headerFactory.createContactHeader(addressFactory.createAddress("sip:" + contactIp + ":" + port)))
		val contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp")
		request.setContent(sdp.toString().getBytes(), contentTypeHeader)
	    val newTx = sipProvider.get.getNewClientTransaction(request) 
   		tx.getDialog().sendRequest(newTx)
   		return newTx
	}
 
    //should we return the bye here?  I think certain things should be un-cancellable, so OK...
	def sendByeRequest(tx:Transaction): ClientTransaction = {
    	println("SENDING BYTE")
    	val dialog = tx.getDialog()
        val byeRequest = dialog.createRequest(Request.BYE)
        val newTx =	sipProvider.get.getNewClientTransaction(byeRequest)
        dialog.sendRequest(newTx)
        return newTx
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
  	 
  	//private def parseToHeader(to:String): String = {
        //TODO: fix case where there is no callerID
     //   to.toString().split("@")(0).split(":")(1)
  	//}

  	private def parseFromHeader(request:Request) : String = { 
  	    try {
            return getDest(request.getHeader("From").asInstanceOf[FromHeader].getAddress().toString)
        } catch  {
            case ex:Exception =>
                error("Exception parsing FROM header, it was " + request.getHeader("From"))
        }
        return ""
    }
  	
  	private def getDest(str:String) = 
  		str.split(";")(0).split("@")(0).split(":")(1)
  	
  	private def printHeaders(request:Request) = { 
  		val iter = request.getHeaderNames()
		while (iter.hasNext()) {
			val headerName = iter.next().toString()
			log("  h = " + headerName + "=" + request.getHeader(headerName))
		}
  	} 
}
	

