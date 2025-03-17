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
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.Assert;

/**
 * Represents a {@code @DynamicPropertySource} annotated method.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Dmytro Nosan
 */
final class DynamicPropertySourceMethod {

	private final Method method;

	/**
	 * Creates a new {@link DynamicPropertySourceMethod} instance for the given method.
	 * @param method the method that represents {@code @DynamicPropertySource} annotated
	 * method.
	 */
	DynamicPropertySourceMethod(Method method) {
		assertValid(method);
		this.method = method;
	}

	/**
	 * Returns the {@link Method} instance.
	 * @return the method instance
	 */
	Method getMethod() {
		return this.method;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DynamicPropertySourceMethod that = (DynamicPropertySourceMethod) obj;
		return this.method.equals(that.method);
	}

	@Override
	public int hashCode() {
		return this.method.hashCode();
	}

	@Override
	public String toString() {
		return this.method.toString();
	}

	/**
	 * Retrieves all methods annotated with {@code @DynamicPropertySource} in the given
	 * class.
	 * @param definitionClass the class to scan
	 * @return a set of {@link DynamicPropertySourceMethod} instances.
	 */
	static Set<DynamicPropertySourceMethod> getMethods(Class<?> definitionClass) {
		return MethodIntrospector.selectMethods(definitionClass, DynamicPropertySourceMethod::isAnnotated)
			.stream()
			.map(DynamicPropertySourceMethod::new)
			.collect(Collectors.toSet());
	}

	private static boolean isAnnotated(Method method) {
		return MergedAnnotations.from(method).isPresent(DynamicPropertySource.class);
	}

	private static void assertValid(Method method) {
		Assert.notNull(method, "'method' must not be null");
		Assert.state(Modifier.isStatic(method.getModifiers()),
				() -> "@DynamicPropertySource method '" + method.getName() + "' must be static");
		Class<?>[] types = method.getParameterTypes();
		Assert.state(types.length == 1 && types[0] == DynamicPropertyRegistry.class,
				() -> "@DynamicPropertySource method '" + method.getName()
						+ "' must accept a single DynamicPropertyRegistry argument");
	}

}
