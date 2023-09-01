/*
 * Copyright 2023 OPS4J.
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
package org.ops4j.pax.web.itest.server.war;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

@Ignore("Manual test")
public class MultipartTest {

	@Test
	public void sendAttachment() throws Exception {
		final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create().build();
		try (CloseableHttpClient httpclient = HttpClients.custom().setConnectionManager(cm).build()) {

			final HttpPost httppost = new HttpPost("http://127.0.0.1:33429/wab/as1/post");

			final File file = new File("src/test/java/org/ops4j/pax/web/itest/server/war/MultipartTest.java");
			final FileBody fileBody = new FileBody(file, ContentType.DEFAULT_BINARY);
			final StringBody stringBody1 = new StringBody("This is message 1", ContentType.MULTIPART_FORM_DATA);
			final StringBody stringBody2 = new StringBody("This is message 2", ContentType.MULTIPART_FORM_DATA);

			final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.STRICT);
			builder.addPart("file", fileBody);
			builder.addPart("text1", stringBody1);
			builder.addPart("text2", stringBody2);
			final HttpEntity entity = builder.build();

			httppost.setEntity(entity);

			final HttpClientContext clientContext = HttpClientContext.create();
			try (CloseableHttpResponse response = httpclient.execute(httppost, clientContext)) {
				StringBuilder sb = new StringBuilder();
				sb.append(response.getVersion()).append(" ")
						.append(response.getCode()).append(" ")
						.append(response.getReasonPhrase())
						.append("\r\n");
				for (Header header : response.getHeaders()) {
					sb.append(header.getName()).append(header.getValue()).append("\r\n");
				}
				sb.append("\r\n");
				sb.append(EntityUtils.toString(response.getEntity()));

				System.out.println(sb.toString());
			}
		}
	}

}
