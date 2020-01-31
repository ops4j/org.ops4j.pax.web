<%--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

--%>
<%--@elvariable id="RO" type="java.lang.Boolean"--%>
<%@ tag display-name="Text field" body-content="scriptless" isELIgnored="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="path" required="true" rtexprvalue="true" %>
<%@ attribute name="value" required="true" rtexprvalue="true" %>
<%@ attribute name="label" required="true" rtexprvalue="false" %>
<%@ attribute name="maxlength" required="false" rtexprvalue="false" %>

<td>
   ${label}<br />
   <c:choose>
      <c:when test="${!RO}">
         <input type="text" name="${path}" value="${value}" <c:if test="${maxlength != null}">maxlength="${maxlength}"</c:if> />
      </c:when>
      <c:otherwise>
         <span style="color: blue;">${value}&#160;</span>
      </c:otherwise>
   </c:choose>
</td>
