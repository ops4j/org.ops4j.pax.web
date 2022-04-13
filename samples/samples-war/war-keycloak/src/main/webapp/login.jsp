<!--

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

-->
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
	<title>Login Page for Examples</title>
<body>
<form method="POST" action='<%= response.encodeURL("j_security_check") %>'>
	<table>
		<tr>
			<th style="text-align: right"><label for="u">Username:</label></th>
			<td style="text-align: left"><input id="u" type="text" name="j_username"></td>
		</tr>
		<tr>
			<th style="text-align: right"><label for="p">Password:</label></th>
			<td style="text-align: left"><input id="p" type="password" name="j_password"></td>
		</tr>
		<tr>
			<td style="text-align: right"><input type="submit" value="Log In"></td>
			<td style="text-align: left"><input type="reset"></td>
		</tr>
	</table>
</form>
</body>
</html>
