/*
 * Copyright 2022 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.samples.whiteboard.jaxrs;

import java.nio.charset.StandardCharsets;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/")
public class RestService {

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.MEDIA_TYPE_WILDCARD)
	@ApiOperation(value = "Returns universal greeting message")
	@ApiResponses({
			@ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 405, message = "Error: Not Allowed")
	})
	public Response hello(@Context HttpHeaders headers, @Context UriInfo uriInfo) {
		return Response.status(Response.Status.OK)
				.entity("Hello JAX-RS\n".getBytes(StandardCharsets.UTF_8))
				.type(MediaType.TEXT_PLAIN).build();
	}

}
