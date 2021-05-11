<%--

    Copyright 2021 OPS4J.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
	<title>Hello JSP Info</title>
	<meta charset="UTF-8">
</head>
<body>
	<h3><c:out value="${param['j1']}" /></h3>
	<h2><%= application.getInitParameter("param-from-fragment") %></h2>
	<h2><%= application.getAttribute("generated-attribute") %></h2>
	<h4>${applicationScope['osgi-bundlecontext']}</h4>
	<h5>${applicationScope['org.springframework.osgi.web.org.osgi.framework.BundleContext']}</h5>
	<h6>${fn:length(applicationScope['javax.servlet.context.orderedLibs'])}</h6>
</body>
</html>
