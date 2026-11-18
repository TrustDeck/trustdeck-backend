/*
 * Trust Deck Services
 * Copyright 2026 Armin Müller
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.trustdeck.model;

import java.util.List;

/**
 * A scalar attribute of an entity type by which entity searches can be sorted.
 * 
 * @param path the attribute's path as the clients name it: the names of the enclosing named groups
 *        and the attribute's name, separated by dots (e.g. {@code address.city})
 * @param type the attribute's type from the type definition (e.g. {@code string} or {@code number})
 * @param jsonPath the path to the attribute's value in the stored entity data; it contains the
 *        index {@code 0} after every repeatable group or attribute, so that entities are sorted 
 *        by the first element of a repeated value
 * @param sortIndex whether the type definition asks for a database index to sort by this attribute
 * @author Armin Müller
 */
public record SortableAttribute(String path, String type, List<String> jsonPath, boolean sortIndex) {

	/**
	 * Checks whether the attribute's values are numbers, which are sorted numerically.
	 * 
	 * @return {@code true} for number and integer attributes, {@code false} otherwise
	 */
	public boolean isNumeric() {
		return "number".equalsIgnoreCase(type) || "integer".equalsIgnoreCase(type);
	}
}
