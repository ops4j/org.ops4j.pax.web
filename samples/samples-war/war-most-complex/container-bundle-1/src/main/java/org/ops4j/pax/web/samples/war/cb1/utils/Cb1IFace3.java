/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.samples.war.cb1.utils;

import org.ops4j.pax.web.samples.war.cb3.utils.IFace3;

/**
 * An interface that is visible from {@code the-wab-itself}, but which extends an interface, which should not be
 * visible.
 */
public interface Cb1IFace3 extends IFace3 {

}
