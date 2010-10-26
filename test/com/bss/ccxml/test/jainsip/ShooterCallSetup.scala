//errr..this is weird, can I open source something like this?
package com.bss.ccxml.test.jainsip
 

import javax.sip._;
import javax.sip.address._;
import javax.sip.header._;
import javax.sip.message._;

import java.text.ParseException;
import java.util._;
import java.util.ArrayList;

/**
 * Copied from JainSip, and ported.
 * this tries to make a call and then hangs up
 * 
 * @author M. Ranganathan
 */

class ShooterCallSetup(val ip:String, destIp:String, port:Int ) extends SipListener {
	
	val sendBye = true;
  
	var  sipProvider:SipProvider = null;

	var  addressFactory:AddressFactory = null;

	var  messageFactory:MessageFactory = null;

	var  headerFactory:HeaderFactory = null;

	var  sipStack:SipStack = null;

	var  contactHeader:ContactHeader = null;

	var udpListeningPoint:ListeningPoint = null;

	var  inviteTid:ClientTransaction = null;

	var dialog:Dialog = null;

	var byeTaskRunning = false;
 
	class ByeTask(val dialog:Dialog)  extends TimerTask {
	 
		 
		def run () {
			try {
			   val byeRequest = this.dialog.createRequest(Request.BYE);
			   val ct = sipProvider.getNewClientTransaction(byeRequest);
			   dialog.sendRequest(ct);
			} catch {
			  case ex:Exception =>
				ex.printStackTrace();
				System.exit(0);
			}

		}

	}

	val usageString = "java  examples.ShooterCallSetup.ShooterCallSetup \n >>>> is your class path set to the root?";

	private def usage() {
		System.out.println(usageString);
		System.exit(0);

	}


	def processRequest( requestReceivedEvent:RequestEvent) {
		val request = requestReceivedEvent.getRequest();
		val serverTransactionId = requestReceivedEvent.getServerTransaction();

		System.out.println("\n\nRequest " + request.getMethod()
				+ " received at " + sipStack.getStackName()
				+ " with server transaction id " + serverTransactionId);

		// We are the UAC so the only request we get is the BYE.
		if (request.getMethod().equals(Request.BYE))
			processBye(request, serverTransactionId);
		else {
			try {
				serverTransactionId.sendResponse( messageFactory.createResponse(202,request) );
			} catch {
			  case ex:Exception =>
			    	ex.printStackTrace();
			}
		}

	}

	def processBye(request:Request, serverTransactionId:ServerTransaction) {
		try {
			System.out.println("ShooterCallSetup:  got a bye .");
			if (serverTransactionId == null) {
				System.out.println("ShooterCallSetup:  null TID.");
				return;
			}
			val dialog = serverTransactionId.getDialog();
			System.out.println("Dialog State = " + dialog.getState());
			val  response = messageFactory.createResponse(200, request);
			serverTransactionId.sendResponse(response);
			System.out.println("ShooterCallSetup:  Sending OK.");
			System.out.println("	Dialog State = " + dialog.getState());

		} catch  {
		  case ex:Exception =>
			ex.printStackTrace();
			System.exit(0);

		}
	}

 	def processResponse(responseReceivedEvent:ResponseEvent ) {
 		var ackRequest:Request = null;
		System.out.println("Got a response");
		val response = responseReceivedEvent.getResponse().asInstanceOf[Response];
		val tid = responseReceivedEvent.getClientTransaction();
		val cseq = response.getHeader(CSeqHeader.NAME).asInstanceOf[CSeqHeader];

		System.out.println("Response received : Status Code = "+ response.getStatusCode() + " " + cseq);
		
			
		if (tid == null) {
			
			// RFC3261: MUST respond to every 2xx
			if (ackRequest!=null && dialog!=null) {
			   System.out.println("re-sending ACK");
			   try {
			      dialog.sendAck(ackRequest);
			   } catch {
			     case se:SipException =>
			      se.printStackTrace(); 
			   }
			}			
			return;
		}
		// If the caller is supposed to send the bye
		if ( true && !byeTaskRunning) {
			byeTaskRunning = true;
			new Timer().schedule(new ByeTask(dialog), 4000) ;
		}
		System.out.println("transaction state is " + tid.getState());
		System.out.println("Dialog = " + tid.getDialog());
		System.out.println("Dialog State is " + tid.getDialog().getState());

		try {
			if (response.getStatusCode() == Response.OK) {
				if (cseq.getMethod().equals(Request.INVITE)) {
					System.out.println("Dialog after 200 OK  " + dialog);
					System.out.println("Dialog State after 200 OK  " + dialog.getState());
					val h = response.getHeader(CSeqHeader.NAME).asInstanceOf[CSeqHeader];
					ackRequest = dialog.createAck( h.getSeqNumber() );
					System.out.println("Sending ACK");
					dialog.sendAck(ackRequest);
					
					// JvB: test REFER, reported bug in tag handling
					dialog.sendRequest(  sipProvider.getNewClientTransaction( dialog.createRequest("REFER") )); 
					
				} else if (cseq.getMethod().equals(Request.CANCEL)) {
					if (dialog.getState() == DialogState.CONFIRMED) {
						// oops cancel went in too late. Need to hang up the
						// dialog.
						System.out.println("Sending BYE -- cancel went in too late !!");
						val byeRequest = dialog.createRequest(Request.BYE);
						val ct = sipProvider.getNewClientTransaction(byeRequest);
						dialog.sendRequest(ct);

					}

				}
			}
		} catch  {
		  case ex:Exception =>
			ex.printStackTrace();
			System.exit(0);
		}

	}

	def processTimeout(timeoutEvent:javax.sip.TimeoutEvent) {

		System.out.println("Transaction Time out");
	}

	def sendCancel() {
		try {
			System.out.println("ShooterCallSetup: 	Sending cancel");
			val cancelRequest = inviteTid.createCancel();
			val cancelTid = sipProvider.getNewClientTransaction(cancelRequest);
			cancelTid.sendRequest();
		} catch  {
		  case ex:Exception =>
			ex.printStackTrace();
		}
	}

	def init() { 
		sipStack = null;
		val sipFactory = SipFactory.getInstance();
		sipFactory.setPathName("gov.nist");
		val properties = new Properties();
		// If you want to try TCP transport change the following to
		val transport = "udp";
		val peerHostPort = destIp+":5070";
		properties.setProperty("javax.sip.OUTBOUND_PROXY", peerHostPort+"/"+ transport);
  
		// If you want to use UDP then uncomment this.
		properties.setProperty("javax.sip.STACK_NAME", "ShooterCallSetup");

		// The following properties are specific to nist-sip
		// and are not necessarily part of any other jain-sip
		// implementation.
		// You can set a max message size for tcp transport to
		// guard against denial of service attack.
    
		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG","ShooterCallSetupdebug.txt");
		properties.setProperty("gov.nist.javax.sip.SERVER_LOG","ShooterCallSetuplog.txt");

		// Drop the client connection after we are done with the transaction.
		properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS","false");
		// Set to 0 (or NONE) in your production code for max speed.
		// You need 16 (or TRACE) for logging traces. 32 (or DEBUG) for debug + traces.
		// Your code will limp at 32 but it is best for debugging.
		properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "TRACE");

		try {
			// Create SipStack object
			sipStack = sipFactory.createSipStack(properties);
			System.out.println("createSipStack " + sipStack + " sipStack ip = " + sipStack.getIPAddress);
		} catch {
		  case e:PeerUnavailableException =>
			// could not find
			// gov.nist.jain.protocol.ip.sip.SipStackImpl
			// in the classpath
			e.printStackTrace();
			System.err.println(e.getMessage());
			System.exit(0);
		}

		try {
			headerFactory = sipFactory.createHeaderFactory();
			addressFactory = sipFactory.createAddressFactory();
			messageFactory = sipFactory.createMessageFactory();
			udpListeningPoint = sipStack.createListeningPoint(ip, port, "udp");
			sipProvider = sipStack.createSipProvider(udpListeningPoint);
			val listener = this;
			sipProvider.addSipListener(listener);

			val fromName = "BigGuy";
			val fromSipAddress = "here.com";
			val fromDisplayName = "The Master Blaster";

			val toSipAddress = "there.com";
			val toUser = "LittleGuy";
			val toDisplayName = "The Little Blister";

			// create >From Header
			val fromAddress = addressFactory.createSipURI(fromName,fromSipAddress);

			val fromNameAddress = addressFactory.createAddress(fromAddress);
			fromNameAddress.setDisplayName(fromDisplayName);
			val fromHeader = headerFactory.createFromHeader(fromNameAddress, "12345");

			// create To Header
			val toAddress = addressFactory.createSipURI(toUser, toSipAddress);
			val toNameAddress = addressFactory.createAddress(toAddress);
			toNameAddress.setDisplayName(toDisplayName);
			val toHeader = headerFactory.createToHeader(toNameAddress,null);

			// create Request URI
			val requestURI = addressFactory.createSipURI(toUser,peerHostPort);

			// Create ViaHeaders

			val viaHeaders = new ArrayList[ViaHeader]();
			val ipAddress = udpListeningPoint.getIPAddress();
			val viaHeader = headerFactory.createViaHeader(ipAddress,sipProvider.getListeningPoint(transport).getPort(),transport, null);

			// add via headers
			viaHeaders.add(viaHeader);

			// Create ContentTypeHeader
			val contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");

			// Create a new CallId header
			val callIdHeader = sipProvider.getNewCallId();

			// Create a new Cseq header
			val cSeqHeader = headerFactory.createCSeqHeader(1L,Request.INVITE);

			// Create a new MaxForwardsHeader
			val maxForwards = headerFactory.createMaxForwardsHeader(70);

			// Create the request.
			val request = messageFactory.createRequest(requestURI,
					Request.INVITE, callIdHeader, cSeqHeader, fromHeader,
					toHeader, viaHeaders, maxForwards);
			// Create contact headers
			val host = "127.0.0.1";

			val contactUrl = addressFactory.createSipURI(fromName, host);
			contactUrl.setPort(udpListeningPoint.getPort());
			contactUrl.setLrParam();

			// Create the contact name address.
			val contactURI = addressFactory.createSipURI(fromName, host);
			contactURI.setPort(sipProvider.getListeningPoint(transport).getPort());

			val contactAddress = addressFactory.createAddress(contactURI);

			// Add the contact address.
			contactAddress.setDisplayName(fromName);

			contactHeader = headerFactory.createContactHeader(contactAddress);
			request.addHeader(contactHeader);

			// You can add extension headers of your own making
			// to the outgoing SIP request.
			// Add the extension header.
			var extensionHeader = headerFactory.createHeader("My-Header","my header value");
			request.addHeader(extensionHeader);

			val sdpData = <msg>v=0\r\n
					o=4855 13760799956958020 13760799956958020
					 IN IP4  129.6.55.78\r\n s=mysession session\r\n
					p=+46 8 52018010\r\n c=IN IP4  129.6.55.78\r\n
					t=0 0\r\n m=audio 6022 RTP/AVP 0 4 18\r\n
					a=rtpmap:0 PCMU/8000\r\n =rtpmap:4 G723/8000\r\n
					a=rtpmap:18 G729A/8000\r\n a=ptime:20\r\n</msg>.text;
			
			val contents = sdpData.getBytes();

			request.setContent(contents, contentTypeHeader);
			// You can add as many extension headers as you
			// want.

			extensionHeader = headerFactory.createHeader("My-Other-Header","my new header value ");
			request.addHeader(extensionHeader);

			val callInfoHeader = headerFactory.createHeader("Call-Info",
					"<http://www.antd.nist.gov>");
			request.addHeader(callInfoHeader);

			// Create the client transaction.
			inviteTid = sipProvider.getNewClientTransaction(request);

			// send the request out.
			inviteTid.sendRequest();

			dialog = inviteTid.getDialog();

		} catch {
		  case ex:Exception=>
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			usage();
		}
	}

	def main(args:Array[String]) {
		//new ShooterCallSetup().init();

	}

	def processIOException(exceptionEvent:IOExceptionEvent) {
		System.out.println("IOException happened for "
				+ exceptionEvent.getHost() + " port = "
				+ exceptionEvent.getPort());

	}

	def processTransactionTerminated(transactionTerminatedEvent:TransactionTerminatedEvent) {
		System.out.println("ShooterCallSetup Transaction terminated event recieved");
	}

	def processDialogTerminated(dialogTerminatedEvent:DialogTerminatedEvent) {
		System.out.println("dialogTerminatedEvent");
	}
}
