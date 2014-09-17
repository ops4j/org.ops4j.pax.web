/**
 * 
 */
package org.ops4j.pax.web.deployer.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Achim
 * 
 */
public class DeployerUtilsTest {

	/**
	 * Test method for
	 * {@link org.ops4j.pax.web.deployer.internal.DeployerUtils#extractNameVersionType(java.lang.String)}
	 * .
	 */
	@Test
	public void testExtractNameVersionType() {

		String[] nameVersion;
		// test war types with version
		nameVersion = DeployerUtils.extractNameVersionType("test-1.0.0.war");
		assertEquals("test", nameVersion[0]);
		assertEquals("1.0.0", nameVersion[1]);

		// test standard war types
		nameVersion = DeployerUtils.extractNameVersionType("test.war");

		assertEquals("test", nameVersion[0]);
		assertEquals("0.0.0", nameVersion[1]);

	}

}
