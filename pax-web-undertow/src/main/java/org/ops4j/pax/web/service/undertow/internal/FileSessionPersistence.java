/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.undertow.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import io.undertow.servlet.api.SessionPersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSessionPersistence implements SessionPersistenceManager {

	public static final Logger LOG = LoggerFactory.getLogger(FileSessionPersistence.class);

	private File sessionsDir;

	public FileSessionPersistence(File sessionsDir) {
		this.sessionsDir = sessionsDir;
	}

	@Override
	public void persistSessions(String deploymentName, Map<String, PersistentSession> sessionData) {
		if (deploymentName == null || "".equals(deploymentName.trim())) {
			deploymentName = "_ROOT_deployment";
		}
		Map<String, Object> map = new LinkedHashMap<>();
		for (Map.Entry<String, PersistentSession> e : sessionData.entrySet()) {
			Map<String, Object> mps = new LinkedHashMap<>();
			mps.put("expiration", e.getValue().getExpiration().getTime());
			mps.put("data", e.getValue().getSessionData());
			map.put(e.getKey(), mps);
		}
		if (sessionData.size() > 0) {
			try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(sessionsDir, deploymentName))))) {
				oos.writeObject(map);
			} catch (Exception e) {
				LOG.info("Error persisting sessions for deployment " + deploymentName, e);
			}
		} else {
			LOG.debug("No sessions to persist for deployment " + deploymentName);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, PersistentSession> loadSessionAttributes(String deploymentName, ClassLoader classLoader) {
		if (deploymentName == null || "".equals(deploymentName.trim())) {
			deploymentName = "_ROOT_deployment";
		}
		Map<String, PersistentSession> sessionData = new LinkedHashMap<>();
		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(sessionsDir, deploymentName))))) {
			Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>) ois.readObject();
			for (Map.Entry<String, Map<String, Object>> e : map.entrySet()) {
				long expiration = (Long) e.getValue().get("expiration");
				Map<String, Object> data = (Map<String, Object>) e.getValue().get("data");
				sessionData.put(e.getKey(), new PersistentSession(new Date(expiration), data));
			}
		} catch (FileNotFoundException e) {
			// ignore
		} catch (Exception e) {
			LOG.info("Error loading sessions for deployment " + deploymentName, e);
		}
		return sessionData;
	}

	@Override
	public void clear(String deploymentName) {
		new File(sessionsDir, deploymentName).delete();
	}

}
