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
package org.ops4j.pax.web.jsp;

import jakarta.el.ELContext;
import jakarta.el.ELManager;
import jakarta.el.ELProcessor;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.ValueExpression;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ElTest {

	public static Logger log = LoggerFactory.getLogger(ElTest.class);

	@Test
	public void elApi() {
		// Provides an API for using EL in a stand-alone environment. It evaluates expressions without ${}/#{}
		ELProcessor processor = new ELProcessor();

		// Manages EL parsing and evaluation environment.
		ELManager manager = processor.getELManager();

		// Context information for expression parsing and evaluation.
		ELContext context = manager.getELContext();

		// Enables customization of variable, property, method call, and type conversion resolution behavior for EL expression evaluation.
		ELResolver resolver = context.getELResolver();

		// Provides an implementation for creating and evaluating EL expressions.
		ExpressionFactory expressionFactory = ExpressionFactory.newInstance();
		assertThat(expressionFactory.getClass().getName()).isEqualTo("org.apache.el.ExpressionFactoryImpl");

		manager.defineBean("model", new Model());

		assertNotNull(processor.eval("model"));
		assertNull(processor.eval("model.prop"));
		processor.eval("model.prop = 'Grzegorz'");
		assertThat((String) processor.eval("model.prop")).isEqualTo("Grzegorz");
		assertThat((String) processor.eval("model.hello()")).isEqualTo("Grzegorz");
		assertThat((String) processor.eval("model.hello(\"Grzegorz\")")).isEqualTo("[Grzegorz]");

		Model m = (Model) resolver.getValue(context, null, "model");
		assertNotNull(m);
		assertThat(m.hello()).isEqualTo("Grzegorz");

		// immediate and deferred evaluation - not relevant in plain EL usage. It is relevant in JSP/JSF
		ValueExpression ve = expressionFactory.createValueExpression(context, "${model.prop}", String.class);
		assertThat((String) ve.getValue(context)).isEqualTo("Grzegorz");
		ve = expressionFactory.createValueExpression(context, "#{model.prop}", String.class);
		assertThat((String) ve.getValue(context)).isEqualTo("Grzegorz");
	}

	public static class Model {

		private String prop;

		public String getProp() {
			return prop;
		}

		public void setProp(String prop) {
			this.prop = prop;
		}

		public String hello(String arg) {
			return String.format("[%s]", arg);
		}

		public String hello() {
			return prop;
		}
	}

}
