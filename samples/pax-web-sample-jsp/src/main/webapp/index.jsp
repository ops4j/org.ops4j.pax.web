<%@ taglib prefix="log" 
    uri="http://logging.apache.org/log4j/tld/log" %>

<html>
    <body>
        <h1>Pax Web JSP Sample</h1>
        <p> Message:<%= System.getProperty("io.undertow.message") %></p>
        <log:info message="This is a log message."/>
    </body>
</html>