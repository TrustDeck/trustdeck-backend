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

package org.trustdeck.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Describes the outcome of replacing a user's permissions on a resource.
 * Only the actions that are added are checked against the requester's own actions;
 * when any of them is rejected, nothing is changed.
 * 
 * @author Armin Müller
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionReplacementDTO {

	/** The actions that were (or would have been) added. */
	private List<String> added;

	/** The actions that were (or would have been) removed. */
	private List<String> removed;

	/** The actions that the requester is not allowed to grant; if not empty, nothing was changed. */
	private List<String> rejected;
}
