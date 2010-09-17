/**
 * 
 */
package org.ops4j.pax.web.service.spi.model;

import org.ops4j.lang.NullArgumentException;

/**
 * @author Achim
 *
 */
public class LoginConfigModel extends Model {

	private final String realmName;
	private final String authMethod;	
	
	public LoginConfigModel(ContextModel contextModel, String authMethod, String realmName) {
		super(contextModel);
		NullArgumentException.validateNotEmpty( authMethod, "authMethod" );
        NullArgumentException.validateNotEmpty( realmName, "realmName" );
        this.authMethod= authMethod;
        this.realmName= realmName;
	}

	/**
	 * @return the realmName
	 */
	public String getRealmName() {
		return realmName;
	}

	/**
	 * @return the authMethod
	 */
	public String getAuthMethod() {
		return authMethod;
	}

}
