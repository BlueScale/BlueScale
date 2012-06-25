package org.bluescale.util



class ForUnitWrap {
	def foreach(f: String=>Unit): Unit =
	  f("")
 
	 
	
}

object ForUnitWrap {
	implicit def Unit_To_ForUnitWrap(f:Unit): ForUnitWrap = 
		new ForUnitWrap()
}