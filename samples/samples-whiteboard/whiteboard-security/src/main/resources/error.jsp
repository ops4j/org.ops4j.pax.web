<%--

    Copyright 2023 OPS4J.

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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Pax Web Security</title>
</head>
<body>
<p style="color: red; font-style: italic">
    HTTP ${code}: ${msg}
</p>
<p>
    <b>In default (<code>/</code>) context:</b><br/>
    <a href="<c:url context="/" value="/app" />">Go to anonymous area</a><br/>
    <a href="<c:url context="/" value="/secure/info" />">Go to protected area</a><br/>
    <a href="<c:url context="/" value="/very-secure/info" />">Go to highly protected area</a><br/>
</p>
<p>
    <b>In default (<code>/pax-web-security</code>) context:</b><br/>
    <a href="<c:url context="/pax-web-security" value="/app" />">Go to anonymous area</a><br/>
    <a href="<c:url context="/pax-web-security" value="/secure/info" />">Go to protected area</a><br/>
    <a href="<c:url context="/pax-web-security" value="/very-secure/info" />">Go to highly protected area</a><br/>
</p>
<c:if test="${auth}">
    <a href="<c:url context="/" value="/logout" />">Logout from <code>/</code></a><br/>
    <a href="<c:url context="/pax-web-security" value="/logout" />">Logout from <code>/pax-web-security</code></a><br/>
</c:if>

</body>
</html>
