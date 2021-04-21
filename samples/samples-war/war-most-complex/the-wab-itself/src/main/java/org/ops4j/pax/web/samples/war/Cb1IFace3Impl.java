/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.samples.war;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.ops4j.pax.web.samples.war.cb1.utils.Cb1IFace3;

/**
 * A concrete class that implements and interface from container-bundle-1, which extends an interface from
 * container-bundle-3, which extends {@link javax.servlet.ServletRegistration}.
 * And I want to have a {@link javax.servlet.ServletContainerInitializer} which has {@link javax.servlet.ServletRegistration}
 * among values of {@link javax.servlet.annotation.HandlesTypes}.
 */
public class Cb1IFace3Impl implements Cb1IFace3 {

	@Override
	public Set<String> addMapping(String... urlPatterns) {
		return null;
	}

	@Override
	public Collection<String> getMappings() {
		return null;
	}

	@Override
	public String getRunAsRole() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getClassName() {
		return null;
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		return false;
	}

	@Override
	public String getInitParameter(String name) {
		return null;
	}

	@Override
	public Set<String> setInitParameters(Map<String, String> initParameters) {
		return null;
	}

	@Override
	public Map<String, String> getInitParameters() {
		return null;
	}

}
