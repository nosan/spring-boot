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

import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.stereotype.Component;

/**
 * Example {@link WebSecurityCustomizer} used with {@code @WebMvcTest} tests, particularly
 * to verify its discovery.
 *
 * @author Dmytro Nosan
 */
@Component
class ExampleWebSecurityCustomizer implements WebSecurityCustomizer {

	@Override
	public void customize(WebSecurity web) {
		web.ignoring().requestMatchers("/three/aaaa");
	}

}
