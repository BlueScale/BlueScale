package com.bss.ccxml.test.jainsip 

import javax.sip._;
import javax.sip.address._;
import javax.sip.header._;
import javax.sip.message._;

import java.text.ParseException;
import java.util._;

/**
 * This class is a UAS template.
 * 
 *  
 */
 class ShootMeCallSetup extends SipListener {

	var addressFactory:AddressFactory = null;

 	var  messageFactory:MessageFactory = null;

	var headerFactory:HeaderFactory = null;

 	var sipStack:SipStack = null;

	var myAddress = "127.0.0.1";

	var myPort = 5070;

	var inviteTid:ServerTransaction = null;

	var okResponse:Response = null;

	var inviteRequest:Request= null;

	var dialog: Dialog = null;

	val callerSendsBye = true;

	class MyTimerTask( ShootMeCallSetup:ShootMeCallSetup) extends TimerTask {
		 

		def run() {
			ShootMeCallSetup.sendInviteOK();
		}

	}

	val usageString = "java examples.shootist.Shootist \n >>>> is your class path set to the root?";

	def usage() {
		System.out.println(usageString);
		System.exit(0);

	}

	def processRequest(requestEvent:RequestEvent) {
		val request = requestEvent.getRequest();
		val serverTransactionId = requestEvent.getServerTransaction();

		System.out.println("\n\nRequest " + request.getMethod()
				+ " received at " + sipStack.getStackName()
				+ " with server transaction id " + serverTransactionId);

		if (request.getMethod().equals(Request.INVITE)) {
			processInvite(requestEvent, serverTransactionId);
		} else if (request.getMethod().equals(Request.ACK)) {
			processAck(requestEvent, serverTransactionId);
		} else if (request.getMethod().equals(Request.BYE)) {
			processBye(requestEvent, serverTransactionId);
		} else if (request.getMethod().equals(Request.CANCEL)) {
			processCancel(requestEvent, serverTransactionId);
		} else {
			try {
				serverTransactionId.sendResponse( messageFactory.createResponse( 202, request ) );
				
				// send one back
				val prov = requestEvent.getSource().asInstanceOf[SipProvider];
				val refer = requestEvent.getDialog().createRequest("REFER");
				requestEvent.getDialog().sendRequest( prov.getNewClientTransaction(refer) );				
				
			} catch {
			  case ex:Exception =>
			    ex.printStackTrace();
			}
		}

	}

	def  processResponse(responseEvent:ResponseEvent) {
		System.out.println("ShootMeCallSetup 	response event = " + responseEvent);
	}

	/**
	 * Process the ACK request. Send the bye and complete the call flow.
	 */
	def processAck(requestEvent:RequestEvent,serverTransaction:ServerTransaction ) {
		try {
			System.out.println("ShootMeCallSetup: got an ACK! ");
			System.out.println("	Dialog State = " + dialog.getState());
			val provider = requestEvent.getSource().asInstanceOf[SipProvider];
			if (!callerSendsBye) {
				val byeRequest = dialog.createRequest(Request.BYE);
				val ct = provider.getNewClientTransaction(byeRequest);
				dialog.sendRequest(ct);
			}
		} catch  {
		  case ex:Exception =>
			ex.printStackTrace();
		}

	}

	/**
	 * Process the invite request.
	 */
	def processInvite(requestEvent:RequestEvent,serverTransaction:ServerTransaction ) {
		val sipProvider =  requestEvent.getSource().asInstanceOf[SipProvider];
		val request = requestEvent.getRequest();
		try {
			System.out.println("ShootMeCallSetup: got an Invite sending Trying");
			// System.out.println("ShootMeCallSetup: " + request);
			val response = messageFactory.createResponse(Response.TRYING,request);
			var st = requestEvent.getServerTransaction();

			if (st == null) {
				st = sipProvider.getNewServerTransaction(request);
			}
			dialog = st.getDialog();

			st.sendResponse(response);

			this.okResponse = messageFactory.createResponse(Response.OK,request);
			val address = addressFactory.createAddress("ShootMeCallSetup <sip:"+ myAddress + ":" + myPort + ">");
			val contactHeader = headerFactory.createContactHeader(address);
			
			response.addHeader(contactHeader);
			val toHeader =  okResponse.getHeader(ToHeader.NAME).asInstanceOf[ToHeader];
			toHeader.setTag("4321"); // Application is supposed to set.
			okResponse.addHeader(contactHeader);
			this.inviteTid = st;
			// Defer sending the OK to simulate the phone ringing.
			// Answered in 1 second ( this guy is fast at taking calls)
			this.inviteRequest = request;

			new Timer().schedule(new MyTimerTask(this), 1000);
		} catch {
		  case ex:Exception =>
			ex.printStackTrace();
			System.exit(0);
		}
	}

	def sendInviteOK() {
		try {
			if (inviteTid.getState() != TransactionState.COMPLETED) {
				System.out.println("ShootMeCallSetup: Dialog state before 200: "+ inviteTid.getDialog().getState());
				inviteTid.sendResponse(okResponse);
				System.out.println("ShootMeCallSetup: Dialog state after 200: "+ inviteTid.getDialog().getState());
			}
		} catch  {
		  case ex:SipException =>
			ex.printStackTrace();
		 
		  case ex:InvalidArgumentException =>
		    ex.printStackTrace();
		}
	}

	/**
	 * Process the bye request.
	 */
	def processBye( requestEvent:RequestEvent, serverTransactionId:ServerTransaction ) {
		val sipProvider =  requestEvent.getSource().asInstanceOf[SipProvider];
		val request = requestEvent.getRequest();
		val dialog = requestEvent.getDialog();
		System.out.println("ShootMeCallSetup local party = " + dialog.getLocalParty());
		try {
			System.out.println("ShootMeCallSetup:  got a bye sending OK.");
			val response = messageFactory.createResponse(200, request);
			serverTransactionId.sendResponse(response);
			System.out.println("	Dialog State is " + serverTransactionId.getDialog().getState());

		} catch  {
		  case ex:Exception =>
		    ex.printStackTrace();
			System.exit(0);

		}
	}

	def processCancel( requestEvent:RequestEvent, serverTransactionId:ServerTransaction ) {
		val sipProvider =  requestEvent.getSource().asInstanceOf[SipProvider];
		val request = requestEvent.getRequest();
		try {
			System.out.println("ShootMeCallSetup:  got a cancel.");
			if (serverTransactionId == null) {
				System.out.println("ShootMeCallSetup:  null tid.");
				return;
			}
			var response = messageFactory.createResponse(200, request);
			serverTransactionId.sendResponse(response);
			if (dialog.getState() != DialogState.CONFIRMED) {
				response = messageFactory.createResponse(Response.REQUEST_TERMINATED, inviteRequest);
				inviteTid.sendResponse(response);
			}

		} catch {
		  
		  case ex:Exception =>
			ex.printStackTrace();
			System.exit(0);

		}
	}

	def processTimeout(timeoutEvent:TimeoutEvent) {
		var transaction = {
		if (timeoutEvent.isServerTransaction()) {
		  timeoutEvent.getServerTransaction()
		 } else {
			timeoutEvent.getClientTransaction();
		}}
		
		System.out.println("state = " + transaction.getState());
		System.out.println("dialog = " + transaction.getDialog());
		System.out.println("dialogState = "+ transaction.getDialog().getState());
		System.out.println("Transaction Time out");
	}

	def init() { 
		sipStack = null;
		val sipFactory = SipFactory.getInstance();
		sipFactory.setPathName("gov.nist");
		
		val properties = new Properties();
		properties.setProperty("javax.sip.STACK_NAME", "ShootMeCallSetup");
		// You need 16 for logging traces. 32 for debug + traces.
		// Your code will limp at 32 but it is best for debugging.
		properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG","ShootMeCallSetupdebug.txt");
		properties.setProperty("gov.nist.javax.sip.SERVER_LOG","ShootMeCallSetuplog.txt");

		try {
			// Create SipStack object
			sipStack = sipFactory.createSipStack(properties);
			System.out.println("sipStack = " + sipStack);
		} catch  {
		  case e:PeerUnavailableException =>
		
			// could not find
			// gov.nist.jain.protocol.ip.sip.SipStackImpl
			// in the classpath
			e.printStackTrace();
			System.err.println(e.getMessage());
			if (e.getCause() != null)
				e.getCause().printStackTrace();
			System.exit(0);
		}

		try {
			headerFactory = sipFactory.createHeaderFactory();
			addressFactory = sipFactory.createAddressFactory();
			messageFactory = sipFactory.createMessageFactory();
			val lp = sipStack.createListeningPoint("127.0.0.1",myPort, "udp");

			val listener = this;

			val sipProvider = sipStack.createSipProvider(lp);
			System.out.println("udp provider " + sipProvider);
			sipProvider.addSipListener(listener);

		} catch {
		  
		  case ex:Exception =>
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			usage();
		}

	}
/*
	def main(args:Array[String]) {
	  System.out.println("hello world ShootMeCallSetup");
		//new ShootMeCallSetup().init();
	}
*/
 def main(args: Array[String]) {
		System.out.println("hi");
        //how much time to wait before we shut down?
	}

	def processIOException(exceptionEvent:IOExceptionEvent ) {
		System.out.println("IOException");

	}

	def processTransactionTerminated(transactionTerminatedEvent:TransactionTerminatedEvent ) {
		if (transactionTerminatedEvent.isServerTransaction())
			System.out.println("Transaction terminated event recieved"+ transactionTerminatedEvent.getServerTransaction());
		else
			System.out.println("Transaction terminated " + transactionTerminatedEvent.getClientTransaction());

	}

	def processDialogTerminated(dialogTerminatedEvent:DialogTerminatedEvent ) {
		System.out.println("Dialog terminated event recieved");
		val d = dialogTerminatedEvent.getDialog();
		System.out.println("Local Party = " + d.getLocalParty());

	}

}
