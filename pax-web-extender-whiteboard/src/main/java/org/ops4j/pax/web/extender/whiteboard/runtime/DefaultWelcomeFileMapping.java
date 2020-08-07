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
package org.ops4j.pax.web.extender.whiteboard.runtime;

import java.util.Arrays;

import org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping;

/**
 * Default implementation of
 * {@link org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping}.
 *
 * @author dsklyut
 * @since 0.7.0
 */
public class DefaultWelcomeFileMapping extends AbstractContextRelated implements WelcomeFileMapping {

	/** welcome files */
	private String[] welcomeFiles;

	/** redirect flag true - send redirect false - use forward */
	private boolean redirect;

	@Override
	public String[] getWelcomeFiles() {
		return welcomeFiles;
	}

	public void setWelcomeFiles(String[] welcomeFiles) {
		this.welcomeFiles = welcomeFiles;
	}

	@Override
	public boolean isRedirect() {
		return redirect;
	}

	public void setRedirect(boolean redirect) {
		this.redirect = redirect;
	}

	@Override
	public String toString() {
		return "DefaultWelcomeFileMapping{"
				+ "redirect=" + redirect
				+ ", welcomeFiles=" + Arrays.toString(welcomeFiles)
				+ '}';
	}

}
