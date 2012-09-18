package org.bluescale.util

import org.apache.log4j.Logger;


trait LogHelper {

	val loggerName = this.getClass.getName
    val logger = Logger.getLogger(loggerName)

	
	def log(msg: => String) { 
		logger.info(msg)
	}

	def debug(msg: => String) {
		println(msg)
	}

	def error(msg: =>String) {
		logger.error(msg)
	}
	
	def error(ex:Exception, msg: =>String) {
	  logger.error(msg)
	}

}
