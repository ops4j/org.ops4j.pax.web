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
package org.ops4j.pax.web.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.ops4j.io.ZipExploder;

public class JarExploder extends ZipExploder {

    public void processJarFile(JarFile jarFile, String destDir) throws IOException {

        try {
            Map<String, ZipEntry> fEntries = getEntries(jarFile);
            String[] names = fEntries.keySet().toArray(new String[] {});
            if (sortNames) {
                Arrays.sort(names);
            }
            // copy all files
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                ZipEntry e = fEntries.get(name);
                copyFileEntry(destDir, jarFile, e);
            }
        }
        finally {
            try {
                jarFile.close();
            }
            catch (IOException ioe) {
            }
        }
    }

}
