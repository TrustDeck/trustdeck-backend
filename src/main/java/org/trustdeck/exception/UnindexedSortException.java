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

package org.trustdeck.exception;

/**
 * Indicates that a search should be sorted by an attribute without a sort index, although it
 * matches more entities than can be sorted without one.
 * 
 * @author Armin Müller
 */
public class UnindexedSortException extends RuntimeException {

	/** Exception UID. */
	private static final long serialVersionUID = 3092571286643315540L;
	
	/**
	 * Constructor.
	 * 
	 * @param path the path of the attribute to sort by
	 * @param countLimit the number of matches up to which a search can be sorted without an index
	 */
	public UnindexedSortException(String path, int countLimit) {
		super("Sorting by the attribute \"" + path + "\" needs a sort index when more than " + countLimit + " entities match.");
	}
}
