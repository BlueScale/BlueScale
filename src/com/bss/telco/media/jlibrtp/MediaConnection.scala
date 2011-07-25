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

import javax.sdp._
import com.bss.telco.api._
import com.bss.telco._

class MediaConnection extends Joinable[MediaConnection]
                      with Playable {

    var sdp = null
    
    private var _joinedTo:Option[Joinable[_]] = None
    
    var connState = UNCONNECTED()
   
    override def joinedTo = _joinedTo
    
    override def join(connection:Joinable[_], f:()=>Unit) {

    }
    
    def connectionState = connState

    def joinPlay(joinable:Joinable[_], f:()=>Unit) {
       //get SDP info for incoming data. 
       joinable.connect(this, false, ()=> {
           this._joinedTo = Some(joinable) 
    	   	//reconnect should not take an SDP, should take a joinable...and jsut connect with the SDP from it. 
           	play(()=>println("PLAYING"));
            //now take the other all's SDP and lets make sure we're listening to that.  now we can playw
            })
    }
    
    override def play(f:()=>Unit) {
        //play music
        f()
    }
    
    override def cancel(f:()=>Unit) {
      
    }

    def getLocalSdp() : SessionDescription = {
        return null
    }

    //PROTECTED STUFF FOR JOINABLE
    override protected[telco] def connect(join:Joinable[_], connectedCallback:()=>Unit) {
    	//store SDP somewhere
    	connectedCallback()
    }

    override protected[telco] def connect(join:Joinable[_], connectAnyMedia:Boolean, connectedCallback:()=>Unit) {//doesn't need to be here? 
    	connectedCallback()
	}
    
    protected[telco] def onConnect(f:()=>Unit) {
      //?
    }

    protected[telco] def unjoin(f:()=>Unit) {
      
    }//TODO: find out why protected isn't working here?  I'm accessing it from a subclass...
}



	



