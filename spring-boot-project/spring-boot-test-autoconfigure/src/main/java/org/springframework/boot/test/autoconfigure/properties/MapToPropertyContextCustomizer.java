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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ContextCustomizer} to support {@link MapToProperty} annotation.
 *
 * @author Dmytro Nosan
 */
class MapToPropertyContextCustomizer implements ContextCustomizer {

	private final Map<Field, Set<String>> fields;

	MapToPropertyContextCustomizer(Map<Field, Set<String>> fields) {
		this.fields = fields;
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		Map<String, Object> properties = getProperties(this.fields);
		MapPropertySource propertySource = new MapPropertySource("@MapToProperty", properties);
		context.getEnvironment().getPropertySources().addFirst(propertySource);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		MapToPropertyContextCustomizer other = (MapToPropertyContextCustomizer) obj;
		if (this.fields.equals(other.fields)) {
			for (Field field : this.fields.keySet()) {
				Class<?> type = field.getType();
				if (ClassUtils.isAssignable(Supplier.class, type) || ClassUtils.isAssignable(Callable.class, type)
						|| ClassUtils.isAssignable(Optional.class, type)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.fields.hashCode();
	}

	private static Map<String, Object> getProperties(Map<Field, Set<String>> fields) {
		Map<String, Object> properties = new LinkedHashMap<>();
		fields.forEach((field, names) -> names.forEach((name) -> {
			Object value = unwrapValue(getField(field, null));
			putProperties(field, name, value, properties);
		}));
		return properties;
	}

	private static void putProperties(Field field, String name, Object value, Map<String, Object> properties) {
		if (value instanceof Collection<?>) {
			putCollection(field, name, (Collection<?>) value, properties);
		}
		else if (ObjectUtils.isArray(value)) {
			putCollection(field, name, Arrays.asList(ObjectUtils.toObjectArray(value)), properties);
		}
		else if (value instanceof Map<?, ?>) {
			putMap(field, name, (Map<?, ?>) value, properties);
		}
		else if (value instanceof TestPropertyValues) {
			putTestPropertyValues(field, name, ((TestPropertyValues) value), properties);
		}
		else {
			putValue(field, name, value, properties);
		}
	}

	private static void putCollection(Field field, String prefix, Collection<?> collection,
			Map<String, Object> properties) {
		if (!CollectionUtils.isEmpty(collection)) {
			Iterator<?> iterator = collection.iterator();
			int index = 0;
			while (iterator.hasNext()) {
				Object value = iterator.next();
				putValue(field, prefix + "[" + index + "]", value, properties);
				index++;
			}
		}
	}

	private static void putMap(Field field, String prefix, Map<?, ?> map, Map<String, Object> properties) {
		if (!CollectionUtils.isEmpty(map)) {
			map.forEach((key, value) -> putValue(field, dotAppend(prefix, Objects.toString(key)), value, properties));
		}
	}

	private static void putTestPropertyValues(Field field, String name, TestPropertyValues testPropertyValues,
			Map<String, Object> properties) {
		Map value = (Map<?, ?>) getField(ReflectionUtils.findField(TestPropertyValues.class, "properties"),
				testPropertyValues);
		putMap(field, name, value, properties);
	}

	private static void putValue(Field field, String name, Object value, Map<String, Object> properties) {
		Assert.hasText(name, () -> "Property name for a field [" + field + "] is empty");
		if (properties.containsKey(name)) {
			throw new IllegalStateException("Property [" + name + "] is already present in " + properties);
		}
		properties.put(name, value);
	}

	private static String dotAppend(String prefix, String postfix) {
		if (StringUtils.hasText(prefix)) {
			return prefix.endsWith(".") ? prefix + postfix : prefix + "." + postfix;
		}
		return postfix;
	}

	private static Object getField(Field field, Object instance) {
		Assert.state(field != null, "Field must not be null");
		ReflectionUtils.makeAccessible(field);
		return ReflectionUtils.getField(field, instance);
	}

	private static Object unwrapValue(Object value) {
		if (value instanceof Supplier<?>) {
			return unwrapValue(((Supplier<?>) value).get());
		}
		if (value instanceof Callable<?>) {
			try {
				return unwrapValue(((Callable<?>) value).call());
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("Can not get a value from a Callable [" + value + "]", ex);
			}
		}
		if (value instanceof Optional<?>) {
			return unwrapValue(((Optional<?>) value).orElse(null));
		}
		return value;
	}

}
