/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MapToProperty} with {@link TestPropertyValues}.
 *
 * @author Dmytro Nosan
 */
@ExtendWith(SpringExtension.class)
class MapToPropertyTestPropertyValuesTests {

	@MapToProperty("spring-boot")
	private static final TestPropertyValues prefixValues = TestPropertyValues.of("version=2.0.0");

	@MapToProperty
	private static final TestPropertyValues values = TestPropertyValues.of("spring-framework.version=5.0.0");

	@Autowired
	private Environment environment;

	@Test
	void hasProperty() {
		assertThat(this.environment.getProperty("spring-boot.version")).isEqualTo("2.0.0");
		assertThat(this.environment.getProperty("spring-framework.version")).isEqualTo("5.0.0");
	}

}
