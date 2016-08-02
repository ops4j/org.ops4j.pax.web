/*
 * Copyright 2014 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.jsp;

import org.apache.juli.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps SLF4J logger for JULI.
 *
 * @author Harald Wellmann
 */
public class Slf4jLog implements Log {


	private Logger logger;


	public Slf4jLog() {
	}

	public Slf4jLog(String name) {
		this.logger = LoggerFactory.getLogger(name);
	}

	@Override
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	@Override
	public boolean isFatalEnabled() {
		return logger.isErrorEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	@Override
	public void trace(Object message) {
		logger.trace(String.valueOf(message));
	}

	@Override
	public void trace(Object message, Throwable t) {
		logger.trace(String.valueOf(message), t);
	}

	@Override
	public void debug(Object message) {
		logger.debug(String.valueOf(message));
	}

	@Override
	public void debug(Object message, Throwable t) {
		logger.debug(String.valueOf(message), t);
	}

	@Override
	public void info(Object message) {
		logger.info(String.valueOf(message));
	}

	@Override
	public void info(Object message, Throwable t) {
		logger.info(String.valueOf(message), t);
	}

	@Override
	public void warn(Object message) {
		logger.warn(String.valueOf(message));
	}

	@Override
	public void warn(Object message, Throwable t) {
		logger.warn(String.valueOf(message), t);
	}

	@Override
	public void error(Object message) {
		logger.error(String.valueOf(message));
	}

	@Override
	public void error(Object message, Throwable t) {
		logger.error(String.valueOf(message), t);
	}

	@Override
	public void fatal(Object message) {
		logger.error(String.valueOf(message));
	}

	@Override
	public void fatal(Object message, Throwable t) {
		logger.error(String.valueOf(message), t);
	}

}