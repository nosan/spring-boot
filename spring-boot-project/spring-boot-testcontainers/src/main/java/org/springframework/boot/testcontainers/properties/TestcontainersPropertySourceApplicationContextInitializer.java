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

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

/**
 * {@link ApplicationContextInitializer} to attach {@link TestcontainersPropertySource}.
 *
 * @author Dmytro Nosan
 * @since 3.2.11
 */
public class TestcontainersPropertySourceApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static final String DYNAMIC_PROPERTY_REGISTRY_SOURCE_CLASS = "org.springframework.test.context.DynamicPropertyRegistry";

	private static final Set<ConfigurableApplicationContext> applied = Collections
		.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		if (!applied.add(applicationContext)) {
			return;
		}
		if (ClassUtils.isPresent(DYNAMIC_PROPERTY_REGISTRY_SOURCE_CLASS, getClass().getClassLoader())) {
			ConfigurableEnvironment environment = applicationContext.getEnvironment();
			TestcontainersPropertySource.attach(environment);
		}
	}

}
