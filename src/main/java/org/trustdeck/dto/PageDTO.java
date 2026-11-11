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
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One page of a paged search result.
 * 
 * The total is counted only up to a limit, so that large result sets (e.g. a domain with
 * millions of pseudonyms) do not have to be counted completely.
 * 
 * @param <T> the type of the items on the page
 * @author Armin Müller
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {

	/** The items on this page. */
	private List<T> items;

	/** The zero-based index of this page. */
	private int page;

	/** The requested maximum number of items per page. */
	private int size;

	/** The number of matching items, counted at most up to the count limit. */
	private long total;

	/** Whether more items match than counted, i.e. whether {@link #total} is only a lower bound. */
	private boolean totalIsLowerBound;
}
