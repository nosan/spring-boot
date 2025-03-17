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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.testcontainers.lifecycle.Startable;

import org.springframework.aot.generate.AccessControl;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.ClassUtils;
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
			Set<ContainerField> importedContainers) {
		Set<DynamicPropertySourceMethod> methods = DynamicPropertySourceMethod.getMethods(definitionClass);
		if (methods.isEmpty()) {
			return;
		}
		RootBeanDefinition registrarDefinition = new RootBeanDefinition();
		registrarDefinition.setBeanClass(DynamicPropertySourcePropertyRegistrar.class);
		registrarDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registrarDefinition
			.setInstanceSupplier(() -> new DynamicPropertySourcePropertyRegistrar(methods, importedContainers));
		registrarDefinition.setAttribute(DynamicPropertySourcePropertyRegistrar.class.getName(), true);
		beanDefinitionRegistry.registerBeanDefinition(definitionClass.getName() + ".dynamicPropertyRegistrar",
				registrarDefinition);
	}

	static class DynamicPropertySourcePropertyRegistrar implements DynamicPropertyRegistrar {

		private final Set<Method> methods;

		private final Set<ContainerField> containerFields;

		DynamicPropertySourcePropertyRegistrar(Set<DynamicPropertySourceMethod> methods,
				Set<ContainerField> containerFields) {
			this.methods = methods.stream().map(DynamicPropertySourceMethod::getMethod).collect(Collectors.toSet());
			this.containerFields = containerFields;
		}

		@Override
		public void accept(DynamicPropertyRegistry registry) {
			DynamicPropertyRegistry containersBackedRegistry = new ContainersBackedDynamicPropertyRegistry(registry,
					this.containerFields);
			this.methods.forEach((method) -> {
				ReflectionUtils.makeAccessible(method);
				ReflectionUtils.invokeMethod(method, null, containersBackedRegistry);
			});
		}

	}

	static class ContainersBackedDynamicPropertyRegistry implements DynamicPropertyRegistry {

		private final DynamicPropertyRegistry delegate;

		private final Set<Startable> startables;

		ContainersBackedDynamicPropertyRegistry(DynamicPropertyRegistry delegate, Set<ContainerField> containerFields) {
			this.delegate = delegate;
			this.startables = containerFields.stream()
				.map(ContainerField::getContainer)
				.filter(Startable.class::isInstance)
				.map(Startable.class::cast)
				.collect(Collectors.toSet());
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

	static class DynamicPropertySourcePropertyRegistrarBeanRegistrationAotProcessor
			implements BeanRegistrationAotProcessor {

		@Override
		public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
			RootBeanDefinition bd = registeredBean.getMergedBeanDefinition();
			if (Boolean.TRUE.equals(bd.getAttribute(DynamicPropertySourcePropertyRegistrar.class.getName()))) {
				ConfigurableListableBeanFactory beanFactory = registeredBean.getBeanFactory();
				String beanName = registeredBean.getBeanName();
				DynamicPropertySourcePropertyRegistrar registrar = beanFactory.getBean(beanName,
						DynamicPropertySourcePropertyRegistrar.class);
				return BeanRegistrationAotContribution
					.withCustomCodeFragments((codeFragments) -> new AotContribution(codeFragments, registrar));
			}
			return null;
		}

		private static final class AotContribution extends BeanRegistrationCodeFragmentsDecorator {

			private static final String CONTAINER_FIELDS_VARIABLE_NAME = "containerFields";

			private static final String DYNAMIC_PROPERTY_SOURCE_METHODS_VARIABLE_NAME = "dynamicPropertySourceMethods";

			private final DynamicPropertySourcePropertyRegistrar registrar;

			AotContribution(BeanRegistrationCodeFragments delegate, DynamicPropertySourcePropertyRegistrar registrar) {
				super(delegate);
				this.registrar = registrar;
			}

			@Override
			public ClassName getTarget(RegisteredBean registeredBean) {
				return ClassName.get(DynamicPropertySourcePropertyRegistrar.class);
			}

			@Override
			public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
					BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
				ClassName targetClassName = beanRegistrationCode.getClassName();
				ReflectionHints hints = generationContext.getRuntimeHints().reflection();
				return beanRegistrationCode.getMethods().add("getInstance", (code) -> {
					code.addModifiers(javax.lang.model.element.Modifier.PRIVATE,
							javax.lang.model.element.Modifier.STATIC);
					code.returns(DynamicPropertySourcePropertyRegistrar.class);
					code.addStatement("$T $L = new $T<>()",
							ParameterizedTypeName.get(Set.class, DynamicPropertySourceMethod.class),
							DYNAMIC_PROPERTY_SOURCE_METHODS_VARIABLE_NAME, LinkedHashSet.class);
					for (Method method : this.registrar.methods) {
						hints.registerMethod(method, ExecutableMode.INVOKE);
						code.addStatement("$L.add(new $T($L))", DYNAMIC_PROPERTY_SOURCE_METHODS_VARIABLE_NAME,
								DynamicPropertySourceMethod.class, findMethod(method, targetClassName));
					}
					code.addStatement("$T $L = new $T<>()", ParameterizedTypeName.get(Set.class, ContainerField.class),
							CONTAINER_FIELDS_VARIABLE_NAME, LinkedHashSet.class);
					for (ContainerField containerField : this.registrar.containerFields) {
						hints.registerField(containerField.getField());
						code.addStatement("$L.add(new $T($L))", CONTAINER_FIELDS_VARIABLE_NAME, ContainerField.class,
								findField(containerField.getField(), targetClassName));
					}
					code.addStatement("return new $T($L, $L)", DynamicPropertySourcePropertyRegistrar.class,
							DYNAMIC_PROPERTY_SOURCE_METHODS_VARIABLE_NAME, CONTAINER_FIELDS_VARIABLE_NAME);
				}).toMethodReference().toCodeBlock();
			}

			private static CodeBlock findMethod(Method method, ClassName targetClassName) {
				return CodeBlock.of("$T.findMethod($L, $S, $L)", ReflectionUtils.class,
						resolveClassName(method.getDeclaringClass(), targetClassName), method.getName(),
						getParameterTypes(method, targetClassName));
			}

			private static CodeBlock getParameterTypes(Method method, ClassName targetClassName) {
				return Arrays.stream(method.getParameterTypes())
					.map((type) -> CodeBlock.of("$L", resolveClassName(type, targetClassName)))
					.collect(CodeBlock.joining(", "));
			}

			private static CodeBlock findField(Field field, ClassName targetClassName) {
				return CodeBlock.of("$T.findField($L, $S)", ReflectionUtils.class,
						resolveClassName(field.getDeclaringClass(), targetClassName), field.getName());
			}

			private static CodeBlock resolveClassName(Class<?> clazz, ClassName targetClassName) {
				if (AccessControl.forClass(clazz).isAccessibleFrom(targetClassName)) {
					return CodeBlock.of("$T.class", clazz);
				}
				return CodeBlock.of("$T.resolveClassName($S, $T.class.getClassLoader())", ClassUtils.class,
						clazz.getName(), targetClassName);
			}

		}

	}

}
