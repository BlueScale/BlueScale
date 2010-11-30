package com.bss.util

import org.apache.log4j.Logger;


trait LogHelper {

	val loggerName = this.getClass.getName
	Logger.getRootLogger().removeAllAppenders()
    //val logger = Logger.getLogger(loggerName)

	
	def log(msg: => String) { 

		//logger.info(msg)
	}

	def debug(msg: => String) {
		println(msg)
	}

	def error(msg: =>String) {
		//logger.error(msg)
	}

}
