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
package org.ops4j.pax.web.test.jsp;

import javax.el.ELContext;
import javax.el.ELManager;
import javax.el.ELProcessor;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * This test checks if pax-web-jsp bundle contains everything we need for Expression Language
 */
public class ElTest {

    public static Logger log = LoggerFactory.getLogger(ElTest.class);

    @Test
    public void noFactory() throws Exception {
        // can't use directly, because pax-web-jsp private packages it and IDEs see only what's provided
        // by submodule prior to maven magic
        assertNotNull(Class.forName("org.apache.el.ExpressionFactoryImpl"));
    }

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
        assertThat(expressionFactory.getClass().getName(), equalTo("org.apache.el.ExpressionFactoryImpl"));

        manager.defineBean("model", new Model());

        assertNotNull(processor.eval("model"));
        assertNull(processor.eval("model.prop"));
        processor.eval("model.prop = 'Grzegorz'");
        assertThat(processor.eval("model.prop"), equalTo("Grzegorz"));
        assertThat(processor.eval("model.hello()"), equalTo("Grzegorz"));
        assertThat(processor.eval("model.hello(\"Grzegorz\")"), equalTo("[Grzegorz]"));

        Model m = (Model) resolver.getValue(context, null, "model");
        assertNotNull(m);
        assertThat(m.hello(), equalTo("Grzegorz"));

        ValueExpression ve = expressionFactory.createValueExpression(context, "${model.prop}", String.class);
        assertThat(ve.getValue(context), equalTo("Grzegorz"));
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
