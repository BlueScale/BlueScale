#BlueScale

BlueScale is an Open Source platform allowing web developers to create telecom and VoIP related products or services.
It uses a simple REST front end to control the scalable SIP server at its core.  



## Launching

After unzipping the distribution, you need to create a BlueScaleConfig.xml file by copying the example BlueScaleConfig.Sample.xml. 
ListeningAddress and ListeningPort tag values must be a local IP and Port the server can bind to.

ContactAddress must be the external IP your SIP trunking Provider will be sending SIP to, and needs to be forwarding to your machine.

DestAddress and DestPort must be the gateway of your SIP trunking provider.

CallbackUrl should point to the address of your web application that BlueScale will post to when an incoming call occurs. 

Once you finish editing your config, launch the run.sh script and you're ready to start building telco apps!



## System Requirements

- Java 1.6 
- a SIP trunking provider/gateway


## Example: Forward Incoming Calls

1. BlueScale will post a call status with the following parameters to URL specified in BlueScaleConifg.xml.  
    <pre>
     CallId = 'A1234...',
     To     = '7147779999',
     From   = '9494445566',
     CallStatus = 'Unconnected',
     Direction  = 'Incoming'
     </pre>

2. Your application will respond to the post with BlueML, an XML Dialect for controlling BlueScale.  This will cause the phone specified in the number tag to ring, and connect it to the incoming call.  
        
        <Response>
            <Dial>
                <Number>1112223333</Number>
                <From>9494445566</From>
                <Action>http://127.0.0.1:8081/Status</Action>
            </Dial>
        </Response>

3. If and when the phone is answered that was specified in the Number tag, BlueScale will post a status for that call, with the same parameters as step one.
     <pre>
     CallId = 'B6789...',
     To     = '1112223333',
     From   = '9494445566',
     CallStatus = 'Connected',
     Direction  = 'Outgoing'
     </pre>

4. BlueScale will then post a conversation status with the following parameters.
     <pre>
     FirstCallId  = 'A1234...'
     SecondCallId = 'B6789...'
     ConversationStatus = 'Connected'
     </pre>

5. When either call hangs up, the connected call will also be hung up, and BlueScale will post back the new Disconnected Status.
    <pre>
    FirstCallId  = 'A1234...'
    SecondCallId = 'B6789...'
    ConversationStatus = 'Disconnected'
    </pre>

## Example: Click to Call

1. Execute an HTTP Post to the Bluescale URL http://[bluesacleIP]/Calls/ specifying the first number you wish to connect to:
    <pre>
    To      =  '7145551234'
    From    =  '9497773456'
    Url     =  'http://127.0.0.1/'
    </pre>

2. BlueScale will post a call status with the following parameteres:
    <pre>
    CallId = '123abc...',
    To      =  '7145551234'
    From    =  '9497773456'
    CallStatus = 'Connected',
    Direction  = 'Outgoing'
    </pre>

3.  Your application will respond to the post with BlueML, an XML Dialect for controlling BlueScale.  This will cause the phone specified in the number tag to ring, and connect it to the incoming call.  
        
        <Response>
            <Dial>
                <Number>9497773456</Number>
                <From>7145551234</From>
                <Action>http://127.0.0.1:8081/Status</Action>
            </Dial>
        </Response>

4.  If and when the phone is answered that was specified in the Number tag, BlueScale will post a status for that call, with the same parameters as step two.
     <pre>
     CallId  = '567def...',
     To       = '1112223333',
     From   = '9494445566',
     CallStatus = 'Connected',
     Direction  = 'Outgoing'
     </pre>

5. BlueScale will then post a conversation status with the following parameters.
     <pre>
     FirstCallId  = '123abc...'
     SecondCallId = '567def...'
     ConversationStatus = 'Connected'
     </pre>

6. When either call hangs up, the connected call will also be hung up, and BlueScale will post back the new Disconnected Status.
    <pre>
    FirstCallId  = '123abc...'
    SecondCallId = '567def...'
    ConversationStatus = 'Disconnected'
    </pre>

    


## TODO
- Ability to register SIP phones and expose the rigister through the Web API

- Media support for playing .wav files to connected calls.

- DTMF detection to allow postbacks when the keypad is pressed.

- Official performance numbers.  I'm pushing close to a thousand concurrent calls on my dual core laptop, more tests to come on beefier boxes.
