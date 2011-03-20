<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<html>
    <head>
        <title>Hello World</title>
    </head>
    <body>
        <f:view>
            <h:form id="mainForm">
              <h:panelGrid columns="2">
                <h:outputLabel for="name" value="Please enter your name" />
                <h:inputText id="name" value="#{helloWorld.name}" required="true"/>
                <h:commandButton value="Press me" action="#{helloWorld.send}"/>
                <h:messages showDetail="true" showSummary="false"/>
              </h:panelGrid>
            </h:form>
        </f:view>
    </body>
</html>
