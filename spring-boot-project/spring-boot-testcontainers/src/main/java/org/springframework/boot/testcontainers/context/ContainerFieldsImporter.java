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
import java.util.Set;

import org.testcontainers.containers.Container;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;

/**
 * Used by {@link ImportTestcontainersRegistrar} to import {@link Container} fields.
 *
 * @author Phillip Webb
 */
class ContainerFieldsImporter {

	Set<ContainerField> registerBeanDefinitions(BeanDefinitionRegistry registry, Class<?> definitionClass) {
		Set<ContainerField> containerFields = ContainerField.getContainerFields(definitionClass);
		for (ContainerField containerField : containerFields) {
			Field field = containerField.getField();
			Container<?> container = containerField.getContainer();
			registerBeanDefinition(registry, field, container);
		}
		return containerFields;
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
