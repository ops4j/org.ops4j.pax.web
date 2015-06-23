<HTML>

<HEAD>
<TITLE>hello jsp</TITLE>
<!-- the variable, message, is declared and initialized -->
<%!
String message = "Hello, World, from JSP";
%>
</HEAD>

<BODY>

<!-- the value of the variable, message, is inserted between h2 tags -->
<h2><font color="#AA0000"><%=message%></font></h2>

<h3><font color="#AA0000">
<!-- the java.util.Date method is executed and the result inserted between h3 tags -->
<jsp:include page="includes/test.jsp" />
</font></h3>

<c:set var="hello" value="Hello World"/>
    <h1>
    <c:out value="${hello}"/>
    </h1>

</BODY>

</HTML>
