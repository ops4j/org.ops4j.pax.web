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
package org.ops4j.pax.web.service.tomcat.internal;

import org.apache.tomcat.util.threads.TaskThreadFactory;

public class PaxWebTaskThreadFactory extends TaskThreadFactory {

	public PaxWebTaskThreadFactory(String namePrefix, boolean daemon, int priority) {
		super(namePrefix, daemon, priority);
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = super.newThread(r);

		// and this is it - we need TCCL of the thread to be a CL from pax-web-tomcat, not
		// from pax-web-tomcat-common which would be used because org.apache.tomcat.util.threads.TaskThreadFactory
		// is container in pax-web-tomcat-common bundle
		t.setContextClassLoader(TomcatFactory.class.getClassLoader());

		return t;
	}

}
