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

import org.testcontainers.containers.Container;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
import org.springframework.core.annotation.MergedAnnotations;

/**
 * {@link RootBeanDefinition} used for testcontainer bean definitions.
 *
 * @author Phillip Webb
 * @author Dmytro Nosan
 */
class TestcontainerFieldBeanDefinition extends RootBeanDefinition implements TestcontainerBeanDefinition {

	private final Container<?> container;

	private final MergedAnnotations annotations;

	TestcontainerFieldBeanDefinition(ContainerField containerField) {
		this.container = containerField.getContainer();
		this.annotations = MergedAnnotations.from(containerField.getField());
		setBeanClass(ContainerFactory.class);
		setTargetType(this.container.getClass());
		setRole(ROLE_INFRASTRUCTURE);
		getConstructorArgumentValues().addGenericArgumentValue(containerField);
		setFactoryMethodName("create");
	}

	@Override
	public String getContainerImageName() {
		return this.container.getDockerImageName();
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}

	static final class ContainerFactory {

		private ContainerFactory() {
		}

		static Container<?> create(ContainerField containerField) {
			return containerField.getContainer();
		}

	}

}
