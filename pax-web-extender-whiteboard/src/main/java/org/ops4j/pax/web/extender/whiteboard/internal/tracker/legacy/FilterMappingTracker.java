/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy;

import java.util.Arrays;
import jakarta.servlet.Filter;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.events.FilterEventData;
import org.ops4j.pax.web.service.whiteboard.FilterMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * <p>Tracks {@link FilterMapping}s.</p>
 *
 * <p>This customizer customizes {@code FilterMapping -> ElementModel<Filter> = FilterModel}</p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.4.0, April 05, 2008
 */
public class FilterMappingTracker extends AbstractMappingTracker<FilterMapping, Filter, FilterEventData, FilterModel> {

	private FilterMappingTracker(final WhiteboardExtenderContext whiteboardExtenderContext, final BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<FilterMapping, FilterModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {
		return new FilterMappingTracker(whiteboardExtenderContext, bundleContext).create(FilterMapping.class);
	}

	@Override
	protected FilterModel doCreateElementModel(Bundle bundle, FilterMapping filterMapping, Integer rank, Long serviceId) {

		// pass everything to a handy builder
		String[] dispatcherTypeNames = Arrays.stream(filterMapping.getDispatcherTypes())
				.map(Enum::name).toArray(String[]::new);

		FilterModel.Builder builder = new FilterModel.Builder()
				.withRegisteringBundle(bundle)
				.withServiceRankAndId(rank, serviceId)
				.withUrlPatterns(filterMapping.getUrlPatterns())
				.withServletNames(filterMapping.getServletNames())
				.withRegexMapping(filterMapping.getRegexPatterns())
				.withFilterName(filterMapping.getFilterName())
				.withInitParams(filterMapping.getInitParameters())
				.withAsyncSupported(filterMapping.getAsyncSupported())
				.withDispatcherTypes(dispatcherTypeNames);

		// handle actual source of the servlet
		if (filterMapping.getFilterClass() != null) {
			builder.withFilterClass(filterMapping.getFilterClass());
		} else {
			builder.withFilter(filterMapping.getFilter());
		}

		return builder.build();
	}

}
