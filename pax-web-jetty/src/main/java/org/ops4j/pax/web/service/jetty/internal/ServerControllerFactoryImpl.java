/*
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.jetty.internal;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections; 
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;

class ServerControllerFactoryImpl implements ServerControllerFactory {

	private static class ServiceMap<T> extends AbstractMap<T, Integer> {
		
		private static class ComparatorImpl<T> implements Comparator<Map.Entry<T, Integer>> {
			
			@Override
			public int compare(Map.Entry<T, Integer> entry1, Map.Entry<T, Integer> entry2) {
				return entry1.getValue().compareTo(entry2.getValue());
			}
			
		}
		
		private Comparator<Map.Entry<T, Integer>> COMPARATOR = new ComparatorImpl<T>();
		private List<Map.Entry<T, Integer>> entries;
			
		public ServiceMap() {
			entries = new ArrayList<>();
		}
		
		@Override
		public Set<Map.Entry<T, Integer>> entrySet() {
			return new AbstractSet<Map.Entry<T, Integer>>() {

				@Override
				public Iterator<Map.Entry<T, Integer>> iterator() {
					return entries.iterator();
				}

				@Override
				public int size() {
					return entries.size();
				}
				
			};
		}

		@Override
		public Integer put(T service, Integer ranking) {
			Map.Entry<T, Integer> entry = new AbstractMap.SimpleImmutableEntry<>(service, ranking);
			entries.add(entry);
			Collections.sort(entries, COMPARATOR);
			return null;
		}
		
	}

	private Bundle bundle;
	private Map<Handler, Integer> handlers = new ServiceMap<>();
	private Map<Connector, Integer> connectors = new ServiceMap<>();
	private Map<Customizer, Integer> customizers = new ServiceMap<>();
	private List<ServerControllerImpl> serverControllers = new LinkedList<>();
	private Comparator<?> priorityComparator;

	public ServerControllerFactoryImpl(Bundle bundle, Comparator<?> priorityComparator) {
		this.bundle = bundle;
		this.priorityComparator = priorityComparator;
	}

	public void addHandler(Handler handler, int ranking) {
		synchronized (ServerControllerFactoryImpl.this.serverControllers) {
    		for (ServerControllerImpl serverController : this.serverControllers) {
    			serverController.jettyServer.addHandler(handler); 
    		}
    		this.handlers.put(handler, ranking);
		}
	}

	public void removeHandler(Handler handler) {
		synchronized (ServerControllerFactoryImpl.this.serverControllers) {
    		this.handlers.remove(handler);
    		for (ServerControllerImpl serverController : this.serverControllers) {
    			serverController.jettyServer.removeHandler(handler);
    		}
		}
	}

	public void addConnector(Connector connector, int ranking) {
		synchronized (ServerControllerFactoryImpl.this.serverControllers) {
    		for (ServerControllerImpl serverController : this.serverControllers) {
    			serverController.jettyServer.addConnector(connector);
    		}
    		this.connectors.put(connector, ranking);
		}
	}

	public void removeConnector(Connector connector) {
		synchronized (ServerControllerFactoryImpl.this.serverControllers) {
    		this.connectors.remove(connector);
    		for (ServerControllerImpl serverController : this.serverControllers) {
    			serverController.jettyServer.removeConnector(connector);
    		}
		}
	}
	
	public void addCustomizer(Customizer customizer, int ranking) {
		synchronized (ServerControllerFactoryImpl.this.serverControllers) {
			List<Customizer> customizers = Collections.singletonList(customizer);
    		for (ServerControllerImpl serverController : this.serverControllers) {
    			serverController.addCustomizers(customizers);
    		}
    		this.customizers.put(customizer, ranking);
		}
	}

	public void removeCustomizer(Customizer customizer) {
		synchronized (ServerControllerFactoryImpl.this.serverControllers) {
    		this.customizers.remove(customizer);
			List<Customizer> customizers = Collections.singletonList(customizer);
    		for (ServerControllerImpl serverController : this.serverControllers) {
				serverController.removeCustomizers(customizers);
    		}
		}
	}

	@Override
	public ServerController createServerController(ServerModel serverModel) {
		return new ServerControllerImpl(new JettyFactoryImpl(serverModel, bundle, priorityComparator), priorityComparator) {

			@Override
			public synchronized void start() {
				synchronized (serverControllers) {
					for (Connector connector : connectors.keySet()) {
						jettyServer.addConnector(connector);
					}
					super.start();
					if (!customizers.isEmpty()) {
						addCustomizers(ServerControllerFactoryImpl.this.customizers.keySet());
					}
					for (Handler handler : handlers.keySet()) {
						jettyServer.addHandler(handler);
					}
					serverControllers.add(this);
				}
			}

			@Override
			public synchronized void stop()
			{
				synchronized (serverControllers) {
					serverControllers.remove(this);
					for (Handler handler : ServerControllerFactoryImpl.this.handlers.keySet()) {
						jettyServer.removeHandler(handler);
					}
					if (!customizers.isEmpty()) {
						removeCustomizers(customizers.keySet());
					}
    				super.stop();
    				for (Connector connector : connectors.keySet()) {
    					jettyServer.addConnector(connector);
    				}
				}
			}
			
		};
	}


}