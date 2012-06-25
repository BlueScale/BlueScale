package org

package object bluescale  {
  
	type Foreachable[+A] = {
		def foreach[U](f: A=>U): Unit  
	}


}