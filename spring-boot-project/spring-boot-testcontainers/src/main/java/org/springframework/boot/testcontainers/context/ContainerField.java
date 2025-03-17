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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.testcontainers.containers.Container;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * Represents a {@link Field} of a class that is a {@link Container Testcontainer}.
 *
 * @author Phillip Webb
 * @author Dmytro Nosan
 */
final class ContainerField {

	private final Field field;

	private final Supplier<Container<?>> container;

	/**
	 * Creates a new {@link ContainerField} instance for the given field.
	 * @param field the field that represents the container
	 */
	ContainerField(Field field) {
		assertValid(field);
		this.field = field;
		this.container = SingletonSupplier.of(() -> getContainer(field));
	}

	/**
	 * Returns the {@link Field} instance.
	 * @return the field instance
	 */
	Field getField() {
		return this.field;
	}

	/**
	 * Returns the {@link Container} instance.
	 * @return the container instance
	 */
	Container<?> getContainer() {
		return this.container.get();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ContainerField that = (ContainerField) obj;
		return this.field.equals(that.field);
	}

	@Override
	public int hashCode() {
		return this.field.hashCode();
	}

	@Override
	public String toString() {
		return this.field.toString();
	}

	/**
	 * Finds {@link Container} fields for the specified class, traversing up the class
	 * hierarchy.
	 * @param clazz the class to inspect for container fields
	 * @return the {@link ContainerField} instances
	 */
	static Set<ContainerField> getContainerFields(Class<?> clazz) {
		Set<ContainerField> fields = new LinkedHashSet<>();
		ReflectionUtils.doWithFields(clazz, (field) -> fields.add(new ContainerField(field)),
				ContainerField::isContainerField);
		return fields;
	}

	private static boolean isContainerField(Field candidate) {
		return Container.class.isAssignableFrom(candidate.getType());
	}

	private static Container<?> getContainer(Field field) {
		ReflectionUtils.makeAccessible(field);
		Container<?> container = (Container<?>) ReflectionUtils.getField(field, null);
		Assert.state(container != null, () -> "Container field '" + field.getName() + "' must not have a null value");
		return container;
	}

	private static void assertValid(Field field) {
		Assert.notNull(field, "'field' must not be null");
		Assert.state(Modifier.isStatic(field.getModifiers()),
				() -> "Container field '" + field.getName() + "' must be static");
	}

}
