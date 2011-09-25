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

package org.bluescale.telco.media.jlibrtp

import java.util.concurrent.ConcurrentHashMap
import org.bluescale.telco.api._
import java.io.FileOutputStream
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.URL

object MediaFileManager {
  
	var filePath:Option[String] = None
	
	val dataMap = new ConcurrentHashMap[MediaConnection,Array[Byte]]()
  
	def addMedia(mc:MediaConnection, data:Array[Byte]) =
		dataMap.put(mc, safeConcat(data, dataMap.get(mc) ) )
	 
	def safeConcat(stuff:Array[Byte]*) : Array[Byte] = 
		stuff.filter( _ != null).flatten.toArray
	
	  
	def finishAddMedia(mc:MediaConnection) : String = {
		val path = filePath.getOrElse(".") + mc.hashCode() + ".wav"
		val fileStream = new FileOutputStream(path)
		fileStream.write(dataMap.get(mc))
		fileStream.close()
		return path
	}

	def getInputStream(url:String) : BufferedInputStream = {
		throw new Exception("NOT IMPLEMENTED")
	}
}