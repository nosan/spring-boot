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

package org.springframework.boot.test.context;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertySourceLoaderPropertySourceFactory}.
 *
 * @author Dmytro Nosan
 */
class PropertySourceLoaderPropertySourceFactoryTests {

	private final PropertySourceLoaderPropertySourceFactory factory = new PropertySourceLoaderPropertySourceFactory();

	@Test
	void shouldCreatePropertySourceWithYamlPropertySourceLoader() throws IOException {
		ClassPathResource resource = new ClassPathResource("test.yaml");
		PropertySource<?> propertySource = this.factory.createPropertySource("test", new EncodedResource(resource));
		assertThat(propertySource.getName()).isEqualTo("test");
		assertThat(propertySource.getProperty("spring.bar")).isEqualTo("bar");
		assertThat(propertySource.getProperty("spring.foo")).isEqualTo("baz");
	}

	@Test
	void shouldCreatePropertySourceWithPropertiesPropertySourceLoader() throws IOException {
		ClassPathResource resource = new ClassPathResource("test.properties");
		PropertySource<?> propertySource = this.factory.createPropertySource(null, new EncodedResource(resource));
		assertThat(propertySource.getName()).isEqualTo(resource.getDescription());
		assertThat(propertySource.getProperty("spring.jazz")).isEqualTo("zzaj");
		assertThat(propertySource.getProperty("spring.bazz")).isEqualTo("quartz");
	}

	@Test
	void shouldLoadPropertiesWithFallbackPropertySourceFactory() throws IOException {
		String properties = "key=value";
		Charset charset = StandardCharsets.UTF_8;
		ByteArrayResource resource = new ByteArrayResource(properties.getBytes(charset));
		PropertySource<?> propertySource = this.factory.createPropertySource(null,
				new EncodedResource(resource, charset));
		assertThat(propertySource.getName()).isEqualTo(resource.getDescription());
		assertThat(propertySource.getProperty("key")).isEqualTo("value");
	}

}
