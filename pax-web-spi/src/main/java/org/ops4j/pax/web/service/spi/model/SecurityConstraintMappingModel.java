package org.ops4j.pax.web.service.spi.model;

import java.util.List;

import org.ops4j.lang.NullArgumentException;

public class SecurityConstraintMappingModel extends Model {

	private String url;
	private String mapping;
	private String constraintName;
	private List<String> roles;
	private boolean authentication;
	private String dataConstraint;

	public SecurityConstraintMappingModel(ContextModel contextModel, String constraintName,
			String mapping, String url, String dataConstraint, boolean authentication, List<String> roles) {
		super(contextModel);
		NullArgumentException.validateNotEmpty( constraintName, "constraintName" );
		NullArgumentException.validateNotEmpty( url, "url" );
		NullArgumentException.validateNotEmpty( dataConstraint, "dataConstraint" );
		NullArgumentException.validateNotEmpty( roles.toArray(), "roles" );
		NullArgumentException.validateNotEmptyContent( (String[]) roles.toArray(new String[roles.size()]), "roles content" );
		this.constraintName = constraintName;
		this.mapping = mapping;
		this.url = url;
		this.dataConstraint = dataConstraint;
		this.authentication = authentication;
		this.roles = roles;
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

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public boolean isAuthentication() {
		return authentication;
	}

	public void setAuthentication(boolean authentication) {
		this.authentication = authentication;
	}

	public String getDataConstraint() {
		return dataConstraint;
	}

	public void setDataConstraint(String dataConstraint) {
		this.dataConstraint = dataConstraint;
	}

}
