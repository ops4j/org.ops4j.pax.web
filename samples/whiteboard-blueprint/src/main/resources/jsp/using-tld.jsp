<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html>
<body align='center'>
    <c:set var="hello" value="Hello World"/>
    <h1>
    <c:out value="${hello}"/>
    </h1>
    <img src='/images/logo.png' border='0'/>

    <h1>from jsp using TLD</h1>
</body>
</html>