/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.spi.config;

public interface LogConfiguration {

	/**
	 * Whether request logging (at entire server level) should be enabled.
	 * @return
	 */
	Boolean isLogNCSAFormatEnabled();

	/**
	 * Returns a directory to store request log files
	 * @return
	 */
	String getLogNCSADirectory();

	/**
	 * Returns a filename to keep current request log file
	 * @return
	 */
	String getLogNCSAFile();

	/**
	 * Whether request log file should be opened in append mode
	 * @return
	 */
	Boolean isLogNCSAAppend();

	/**
	 * Date format to use when current file is renamed during rollover
	 * @return
	 */
	String getLogNCSAFilenameDateFormat();

	/**
	 * How many files to keep during rollover
	 * @return
	 */
	Integer getLogNCSARetainDays();

	/**
	 * Whether to use <em>extended</em> request log format (including {@code Referer} and {@code User-Agent} headers)
	 * @return
	 */
	Boolean isLogNCSAExtended();

	/**
	 * Timezone to use for request log file, when formatting timestamps
	 * @return
	 */
	String getLogNCSATimeZone();

}
