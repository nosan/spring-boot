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

package org.springframework.boot.test.context.bootstrap;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringBootTestContextBootstrapper} with
 * {@link TestPropertySource}.
 *
 * @author Dmytro Nosan
 */
@ExtendWith(SpringExtension.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
@TestPropertySource(locations = { "classpath:test.yaml", "classpath:test1.yaml", "classpath:test.properties" })
@TestPropertySource(locations = "classpath:unknown.properties",
		factory = SpringBootTestContextBootstrapperWithTestPropertySourceTests.CustomPropertySourceFactory.class)
class SpringBootTestContextBootstrapperWithTestPropertySourceTests {

	@Autowired
	private Environment environment;

	@Test
	void loadProperties() {
		assertThat(this.environment.getProperty("spring.bar")).isEqualTo("bar");
		assertThat(this.environment.getProperty("spring.foo")).isEqualTo("baz");
		assertThat(this.environment.getProperty("spring.buzz")).isEqualTo("fazz");
		assertThat(this.environment.getProperty("spring.jazz")).isEqualTo("zzaj");
		assertThat(this.environment.getProperty("spring.bazz")).isEqualTo("quartz");
		assertThat(this.environment.getProperty("custom")).isEqualTo("present");
	}

	static class CustomPropertySourceFactory implements PropertySourceFactory {

		@Override
		public PropertySource<?> createPropertySource(String name, EncodedResource resource) {
			return new MapPropertySource("custom", Map.of("custom", "present"));
		}

	}

}
