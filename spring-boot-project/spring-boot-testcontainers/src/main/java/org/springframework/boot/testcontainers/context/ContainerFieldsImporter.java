/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.testcontainers.context;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.testcontainers.containers.Container;
import org.testcontainers.lifecycle.Startable;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Used by {@link ImportTestcontainersRegistrar} to import {@link Container} fields.
 *
 * @author Phillip Webb
 */
class ContainerFieldsImporter {

	Map<Field, Startable> registerBeanDefinitions(BeanDefinitionRegistry registry, Class<?> definitionClass) {
		Map<Field, Startable> importedContainers = new LinkedHashMap<>();
		for (Field field : getContainerFields(definitionClass)) {
			assertValid(field);
			Container<?> container = getContainer(field);
			if (container instanceof Startable startable) {
				importedContainers.put(field, startable);
			}
			registerBeanDefinition(registry, field, container);
		}
		return importedContainers;
	}

	private List<Field> getContainerFields(Class<?> containersClass) {
		List<Field> containerFields = new ArrayList<>();
		ReflectionUtils.doWithFields(containersClass, containerFields::add, this::isContainerField);
		return List.copyOf(containerFields);
	}

	private boolean isContainerField(Field candidate) {
		return Container.class.isAssignableFrom(candidate.getType());
	}

	private void assertValid(Field field) {
		Assert.state(Modifier.isStatic(field.getModifiers()),
				() -> "Container field '" + field.getName() + "' must be static");
	}

	private Container<?> getContainer(Field field) {
		ReflectionUtils.makeAccessible(field);
		Container<?> container = (Container<?>) ReflectionUtils.getField(field, null);
		Assert.state(container != null, () -> "Container field '" + field.getName() + "' must not have a null value");
		return container;
	}

	private void registerBeanDefinition(BeanDefinitionRegistry registry, Field field, Container<?> container) {
		ContainerImageMetadata containerMetadata = new ContainerImageMetadata(container.getDockerImageName());
		TestcontainerFieldBeanDefinition beanDefinition = new TestcontainerFieldBeanDefinition(field, container);
		containerMetadata.addTo(beanDefinition);
		String beanName = generateBeanName(field);
		registry.registerBeanDefinition(beanName, beanDefinition);
	}

	private String generateBeanName(Field field) {
		return "importTestContainer.%s.%s".formatted(field.getDeclaringClass().getName(), field.getName());
	}

}
