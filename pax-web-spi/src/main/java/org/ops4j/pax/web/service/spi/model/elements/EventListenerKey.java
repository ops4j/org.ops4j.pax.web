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

import java.util.Objects;

public class EventListenerKey implements Comparable<EventListenerKey> {

	// fields matching org.ops4j.pax.web.service.spi.model.elements.ElementModel
	private final int serviceRank;
	private final long serviceId;
	// for listeners added without model, we consider only the position
	private final int ranklessPosition;
	private final int hashCode;

	private EventListenerKey(int serviceRank, long serviceId, int ranklessPosition, int hashCode) {
		this.serviceRank = serviceRank;
		this.serviceId = serviceId;
		this.ranklessPosition = ranklessPosition;
		this.hashCode = hashCode;
	}

	/**
	 * Create a key from {@link EventListenerModel}
	 *
	 * @param model
	 * @return
	 */
	public static EventListenerKey ofModel(EventListenerModel model) {
		return new EventListenerKey(model.getServiceRank(), model.getServiceId(), -1, System.identityHashCode(model));
	}

	/**
	 * Create a key from a position in the list of listeners added programatically
	 *
	 * @param position
	 * @return
	 */
	public static EventListenerKey ofPosition(int position) {
		return new EventListenerKey(0, 0L, position, 0);
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

		if (ranklessPosition >= 0 && o.ranklessPosition >= 0) {
			// simple compare by position - earlier = first
			return Integer.compare(this.ranklessPosition, o.ranklessPosition);
		}
		if (ranklessPosition >= 0) {
			// we are rankless, so should be called/used later
			return 1;
		}
		if (o.ranklessPosition >= 0) {
			return -1;
		}
		return Integer.compare(this.hashCode, o.hashCode);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EventListenerKey that = (EventListenerKey) o;
		if (ranklessPosition > 0) {
			return ranklessPosition == that.ranklessPosition;
		}
		return hashCode == that.hashCode;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ranklessPosition, hashCode);
	}

}
