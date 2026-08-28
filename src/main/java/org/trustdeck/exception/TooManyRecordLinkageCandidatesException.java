/*
 * Trust Deck Services
 * Copyright 2026 Armin Müller and Eric Wündisch
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

import lombok.Getter;
import lombok.Setter;

/**
 * Indicates that record linkage blocking produced more candidates than the configured candidate limit allows.
 * 
 * @author Armin Müller
 *
 */
public class TooManyRecordLinkageCandidatesException extends RuntimeException {

	/** Exception UID. */
	private static final long serialVersionUID = 7514032571009906950L;
	
	/** The limit for candidates generated through blocking. */
	@Getter
	@Setter
	private int candidateLimit;
	
	/** 
	 * Constructor.
	 * 
	 * @param limit the limit that was exceeded
	 */
	public TooManyRecordLinkageCandidatesException(int limit) {
		super("Record-linkage candidate generation exceeded the configured limit of " + limit + " candidates.");
		
		this.candidateLimit = limit;
	}
}
