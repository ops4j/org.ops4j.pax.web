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

import org.ops4j.pax.web.service.whiteboard.ErrorPageMapping;

/**
 * Default implementation of
 * {@link org.ops4j.pax.web.service.whiteboard.ErrorPageMapping}
 */
public class DefaultErrorPageMapping extends AbstractContextRelated implements ErrorPageMapping {

	/** Error code or fqn of Exception */
	private String[] errors = new String[0];

	/** Location of error page */
	private String location;

	@Override
	public String[] getErrors() {
		return errors;
	}

	public void setErrors(String[] errors) {
		this.errors = errors;
	}

	@Override
	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	@Override
	public String toString() {
		return "DefaultErrorPageMapping{"
				+ "errors=" + Arrays.asList(errors)
				+ ",location=" + location
				+ "}";
	}

}
