/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.web.servlet.mockmvc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} to validate {@link WebSecurityCustomizer} are
 * discovered.
 *
 * @author Dmytro Nosan
 */
@WebMvcTest(controllers = ExampleController3.class)
class WebMvcTestWebSecurityCustomizerIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void shouldIncludesWebSecurityCustomizers() {
		assertThat(this.mvc.get().uri("/three")).hasStatus4xxClientError();
		assertThat(this.mvc.get().uri("/three/abcd")).hasStatus4xxClientError();
		assertThat(this.mvc.get().uri("/three/aaaa")).hasStatusOk().bodyText().isEqualTo("Hello aaaa");
	}

	/**
	 * Test security configuration to ensure that all endpoints are secured.
	 */
	@TestConfiguration(proxyBeanMethods = false)
	static class SecurityConfig {

		@Bean
		SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
			return http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated()).build();
		}

	}

}
