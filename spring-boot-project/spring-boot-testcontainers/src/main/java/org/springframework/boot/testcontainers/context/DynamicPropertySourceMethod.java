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
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.aot.generate.ValueCodeGenerator;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.javapoet.CodeBlock;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * TODO.
 *
 * @author Dmytro Nosan
 */
record DynamicPropertySourceMethod(Method method) {

	DynamicPropertySourceMethod {
		assertValid(method);
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
	public String toString() {
		return this.method.toString();
	}

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
		Assert.state(Modifier.isStatic(method.getModifiers()),
				() -> "@DynamicPropertySource method '" + method.getName() + "' must be static");
		Class<?>[] types = method.getParameterTypes();
		Assert.state(types.length == 1 && types[0] == DynamicPropertyRegistry.class,
				() -> "@DynamicPropertySource method '" + method.getName()
						+ "' must accept a single DynamicPropertyRegistry argument");
	}

	static class DynamicPropertySourceValueCodeGenerator implements ValueCodeGenerator.Delegate {

		@Override
		public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
			if (value instanceof DynamicPropertySourceMethod method) {
				return CodeBlock.of("new $T($L)", DynamicPropertySourceMethod.class, findMethod(method.method()));
			}
			return null;
		}

		private static CodeBlock findMethod(Method method) {
			return CodeBlock.of("$T.findMethod($L, $S, $L)", ReflectionUtils.class, getDeclaringClass(method),
					method.getName(), getParameterTypes(method));
		}

		private static CodeBlock getDeclaringClass(Method method) {
			return CodeBlock.of("$T.resolveClassName($S, $T.class.getClassLoader())", ClassUtils.class,
					method.getDeclaringClass().getName(), DynamicPropertySourceMethod.class);
		}

		private static CodeBlock getParameterTypes(Method method) {
			return Arrays.stream(method.getParameterTypes())
				.map((type) -> CodeBlock.of("$T.class", type))
				.collect(CodeBlock.joining(", "));
		}

	}

}
