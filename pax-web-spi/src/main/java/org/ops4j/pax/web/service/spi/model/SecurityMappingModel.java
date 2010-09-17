package org.ops4j.pax.web.service.spi.model;

import org.ops4j.lang.NullArgumentException;

public class SecurityMappingModel extends Model {

	private String url;
	private String mapping;
	private String constraintName;

	public SecurityMappingModel(ContextModel contextModel, String constraintName,
			String mapping, String url) {
		super(contextModel);
		NullArgumentException.validateNotEmpty( constraintName, "constraintName" );
		NullArgumentException.validateNotEmpty( mapping, "mapping" );
		NullArgumentException.validateNotEmpty( url, "url" );
		this.constraintName = constraintName;
		this.mapping = mapping;
		this.url = url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param mapping the mapping to set
	 */
	public void setMapping(String mapping) {
		this.mapping = mapping;
	}

	/**
	 * @return the mapping
	 */
	public String getMapping() {
		return mapping;
	}

	/**
	 * @param constraintName the constraintName to set
	 */
	public void setConstraintName(String constraintName) {
		this.constraintName = constraintName;
	}

	/**
	 * @return the constraintName
	 */
	public String getConstraintName() {
		return constraintName;
	}

}
