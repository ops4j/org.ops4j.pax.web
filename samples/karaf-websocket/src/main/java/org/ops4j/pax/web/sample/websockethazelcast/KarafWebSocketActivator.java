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
package org.ops4j.pax.web.sample.websockethazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.instance.GroupProperties;

import org.eclipse.jetty.websocket.api.Session;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Jimmy Pannier
 */
public class KarafWebSocketActivator implements BundleActivator {

	// noeud hazelcast
	private static HazelcastInstance hazelcastNode;

	// topic
	private static ITopic<Object> hazelcastTopic;

	private static final Set<Session> clientSessions = Collections.synchronizedSet(new HashSet<Session>());

	public static final long WEBSOCKET_TIMEOUT = -1;

	public static void onMessage(String message, Session client) {
		hazelcastTopic.publish(message);
	}

	public static void registerConnection(Session session) {
		session.setIdleTimeout(KarafWebSocketActivator.WEBSOCKET_TIMEOUT);
		clientSessions.add(session);

	}

	public static void unregisterConnection(Session session) {
		clientSessions.remove(session);
	}

	/**
	 * méthode qui envoie le message à tous les clients connectés
	 *
	 * @param message
	 */
	public void publishToAll(String message) {
		StringBuffer buf = new StringBuffer(message);
		synchronized (clientSessions) {
			System.out.println("send message '" + message + "' to " + clientSessions.size() + " clients");

			Iterator<Session> it = clientSessions.iterator();
			while (it.hasNext()) {
				Session session = it.next();
				try {
					session.getRemote().sendString(buf.toString());
				} catch (IOException exception) {
					clientSessions.remove(session);
					it = clientSessions.iterator();
					try {
						session.close();
					} catch (Exception e) {
					}
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {

		System.out.println("******************************** WebsocketActivator STARTUP **********************************");

		Config config = new Config();
		config.setProperty(GroupProperties.PROP_VERSION_CHECK_ENABLED, "false");
		config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);

		hazelcastNode = Hazelcast.newHazelcastInstance(config);
		hazelcastTopic = hazelcastNode.getTopic("cloud");
		hazelcastTopic.addMessageListener(new MessageListener<Object>() {

			public void onMessage(Message<Object> arg0) {
				publishToAll(arg0.getMessageObject().toString());
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		hazelcastNode.shutdown();
		System.out.println("******************************** WebsocketActivator SHUTDOWN **********************************");
	}

}