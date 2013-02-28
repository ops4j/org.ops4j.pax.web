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
<h2><font color="#AA0000"><%= message%></font></h2>

</BODY>

</HTML>