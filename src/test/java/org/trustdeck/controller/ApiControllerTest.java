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

package org.trustdeck.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Tests the version endpoint.
 */
class ApiControllerTest {

	/** The version from the build metadata is reported. */
	@Test
	void reportsBuildVersion() {
		Properties properties = new Properties();
		properties.setProperty("version", "v2.10.0");

		assertEquals(Map.of("version", "v2.10.0"), controller(new BuildProperties(properties)).version().getBody());
	}

	/** Without build metadata (e.g. started from an IDE), the version is "unknown". */
	@Test
	void reportsUnknownVersionWithoutBuildMetadata() {
		assertEquals(Map.of("version", "unknown"), controller(null).version().getBody());
	}

	/** The version needs a login, but no permission; the health endpoint doesn't report it. */
	@Test
	void requiresOnlyAuthentication() throws NoSuchMethodException {
		PreAuthorize rule = ApiController.class.getMethod("version").getAnnotation(PreAuthorize.class);
		assertEquals("isAuthenticated()", rule.value());

		assertFalse(((Map<?, ?>) controller(null).ping().getBody()).containsKey("version"));
	}

	@SuppressWarnings("unchecked")
	private static ApiController controller(BuildProperties build) {
		ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(build);
		return new ApiController(provider);
	}
}
