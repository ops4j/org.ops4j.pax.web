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
package org.ops4j.pax.web.samples.vaadin08;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@SpringUI
@Theme("mytheme")
public class MyUI extends UI {

    @Autowired
    private Greeter greeter;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/application.properties")) {
            props.load(is);
        } catch (IOException ignored) {
        }

        final VerticalLayout layout = new VerticalLayout();

        final TextField name = new TextField();
        name.setCaption("Type your name here:");

        Button button = new Button("Click Me");
        button.addClickListener(e -> {
            layout.addComponent(new Label("Thanks " + name.getValue() + ", it works!"));
        });

        Label label1 = new Label(String.format("%s/%s/%s",
                props.getProperty("groupId"), props.getProperty("artifactId"), props.getProperty("version")));
        Label label2 = new Label(greeter.sayHello());

        layout.addComponents(label1, label2, name, button);

        setContent(layout);
    }

    // no need to create @WebServlet when using Spring with Vaadin
//	@WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
//	@VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
//	public static class MyUIServlet extends VaadinServlet {
//	}

}
