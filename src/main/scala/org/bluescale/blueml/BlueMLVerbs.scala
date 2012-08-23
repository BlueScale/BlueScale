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
* Please contact us at www.BlueScale.org
*
*/

package org.bluescale.blueml

trait BlueMLVerb

case class Play(val loop:Int = 0,
                val mediaUrl:String,
                val gather:Option[Gather],
                val url:String) extends BlueMLVerb 

case class Dial(val number:String,
                val from:String,
                val url:String,
                val ringLimit:Int) extends BlueMLVerb

case class DialVoicemail(val number:String,
                    val from:String,
                    val url:String) extends BlueMLVerb

case class Hangup(val url:String) extends BlueMLVerb

case class Auth(val password:String) extends BlueMLVerb

case class Hold() extends BlueMLVerb

case class Gather(val digitLimit: Int,
				val url: String) extends BlueMLVerb