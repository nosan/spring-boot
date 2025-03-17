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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.testcontainers.lifecycle.Startable;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.WildcardTypeName;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.Assert;
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
			Map<Field, Startable> importContainers) {
		Set<Method> methods = MethodIntrospector.selectMethods(definitionClass, this::isAnnotated);
		if (methods.isEmpty()) {
			return;
		}
		methods.forEach(this::assertValid);
		RootBeanDefinition registrarDefinition = new RootBeanDefinition();
		registrarDefinition.setBeanClass(DynamicPropertySourcePropertyRegistrar.class);
		registrarDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registrarDefinition.setInstanceSupplier(() -> new DynamicPropertySourcePropertyRegistrar(methods,
				new LinkedHashSet<>(importContainers.values())));
		new AotMetadata(methods, new LinkedHashSet<>(importContainers.keySet())).addTo(registrarDefinition);
		beanDefinitionRegistry.registerBeanDefinition(definitionClass.getName() + ".dynamicPropertyRegistrar",
				registrarDefinition);
	}

	private boolean isAnnotated(Method method) {
		return MergedAnnotations.from(method).isPresent(DynamicPropertySource.class);
	}

	private void assertValid(Method method) {
		Assert.state(Modifier.isStatic(method.getModifiers()),
				() -> "@DynamicPropertySource method '" + method.getName() + "' must be static");
		Class<?>[] types = method.getParameterTypes();
		Assert.state(types.length == 1 && types[0] == DynamicPropertyRegistry.class,
				() -> "@DynamicPropertySource method '" + method.getName()
						+ "' must accept a single DynamicPropertyRegistry argument");
	}

	static class DynamicPropertySourcePropertyRegistrar implements DynamicPropertyRegistrar {

		private final Set<Method> methods;

		private final Set<Startable> containers;

		DynamicPropertySourcePropertyRegistrar(Set<Method> methods, Set<Startable> containers) {
			this.methods = methods;
			this.containers = containers;
		}

		@Override
		public void accept(DynamicPropertyRegistry registry) {
			DynamicPropertyRegistry containersBackedRegistry = new ContainersBackedDynamicPropertyRegistry(registry,
					this.containers);
			this.methods.forEach((method) -> {
				ReflectionUtils.makeAccessible(method);
				ReflectionUtils.invokeMethod(method, null, containersBackedRegistry);
			});
		}

	}

	static class ContainersBackedDynamicPropertyRegistry implements DynamicPropertyRegistry {

		private final DynamicPropertyRegistry delegate;

		private final Set<Startable> containers;

		ContainersBackedDynamicPropertyRegistry(DynamicPropertyRegistry delegate, Set<Startable> containers) {
			this.delegate = delegate;
			this.containers = containers;
		}

		@Override
		public void add(String name, Supplier<Object> valueSupplier) {
			this.delegate.add(name, () -> {
				startContainers();
				return valueSupplier.get();
			});
		}

		private void startContainers() {
			this.containers.forEach(Startable::start);
		}

	}

	private record AotMetadata(Set<Method> methods, Set<Field> containers) {

		private static final String ATTRIBUTE_NAME = AotMetadata.class.getName();

		private void addTo(AttributeAccessor attributes) {
			attributes.setAttribute(ATTRIBUTE_NAME, this);
		}

		private static boolean isPresent(AttributeAccessor attributes) {
			return attributes.getAttribute(ATTRIBUTE_NAME) != null;
		}

		private static AotMetadata get(AttributeAccessor attributes) {
			return (AotMetadata) attributes.getAttribute(ATTRIBUTE_NAME);
		}
	}

	static class DynamicPropertySourcePropertyRegistrarBeanRegistrationAotProcessor
			implements BeanRegistrationAotProcessor {

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

			private static final ParameterizedTypeName CLASS_TYPE = ParameterizedTypeName
				.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class));

			private final Set<Method> methods;

			private final Set<Field> containers;

			private AotContribution(BeanRegistrationCodeFragments delegate, AotMetadata metadata) {
				super(delegate);
				this.methods = metadata.methods();
				this.containers = metadata.containers();
			}

			@Override
			public CodeBlock generateSetBeanDefinitionPropertiesCode(GenerationContext generationContext,
					BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition,
					Predicate<String> attributeFilter) {
				return super.generateSetBeanDefinitionPropertiesCode(generationContext, beanRegistrationCode,
						beanDefinition, attributeFilter);
			}

			@Override
			public ClassName getTarget(RegisteredBean registeredBean) {
				return ClassName.get(registeredBean.getBeanClass());
			}

			@Override
			public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
					BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
				for (Field field : this.containers) {
					generationContext.getRuntimeHints().reflection().registerField(field);
				}
				for (Method method : this.methods) {
					generationContext.getRuntimeHints().reflection().registerMethod(method, ExecutableMode.INVOKE);
				}
				return beanRegistrationCode.getMethods().add("getInstance", (code) -> {
					code.addModifiers(javax.lang.model.element.Modifier.PRIVATE,
							javax.lang.model.element.Modifier.STATIC);
					code.returns(DynamicPropertySourcePropertyRegistrar.class);
					code.addStatement("$T methods = new $T<>()", ParameterizedTypeName.get(Set.class, Method.class),
							LinkedHashSet.class);
					for (Method method : this.methods) {
						code.addStatement("methods.add($L)", getMethod(beanRegistrationCode, method).toMethodReference()
							.toInvokeCodeBlock(ArgumentCodeGenerator.none()));
					}
					code.addStatement("$T containers = new $T<>()",
							ParameterizedTypeName.get(Set.class, Startable.class), LinkedHashSet.class);
					for (Field field : this.containers) {
						code.addStatement("containers.add($L)",
								getContainerField(beanRegistrationCode, field).toMethodReference()
									.toInvokeCodeBlock(ArgumentCodeGenerator.none()));
					}
					code.addStatement("return new $T(methods, containers)",
							DynamicPropertySourcePropertyRegistrar.class);
				}).toMethodReference().toCodeBlock();
			}

			private static GeneratedMethod getMethod(BeanRegistrationCode beanRegistrationCode, Method method) {
				return beanRegistrationCode.getMethods().add(new String[] { "get", method.getName() }, (code) -> {
					code.addModifiers(javax.lang.model.element.Modifier.PRIVATE,
							javax.lang.model.element.Modifier.STATIC);
					code.returns(Method.class);
					code.addStatement("$T clazz = $T.resolveClassName($S, $T.class.getClassLoader())", CLASS_TYPE,
							ClassUtils.class, method.getDeclaringClass().getName(),
							beanRegistrationCode.getClassName());
					code.addStatement("return $T.findMethod(clazz, $S, $L)", ReflectionUtils.class, method.getName(),
							Arrays.stream(method.getParameterTypes())
								.map((type) -> CodeBlock.of("$T.class", type))
								.collect(CodeBlock.joining(", ")));
				});
			}

			private static GeneratedMethod getContainerField(BeanRegistrationCode beanRegistrationCode, Field field) {
				return beanRegistrationCode.getMethods().add(new String[] { "get", field.getName() }, (code) -> {
					code.addModifiers(javax.lang.model.element.Modifier.PRIVATE,
							javax.lang.model.element.Modifier.STATIC);
					code.returns(Startable.class);
					code.addStatement("$T clazz = $T.resolveClassName($S, $T.class.getClassLoader())", CLASS_TYPE,
							ClassUtils.class, field.getDeclaringClass().getName(), beanRegistrationCode.getClassName());
					code.addStatement("$T field = $T.findField(clazz, $S)", Field.class, ReflectionUtils.class,
							field.getName());
					code.addStatement("$T.makeAccessible(field)", ReflectionUtils.class);
					code.addStatement("return ($T) $T.getField(field, null)", Startable.class, ReflectionUtils.class);
				});
			}

		}

	}

}
