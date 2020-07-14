/*
 * Copyright 2013 Achim Nierbeck.
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
package org.ops4j.pax.web.service.spi.model.elements;

import java.util.Arrays;

import org.ops4j.pax.web.service.spi.model.events.WelcomeFileEventData;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping;

/**
 * @author achim
 */
public class WelcomeFileModel extends ElementModel<WelcomeFileMapping, WelcomeFileEventData> {

	private final String[] welcomeFiles;
	private final boolean redirect;

	public WelcomeFileModel(String[] welcomeFiles, boolean redirect) {
		if (welcomeFiles != null) {
			this.welcomeFiles = Arrays.copyOf(welcomeFiles, welcomeFiles.length);
		} else {
			// which may also mean "unregister all"
			this.welcomeFiles = new String[0];
		}
		this.redirect = redirect;
	}

	@Override
	public void register(WhiteboardWebContainerView view) {
		view.registerWelcomeFiles(this);
	}

	@Override
	public void unregister(WhiteboardWebContainerView view) {
		view.unregisterWelcomeFiles(this);
	}

	@Override
	public WelcomeFileEventData asEventData() {
		WelcomeFileEventData welcomeFileEventData = new WelcomeFileEventData(this.welcomeFiles, redirect);
		setCommonEventProperties(welcomeFileEventData);
		return welcomeFileEventData;
	}

	public String[] getWelcomeFiles() {
		return welcomeFiles;
	}

	public boolean isRedirect() {
		return redirect;
	}

	@Override
	public String toString() {
		return "WelcomeFileModel{id=" + getId()
				+ ",redirect='" + redirect + "'"
				+ (welcomeFiles == null ? "" : ",welcomeFiles=" + Arrays.toString(welcomeFiles))
				+ ",contexts=" + contextModels
				+ "}";
	}

	@Override
	public Boolean performValidation() {
		for (String wf : welcomeFiles) {
			if (wf == null || "".equals(wf.trim()) || wf.contains("/")) {
				throw new IllegalArgumentException("Welcome file \""
						+ wf + "\" should not be empty and should not contain \"/\" character.");
			}
		}

		return Boolean.TRUE;
	}

}
