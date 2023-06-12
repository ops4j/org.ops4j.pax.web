/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.service.spi.servlet;

import java.util.Collection;
import jakarta.servlet.descriptor.JspPropertyGroupDescriptor;

public class DefaultJspPropertyGroupDescriptor implements JspPropertyGroupDescriptor {

	private Collection<String> urlPatterns;
	private String elIgnored;
	private String pageEncoding;
	private String scriptingInvalid;
	private String isXml;
	private Collection<String> includePreludes;
	private Collection<String> includeCodas;
	private String deferredSyntaxAllowedAsLiteral;
	private String trimDirectiveWhitespaces;
	private String defaultContentType;
	private String buffer;
	private String errorOnUndeclaredNamespace;
	private String errorOnELNotFound;

	@Override
	public Collection<String> getUrlPatterns() {
		return urlPatterns;
	}

	public void setUrlPatterns(Collection<String> urlPatterns) {
		this.urlPatterns = urlPatterns;
	}

	@Override
	public String getElIgnored() {
		return elIgnored;
	}

	public void setElIgnored(String elIgnored) {
		this.elIgnored = elIgnored;
	}

	@Override
	public String getPageEncoding() {
		return pageEncoding;
	}

	public void setPageEncoding(String pageEncoding) {
		this.pageEncoding = pageEncoding;
	}

	@Override
	public String getScriptingInvalid() {
		return scriptingInvalid;
	}

	public void setScriptingInvalid(String scriptingInvalid) {
		this.scriptingInvalid = scriptingInvalid;
	}

	@Override
	public String getIsXml() {
		return isXml;
	}

	public void setIsXml(String isXml) {
		this.isXml = isXml;
	}

	@Override
	public Collection<String> getIncludePreludes() {
		return includePreludes;
	}

	public void setIncludePreludes(Collection<String> includePreludes) {
		this.includePreludes = includePreludes;
	}

	@Override
	public Collection<String> getIncludeCodas() {
		return includeCodas;
	}

	public void setIncludeCodas(Collection<String> includeCodas) {
		this.includeCodas = includeCodas;
	}

	@Override
	public String getDeferredSyntaxAllowedAsLiteral() {
		return deferredSyntaxAllowedAsLiteral;
	}

	public void setDeferredSyntaxAllowedAsLiteral(String deferredSyntaxAllowedAsLiteral) {
		this.deferredSyntaxAllowedAsLiteral = deferredSyntaxAllowedAsLiteral;
	}

	@Override
	public String getTrimDirectiveWhitespaces() {
		return trimDirectiveWhitespaces;
	}

	public void setTrimDirectiveWhitespaces(String trimDirectiveWhitespaces) {
		this.trimDirectiveWhitespaces = trimDirectiveWhitespaces;
	}

	@Override
	public String getDefaultContentType() {
		return defaultContentType;
	}

	public void setDefaultContentType(String defaultContentType) {
		this.defaultContentType = defaultContentType;
	}

	@Override
	public String getBuffer() {
		return buffer;
	}

	public void setBuffer(String buffer) {
		this.buffer = buffer;
	}

	@Override
	public String getErrorOnUndeclaredNamespace() {
		return errorOnUndeclaredNamespace;
	}

	public void setErrorOnUndeclaredNamespace(String errorOnUndeclaredNamespace) {
		this.errorOnUndeclaredNamespace = errorOnUndeclaredNamespace;
	}

	@Override
	public String getErrorOnELNotFound() {
		return errorOnELNotFound;
	}

	public void setErrorOnELNotFound(String errorOnELNotFound) {
		this.errorOnELNotFound = errorOnELNotFound;
	}

}
