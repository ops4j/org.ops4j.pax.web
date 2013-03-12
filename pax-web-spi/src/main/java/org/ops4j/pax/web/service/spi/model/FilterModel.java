/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.spi.model;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.Filter;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.spi.util.ConversionUtil;
import org.ops4j.pax.web.service.spi.util.Path;

public class FilterModel extends Model {

	private static final Set<String> VALID_DISPATCHER_VALUES = new HashSet<String>() {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			add("request");
			add("forward");
			add("include");
			add("error");
		}
	};

	private final Filter filter;
	private final String[] urlPatterns;
	private final String[] servletNames;
	private final Map<String, String> initParams;
	private final String name;
	private final Set<String> dispatcher = new HashSet<String>();

	public FilterModel(final ContextModel contextModel, final Filter filter,
			final String[] urlPatterns, final String[] servletNames,
			final Dictionary<String, ?> initParameter) {
		super(contextModel);
		NullArgumentException.validateNotNull(filter, "Filter");
		if (urlPatterns == null && servletNames == null) {
			throw new IllegalArgumentException(
					"Registered filter must have at least one url pattern or servlet name mapping");
		}

		this.filter = filter;
		this.urlPatterns = Path.normalizePatterns(urlPatterns);
		this.servletNames = servletNames;
		this.initParams = ConversionUtil.convertToMap(initParameter);
		String name = initParams.get(WebContainerConstants.FILTER_NAME);
		if (name == null) {
			name = getId();
		}
		this.name = name;
		setupDispatcher();
	}

	/*

     */
	private void setupDispatcher() {
		String dispatches = initParams
				.get(WebContainerConstants.FILTER_MAPPING_DISPATCHER);
		if (dispatches != null && dispatches.trim().length() > 0) {
			if (dispatches.indexOf(",") > -1) {
				// parse
				StringTokenizer tok = new StringTokenizer(dispatches.trim(),
						",");
				while (tok.hasMoreTokens()) {
					String element = tok.nextToken();
					if (element != null && element.trim().length() > 0) {
						if (VALID_DISPATCHER_VALUES.contains(element.trim()
								.toLowerCase())) {
							dispatcher.add(element.trim());
						} else {
							throw new IllegalArgumentException(
									"Incorrect value of dispatcher "
											+ element.trim());
						}
					}
				}
			} else {
				if (VALID_DISPATCHER_VALUES.contains(dispatches.trim()
						.toLowerCase())) {
					dispatcher.add(dispatches.trim());
				} else {
					throw new IllegalArgumentException(
							"Incorrect value of dispatcher "
									+ dispatches.trim());
				}
			}
		}
	}

	public Filter getFilter() {
		return filter;
	}

	public String getName() {
		return name;
	}

	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	public String[] getServletNames() {
		return servletNames;
	}

	public Map<String, String> getInitParams() {
		return initParams;
	}

	public String[] getDispatcher() {
		return dispatcher.toArray(new String[dispatcher.size()]);
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("id=").append(getId())
				.append(",urlPatterns=").append(Arrays.toString(urlPatterns))
				.append(",servletNames=").append(Arrays.toString(servletNames))
				.append(",filter=").append(filter).append(",context=")
				.append(getContextModel()).append("}").toString();
	}

}
