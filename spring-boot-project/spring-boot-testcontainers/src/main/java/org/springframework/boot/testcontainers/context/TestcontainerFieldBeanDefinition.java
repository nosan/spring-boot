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

package org.springframework.boot.testcontainers.context;

import java.lang.reflect.Field;

import org.testcontainers.containers.Container;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RootBeanDefinition} used for testcontainer bean definitions.
 *
 * @author Phillip Webb
 */
class TestcontainerFieldBeanDefinition extends RootBeanDefinition implements TestcontainerBeanDefinition {

	private final Container<?> container;

	private final MergedAnnotations annotations;

	TestcontainerFieldBeanDefinition(Field field, Container<?> container) {
		this.container = container;
		this.annotations = MergedAnnotations.from(field);
		setBeanClass(TestcontainerFactoryBean.class);
		setRole(ROLE_INFRASTRUCTURE);
		setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, container.getClass());
		getConstructorArgumentValues().addIndexedArgumentValue(0, container.getClass().getName());
		getConstructorArgumentValues().addIndexedArgumentValue(1, field.getDeclaringClass().getName());
		getConstructorArgumentValues().addIndexedArgumentValue(2, field.getName());
	}

	@Override
	public String getContainerImageName() {
		return this.container.getDockerImageName();
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}

	static final class TestcontainerFactoryBean implements FactoryBean<Container<?>> {

		private final Class<? extends Container<?>> containerClass;

		private final Class<?> testClass;

		private final String fieldName;

		TestcontainerFactoryBean(Class<? extends Container<?>> containerClass, Class<?> testClass, String fieldName) {
			Assert.notNull(containerClass, "Container class must not be null");
			Assert.notNull(testClass, "Test class must not be null");
			Assert.notNull(fieldName, "Field name must not be null");
			this.testClass = testClass;
			this.containerClass = containerClass;
			this.fieldName = fieldName;
		}

		@Override
		public Container<?> getObject() {
			Field field = ReflectionUtils.findField(this.testClass, this.fieldName);
			Assert.notNull(field, "Field '" + this.fieldName + "' is not found in class '" + this.testClass + "'");
			ReflectionUtils.makeAccessible(field);
			Object container = ReflectionUtils.getField(field, null);
			Assert.notNull(container, "Container field '" + field.getName() + "' in class '" + field.getDeclaringClass()
					+ "' must not have a null value");
			return this.containerClass.cast(container);
		}

		@Override
		public Class<?> getObjectType() {
			return this.containerClass;
		}

	}

}
