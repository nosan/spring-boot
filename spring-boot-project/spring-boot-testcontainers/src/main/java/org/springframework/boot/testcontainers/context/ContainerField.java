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
import java.util.HashSet;
import java.util.Set;

import org.testcontainers.containers.Container;

import org.springframework.aot.generate.ValueCodeGenerator;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Represents a {@link Field} of a class that is a {@link Container Testcontainer}.
 *
 * @author Phillip Webb
 * @author Dmytro Nosan
 */
final class ContainerField {

	private final Field field;

	private final Container<?> container;

	/**
	 * Creates a new {@link ContainerField} instance for the given field.
	 * @param field the field that represents the container
	 */
	ContainerField(Field field) {
		Assert.notNull(field, "'field' must not be null");
		this.field = field;
		this.container = getContainer(field);
	}

	/**
	 * Creates a new {@link ContainerField} instance based on the specified declaring
	 * class and field name.
	 * @param className the name of the class that declares the field
	 * @param fieldName the name of the field to be represented as a container field
	 */
	ContainerField(String className, String fieldName) {
		this(getField(className, fieldName));
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
		return this.container;
	}

	/**
	 * Finds {@link Container} fields for the specified class, traversing up the class
	 * hierarchy.
	 * @param clazz the class to inspect for container fields
	 * @return the {@link ContainerField} instances
	 */
	static Set<ContainerField> getContainerFields(Class<?> clazz) {
		Set<ContainerField> fields = new HashSet<>();
		ReflectionUtils.doWithFields(clazz, (field) -> fields.add(new ContainerField(field)),
				ContainerField::isContainerField);
		return fields;
	}

	private static boolean isContainerField(Field candidate) {
		return Container.class.isAssignableFrom(candidate.getType());
	}

	private static Field getField(String className, String fieldName) {
		Assert.state(className != null, "'className' must not be null");
		Assert.hasText(fieldName, "'fieldName' must not be null or empty");
		Class<?> clazz = ClassUtils.resolveClassName(className, ContainerField.class.getClassLoader());
		return ReflectionUtils.findField(clazz, fieldName);
	}

	private static Container<?> getContainer(Field field) {
		assertValid(field);
		ReflectionUtils.makeAccessible(field);
		Container<?> container = (Container<?>) ReflectionUtils.getField(field, null);
		Assert.state(container != null, () -> "Container field '" + field.getName() + "' must not have a null value");
		return container;
	}

	private static void assertValid(Field field) {
		Assert.state(Modifier.isStatic(field.getModifiers()),
				() -> "Container field '" + field.getName() + "' must be static");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ContainerField that = (ContainerField) obj;
		return this.field.equals(that.field) && this.container.equals(that.container);
	}

	@Override
	public int hashCode() {
		int result = this.field.hashCode();
		result = 31 * result + this.container.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return this.field.toString();
	}

	static class ContainerFieldValueCodeGenerator implements ValueCodeGenerator.Delegate {

		@Override
		public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
			if (value instanceof ContainerField containerField) {
				Field field = containerField.getField();
				return CodeBlock.of("new $T($L)", ContainerField.class, findField(field));
			}
			return null;
		}

		private static CodeBlock findField(Field field) {
			return CodeBlock.of("$T.findField($L, $S)", ReflectionUtils.class, resolveClassName(field),
					field.getName());
		}

		private static CodeBlock resolveClassName(Field field) {
			return CodeBlock.of("$T.resolveClassName($S, $T.class.getClassLoader())", ClassUtils.class,
					field.getDeclaringClass().getName(), ContainerField.class);
		}

	}

}
