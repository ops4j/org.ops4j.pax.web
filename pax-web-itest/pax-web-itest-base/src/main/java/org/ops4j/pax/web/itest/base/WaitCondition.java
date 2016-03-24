/*
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
package org.ops4j.pax.web.itest.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marc Klinger - mklinger[at]nightlabs[dot]de
 */
public abstract class WaitCondition {
	private static final long RETRY_DURATION_MILLIS = 10000;
	private static final long SLEEP_DURATION_MILLIS = 200;
	private static final Logger LOG = LoggerFactory.getLogger(WaitCondition.class);


	private String description;

	protected WaitCondition(String description) {
		this.description = description;
	}


	protected String getDescription() {
		return description;
	}

	protected abstract boolean isFulfilled() throws Exception;

	/**
	 * Execute this WaitCondition with 10s retyDuration and 200ms Thread-sleep
	 * @throws InterruptedException
	 */
	public void waitForCondition() throws InterruptedException {
		waitForCondition(RETRY_DURATION_MILLIS, SLEEP_DURATION_MILLIS);
	}

	/**
	 * Execute this WaitCondition with the given parameters. The Thread sleeps for 200ms until retry.
	 * In case of timeout a log-message is printed
	 * @param retryDuration the duration in which this condition is retried
	 * @throws InterruptedException
	 */
	public void waitForCondition(long retryDuration) throws InterruptedException {
		waitForCondition(retryDuration, SLEEP_DURATION_MILLIS);
	}

	/**
	 * Execute this WaitCondition with the given parameters. In case of timeout a log-message is printed
	 * @param retryDuration the duration in which this condition is retried
	 * @param sleepDuration the duration the Thread sleeps until next attempt
	 * @throws InterruptedException
	 */
	public void waitForCondition(long retryDuration, long sleepDuration) throws InterruptedException {
		waitForCondition(
				retryDuration,
				sleepDuration,
				() -> LOG.warn("Waited for '{}' for {} ms but condition is still not fulfilled",
						getDescription(),
						retryDuration));
	}

	/**
	 * Execute this WaitCondition with the given parameters
	 * @param retryDuration the duration in which this condition is retried
	 * @param sleepDuration the duration the Thread sleeps until next attempt
	 * @param timeoutFunction Runnable which is executed when the condition was nut fullfilled within the given duration
	 * @throws InterruptedException
     */
	public void waitForCondition(long retryDuration, long sleepDuration, Runnable timeoutFunction) throws InterruptedException {
		if(retryDuration <= 0 || sleepDuration <= 0){
			throw new IllegalArgumentException("retryDuration and sleepDuration must be positive!");
		}
		if(timeoutFunction == null){
			throw new IllegalArgumentException("timeoutFunction must be set!");
		}
		//CHECKSTYLE:OFF
		long startTime = System.currentTimeMillis();
		try {
			while (!isFulfilled() && System.currentTimeMillis() < startTime + retryDuration) {
				Thread.sleep(sleepDuration);
			}
			if (!isFulfilled()) {
				timeoutFunction.run();
			} else {
				LOG.info("Successfully waited for '{}' for {} ms", getDescription(), System.currentTimeMillis() - startTime);
			}
		} catch (Exception e) {
			throw new RuntimeException("Error waiting for '" + getDescription() +"'", e);
		}
		//CHECKSTYLE:ON
	}
}