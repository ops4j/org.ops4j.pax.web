/*
 * Copyright 2014 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.karaf;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;

import java.io.File;

import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.configs.CustomProperties;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;

public class RegressionConfiguration {
    public static final MavenUrlReference PAX_WEB_FEATURES = maven().groupId("org.ops4j.pax.web")
        .artifactId("pax-web-features").type("xml").classifier("features").version("5.0.0-SNAPSHOT");
    
    public static Option regressionDefaults() {
        return regressionDefaults("target/exam");        
    }

    public static Option regressionDefaults(String unpackDir) {
        return composite(
            systemProperty("org.osgi.service.http.port").value(System.getProperty("org.osgi.service.http.port", "8080")),

            karafDistributionConfiguration().frameworkUrl(mvnKarafDist()).karafVersion(karafVersion())
                .unpackDirectory(unpackDirFile(unpackDir)).useDeployFolder(false),                
 
            configureConsole().ignoreLocalConsole(),
            KarafDistributionOption.keepRuntimeFolder(),
            mavenBundle("org.ops4j.pax.web.itest", "itest-shared", "5.0.0-SNAPSHOT"),    

            when(isEquinox()).useOptions(                
                editConfigurationFilePut(CustomProperties.KARAF_FRAMEWORK, "equinox"),
                propagateSystemProperty("pax.exam.framework"),
                systemProperty("osgi.console").value("6666"),
                systemProperty("osgi.console.enable.builtin").value("true"))
            );
    }

    private static File unpackDirFile(String unpackDir) {
        return unpackDir == null ? null : new File(unpackDir);
    }

    public static boolean isEquinox() {
        return "equinox".equals(System.getProperty("pax.exam.framework"));
    }

    public static boolean isFelix() {
        return "felix".equals(System.getProperty("pax.exam.framework"));
    }
    
    public static MavenArtifactUrlReference mvnKarafDist() {
        return maven().groupId("org.apache.karaf")
            .artifactId("apache-karaf").type("tar.gz").version(karafVersion());
    }
    
    public static String karafVersion() {
        ConfigurationManager cm = new ConfigurationManager();
        String karafVersion = cm.getProperty("pax.exam.karaf.version", "3.0.1");
        return karafVersion;
    }
}
