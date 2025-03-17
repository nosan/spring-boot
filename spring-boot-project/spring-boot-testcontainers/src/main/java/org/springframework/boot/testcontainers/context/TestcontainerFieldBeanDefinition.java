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

import org.testcontainers.containers.Container;

import org.springframework.aot.generate.AccessControl;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RootBeanDefinition} used for testcontainer bean definitions.
 *
 * @author Phillip Webb
 * @author Dmytro Nosan
 */
class TestcontainerFieldBeanDefinition extends RootBeanDefinition implements TestcontainerBeanDefinition {

	private final Container<?> container;

	private final MergedAnnotations annotations;

	TestcontainerFieldBeanDefinition(Field field, Container<?> container) {
		this.container = container;
		this.annotations = MergedAnnotations.from(field);
		setBeanClass(container.getClass());
		setInstanceSupplier(() -> container);
		setRole(ROLE_INFRASTRUCTURE);
		setAttribute(TestcontainerFieldBeanDefinition.class.getName(), field);
	}

	@Override
	public String getContainerImageName() {
		return this.container.getDockerImageName();
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}

	static class TestcontainerBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

		@Override
		public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
			RootBeanDefinition bd = registeredBean.getMergedBeanDefinition();
			if (bd.getAttribute(TestcontainerFieldBeanDefinition.class.getName()) instanceof Field field) {
				return BeanRegistrationAotContribution
					.withCustomCodeFragments((codeFragments) -> new AotContribution(codeFragments, field));
			}
			return null;
		}

		private static final class AotContribution extends BeanRegistrationCodeFragmentsDecorator {

			private final Field field;

			private AotContribution(BeanRegistrationCodeFragments delegate, Field field) {
				super(delegate);
				this.field = field;
			}

			@Override
			public ClassName getTarget(RegisteredBean registeredBean) {
				return ClassName.get(TestcontainerFieldBeanDefinition.class);
			}

			@Override
			public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
					BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
				ClassName targetClassName = beanRegistrationCode.getClassName();
				generationContext.getRuntimeHints().reflection().registerField(this.field);
				return CodeBlock.of("new $T($L)::getContainer", ContainerField.class,
						findField(this.field, targetClassName));
			}

			private static CodeBlock findField(Field field, ClassName targetClassName) {
				return CodeBlock.of("$T.findField($L, $S)", ReflectionUtils.class,
						resolveClassName(field.getDeclaringClass(), targetClassName), field.getName());
			}

			private static CodeBlock resolveClassName(Class<?> declaringClass, ClassName targetClassName) {
				if (AccessControl.forClass(declaringClass).isAccessibleFrom(targetClassName)) {
					return CodeBlock.of("$T.class", declaringClass);
				}
				return CodeBlock.of("$T.resolveClassName($S, $T.class.getClassLoader())", ClassUtils.class,
						declaringClass.getName(), targetClassName);
			}

		}

	}

}
