<!--

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

-->
<HTML>

<HEAD>
<TITLE>hello jsp</TITLE>
<!-- the variable, message, is declared and initialized -->
<%!
String message = "Hello, World, from JSP2";
%>
</HEAD>

<BODY>

<!-- the value of the variable, message, is inserted between h2 tags -->
<h2><%=message%></h2>

<!-- the java.util.Date method is executed and the result inserted between h3 tags -->
<h3>
<jsp:include page="includes/test.jsp" />
</h3>

</BODY>

</HTML>
