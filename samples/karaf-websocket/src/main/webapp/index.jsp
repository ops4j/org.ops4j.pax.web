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
<!DOCTYPE HTML>
<html>
<head>
<script type="text/javascript">
function WebSocketTest()
{
  if ("WebSocket" in window)
  {
	  var protocol = window.location.protocol;
	  protocol="ws";
	  var full = protocol+"://"+window.location.hostname+(window.location.port ? ':'+window.location.port: '');
	  
     // Let us open a web socket
     var ws = new WebSocket(full+"/cloud-websocket/websocket");
     ws.onopen = function()
     {
        // Web Socket is connected, send data using send()
        ws.send("Message to send");

     };
     ws.onmessage = function (evt) 
     { 
        var received_msg = evt.data;
        alert("Message is received...");
     };
     ws.onclose = function()
     { 
        // websocket is closed.
        alert("Connection is closed..."); 
     };
  }
  else
  {
     // The browser doesn't support WebSocket
     alert("WebSocket NOT supported by your Browser!");
  }
}
</script>
</head>
<body>
<div id="sse">
   <a href="javascript:WebSocketTest()">Run WebSocket</a>
</div>
</body>
</html>