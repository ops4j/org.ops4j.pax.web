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
package org.ops4j.pax.web.service.spi.model.elements;

public class EventListenerKey implements Comparable<EventListenerKey> {

	// fields matching org.ops4j.pax.web.service.spi.model.elements.ElementModel
	private final int serviceRank;
	private final long serviceId;
	// for listeners added without model, we consider only the position
	private final int ranklessPosition;

	private EventListenerKey(int serviceRank, long serviceId, int ranklessPosition) {
		this.serviceRank = serviceRank;
		this.serviceId = serviceId;
		this.ranklessPosition = ranklessPosition;
	}

	/**
	 * Create a key from {@link EventListenerModel}
	 *
	 * @param model
	 * @return
	 */
	public static EventListenerKey ofModel(EventListenerModel model) {
		return new EventListenerKey(model.getServiceRank(), model.getServiceId(), -1);
	}

	/**
	 * Create a key from a position in the list of listeners added programatically
	 *
	 * @param position
	 * @return
	 */
	public static EventListenerKey ofPosition(int position) {
		return new EventListenerKey(0, 0L, position);
	}

	public int getServiceRank() {
		return serviceRank;
	}

	public long getServiceId() {
		return serviceId;
	}

	public int getRanklessPosition() {
		return ranklessPosition;
	}

	@Override
	public int compareTo(EventListenerKey o) {
		int c1 = Integer.compare(this.serviceRank, o.serviceRank);
		if (c1 != 0) {
			// higher rank - "lesser" service in terms of order
			return -c1;
		}
		// higher service id - "greater" service in terms of order
		int c2 = Long.compare(this.serviceId, o.serviceId);
		if (c2 != 0) {
			return c2;
		}

		// we need some fallback here - prefer model created earlier
		return Integer.compare(this.ranklessPosition, o.ranklessPosition);
	}

}
