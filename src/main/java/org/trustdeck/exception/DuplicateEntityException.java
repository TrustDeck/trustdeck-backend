/*
 * Trust Deck Services
 * Copyright 2025 Armin Müller and Eric Wündisch
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
 * This class represents an exception when a duplicate of an entity was found.
 * 
 * @author Armin Müller
 *
 */
public class DuplicateEntityException extends RuntimeException {

	/** Exception UID. */
	private static final long serialVersionUID = 1468720116723555939L;
	
	/** 
	 * Constructor.
	 * 
	 * @param msg the message that should accompany this exception
	 */
	public DuplicateEntityException(String msg) {
		super("A duplicate of the entity type was found: " + msg);
	}
}
