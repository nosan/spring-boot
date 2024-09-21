/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testcontainers.properties;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestcontainersPropertySourceApplicationContextInitializer}.
 *
 * @author Dmytro Nosan
 */
class TestcontainersPropertySourceApplicationContextInitializerTests {

	@Test
	void shouldAttachTestPropertySources() {
		AnnotationConfigApplicationContext applicationContext = createApplicationContext();
		PropertySource<?> propertySource = applicationContext.getEnvironment()
			.getPropertySources()
			.get(TestcontainersPropertySource.NAME);
		assertThat(propertySource).isNotNull().isInstanceOf(TestcontainersPropertySource.class);
	}

	@Test
	@ClassPathExclusions("spring-test-*.jar")
	void shouldNotAttachTestPropertySourcesWhenDynamicRegistryDoesNotExist() {
		AnnotationConfigApplicationContext applicationContext = createApplicationContext();
		PropertySource<?> propertySource = applicationContext.getEnvironment()
			.getPropertySources()
			.get(TestcontainersPropertySource.NAME);
		assertThat(propertySource).isNull();
	}

	private AnnotationConfigApplicationContext createApplicationContext() {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		new TestcontainersPropertySourceApplicationContextInitializer().initialize(applicationContext);
		return applicationContext;
	}

}
