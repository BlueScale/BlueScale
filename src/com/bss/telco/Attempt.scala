package com.bss.telco

/*
 * Might be useful for this to be non-telco specific...
 * we can always change the object to further modularize it
 */
class Attempt[T] private (run:Option[()=>Unit], runParam:Option[T=>Unit], fail:Option[T=>Unit]  ) {
 
	def execute(t:T) = 
		runParam match {
		  case Some(x) => (runParam.get)(t)
		  case None => (run.get)()
		}           
	      
 	def failure(t:T) = fail.get(t) 
 		
}
/*
object Attempt {

	private var defaultFail:Option[(Connection)=>Unit] = None
  
	def setDefaultFail(df:Connection=>Unit) = defaultFail = Some(df)
 
	def apply(run:Connection=>Unit) : Attempt[Connection] = 
	    new Attempt[Connection]( None, Some(run),  defaultFail)
	   
	def apply(run:()=>Unit) : Attempt[Connection] = 
	    new Attempt[Connection]( Some(run), None,  defaultFail)
}
*/

