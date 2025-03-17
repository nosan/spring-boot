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

import javax.lang.model.element.Modifier;

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
import org.springframework.core.AttributeAccessor;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.WildcardTypeName;
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
		this.setBeanClass(container.getClass());
		setInstanceSupplier(() -> container);
		setRole(ROLE_INFRASTRUCTURE);
		new AotMetadata(field).addTo(this);
	}

	@Override
	public String getContainerImageName() {
		return this.container.getDockerImageName();
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}

	private record AotMetadata(Field field) {

		private static final String ATTRIBUTE_NAME = AotMetadata.class.getName();

		private void addTo(AttributeAccessor attributes) {
			attributes.setAttribute(ATTRIBUTE_NAME, this);
		}

		private static boolean isPresent(AttributeAccessor attributes) {
			return attributes.getAttribute(ATTRIBUTE_NAME) != null;
		}

		private static AotMetadata get(AttributeAccessor attributes) {
			return (AotMetadata) attributes.getAttribute(AotMetadata.ATTRIBUTE_NAME);
		}
	}

	static class TestcontainerBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

		@Override
		public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
			RootBeanDefinition bd = registeredBean.getMergedBeanDefinition();
			if (AotMetadata.isPresent(bd)) {
				return BeanRegistrationAotContribution.withCustomCodeFragments(
						(codeFragments) -> new AotContribution(codeFragments, AotMetadata.get(bd)));
			}
			return null;
		}

		private static final class AotContribution extends BeanRegistrationCodeFragmentsDecorator {

			private static final ParameterizedTypeName CONTAINER_TYPE = ParameterizedTypeName
				.get(ClassName.get(Container.class), WildcardTypeName.subtypeOf(Object.class));

			private static final ParameterizedTypeName CLASS_TYPE = ParameterizedTypeName
				.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class));

			private final Field field;

			private AotContribution(BeanRegistrationCodeFragments delegate, AotMetadata metadata) {
				super(delegate);
				this.field = metadata.field();
			}

			@Override
			public ClassName getTarget(RegisteredBean registeredBean) {
				return ClassName.get(this.field.getDeclaringClass());
			}

			@Override
			public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
					BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
				return beanRegistrationCode.getMethods().add("getInstance", (code) -> {
					code.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
					code.returns(CONTAINER_TYPE);
					if (AccessControl.forMember(this.field).isAccessibleFrom(beanRegistrationCode.getClassName())) {
						code.addStatement("return $T.$L", this.field.getDeclaringClass(), this.field.getName());
					}
					else {
						generationContext.getRuntimeHints().reflection().registerField(this.field);
						code.addStatement("$T clazz = $T.resolveClassName($S, $T.class.getClassLoader())", CLASS_TYPE,
								ClassUtils.class, this.field.getDeclaringClass().getName(),
								beanRegistrationCode.getClassName());
						code.addStatement("$T field = $T.findField(clazz, $S)", Field.class, ReflectionUtils.class,
								this.field.getName());
						code.addStatement("$T.makeAccessible(field)", ReflectionUtils.class);
						code.addStatement("return ($T) $T.getField(field, null)", CONTAINER_TYPE,
								ReflectionUtils.class);
					}
				}).toMethodReference().toCodeBlock();
			}

		}

	}

}
