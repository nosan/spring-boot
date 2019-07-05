/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.properties;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ContextCustomizerFactory} to support {@link MapToProperty} annotation.
 *
 * @author Dmytro Nosan
 */
class MapToPropertyContextFactoryCustomizer implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		Map<Field, Set<String>> fields = getFields(testClass);
		if (!fields.isEmpty()) {
			return new MapToPropertyContextCustomizer(fields);
		}
		return null;
	}

	private static Map<Field, Set<String>> getFields(Class<?> testClass) {
		Map<Field, Set<String>> fields = new LinkedHashMap<>();
		ReflectionUtils.doWithFields(testClass, (field) -> {
			Set<String> names = MergedAnnotations.from(field).stream(MapToProperty.class)
					.map(MergedAnnotation::synthesize).map(MapToProperty::value)
					.collect(Collectors.toCollection(LinkedHashSet::new));
			if (!names.isEmpty()) {
				Assert.isTrue(Modifier.isStatic(field.getModifiers()),
						() -> "@MapToProperty cannot be used with non-static fields");
				Assert.isTrue(Modifier.isFinal(field.getModifiers()),
						() -> "@MapToProperty cannot be used with non-final fields");
				Assert.isTrue(!field.getType().equals(Object.class),
						() -> "@MapToProperty cannot be used with [" + Object.class + "]");
				fields.put(field, names);
			}
		});
		return fields;
	}

}
