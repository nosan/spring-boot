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

import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.testcontainers.lifecycle.Startable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.ReflectionUtils;

/**
 * Used by {@link ImportTestcontainersRegistrar} to import
 * {@link DynamicPropertySource @DynamicPropertySource} through a
 * {@link DynamicPropertyRegistrar}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Dmytro Nosan
 */
class DynamicPropertySourceMethodsImporter {

	void registerDynamicPropertySources(BeanDefinitionRegistry beanDefinitionRegistry, Class<?> definitionClass,
			Set<ContainerField> importContainers) {
		Set<DynamicPropertySourceMethod> methods = DynamicPropertySourceMethod.getMethods(definitionClass);
		if (methods.isEmpty()) {
			return;
		}
		RootBeanDefinition registrarDefinition = new RootBeanDefinition();
		registrarDefinition.setBeanClass(DynamicPropertySourcePropertyRegistrar.class);
		registrarDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registrarDefinition.getConstructorArgumentValues().addGenericArgumentValue(methods);
		registrarDefinition.getConstructorArgumentValues().addGenericArgumentValue(importContainers);
		beanDefinitionRegistry.registerBeanDefinition(definitionClass.getName() + ".dynamicPropertyRegistrar",
				registrarDefinition);
	}

	static class DynamicPropertySourcePropertyRegistrar implements DynamicPropertyRegistrar {

		private final Set<Method> methods;

		private final Set<Startable> startables;

		DynamicPropertySourcePropertyRegistrar(Set<DynamicPropertySourceMethod> methods,
				Set<ContainerField> containerFields) {
			this.methods = methods.stream().map(DynamicPropertySourceMethod::method).collect(Collectors.toSet());
			this.startables = containerFields.stream()
				.map(ContainerField::getContainer)
				.filter(Startable.class::isInstance)
				.map(Startable.class::cast)
				.collect(Collectors.toSet());
		}

		@Override
		public void accept(DynamicPropertyRegistry registry) {
			DynamicPropertyRegistry containersBackedRegistry = new ContainersBackedDynamicPropertyRegistry(registry,
					this.startables);
			this.methods.forEach((method) -> {
				ReflectionUtils.makeAccessible(method);
				ReflectionUtils.invokeMethod(method, null, containersBackedRegistry);
			});
		}

	}

	static class ContainersBackedDynamicPropertyRegistry implements DynamicPropertyRegistry {

		private final DynamicPropertyRegistry delegate;

		private final Set<Startable> startables;

		ContainersBackedDynamicPropertyRegistry(DynamicPropertyRegistry delegate, Set<Startable> startables) {
			this.delegate = delegate;
			this.startables = startables;
		}

		@Override
		public void add(String name, Supplier<Object> valueSupplier) {
			this.delegate.add(name, () -> {
				startContainers();
				return valueSupplier.get();
			});
		}

		private void startContainers() {
			this.startables.forEach(Startable::start);
		}

	}

}
