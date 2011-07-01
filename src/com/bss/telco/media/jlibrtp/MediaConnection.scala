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

package com.bss.telco.media.jlibrtp



class MediaConnection extends Joinable
                      with Playable {

    var sdp = null
    
    def join(connection:Joinable[_], f:()=>Unit) {

    }


    def joinPlay(joinable:Joinable[_], f:()=>Unit) {
       //get SDP info for incoming data. 

       joinable.connect(getLocalSdp(), false, ()=> {
            otherCall.joinedTo = Some(this)
            //reconnect should not take an SDP, should take a joinable...and jsut connect with the SDP from it. 
            
            //now take the other all's SDP and lets make sure we're listening to that.  now we can playw
            play(f)



            }
        })
    }
    
    def play(f:()=>Unit) {
        //play music

        f()
    }

    def getLocalSdp() = {
        

    }


}


/*
  	var joinedTo:Option[Joinable[_]] = None
  	
    var unjoinCallback:Option[(Joinable[_],T)=>Unit] = None
	
	def join(connection:Joinable[_], f:()=>Unit)

    def sdp:SessionDescription

    def connectionState:ConnectionState //Possibly not needed here...

    protected[telco] def reconnect(sdp:SessionDescription, f:()=>Unit)

    //protected[telco] def connect(sdp:SessionDescription, connectedCallback:()=>Unit)
    
    protected[telco] def connect(sdp:SessionDescription, connectAnyMedia:Boolean, connectedCallback:()=>Unit)
    
    //protected[telco] 
    def silence(f:()=>Unit)

    protected[telco] def onConnect(f:()=>Unit)

    protected[telco] def unjoin(j:Joinable[_], f:()=>Unit) //TODO: find out why protected isn't working here?  I'm accessing it from a subclass...
*/



