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

package org.springframework.boot.actuate.autoconfigure.opentelemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.util.StringUtils;

/**
 *
 * {@link OpenTelemetryResourceAttributes} extracts information from the
 * {@code OTEL_RESOURCE_ATTRIBUTES} and {@code OTEL_SERVICE_NAME} environment variables
 * and merge them, with resource attributes provided by the user, i.e. the user provided
 * resource information has higher priority.
 *
 * @author Dmytro Nosan
 * @since 3.4.4
 */
public final class OpenTelemetryResourceAttributes {

	private final Map<String, String> resourceAttributes;

	private final Function<String, String> getEnv;

	/**
	 * Creates a new instance of {@link OpenTelemetryResourceAttributes}.
	 * @param resourceAttributes user provided resource attributes to be used
	 */
	public OpenTelemetryResourceAttributes(Map<String, String> resourceAttributes) {
		this(resourceAttributes, System::getenv);
	}

	/**
	 * Creates a new {@link OpenTelemetryResourceAttributes} instance.
	 * @param resourceAttributes user provided resource attributes to be used
	 * @param getEnv a function to retrieve environment variables by name
	 */
	OpenTelemetryResourceAttributes(Map<String, String> resourceAttributes, Function<String, String> getEnv) {
		this.resourceAttributes = (resourceAttributes != null) ? resourceAttributes : Collections.emptyMap();
		this.getEnv = (getEnv != null) ? getEnv : System::getenv;
	}

	/**
	 * Returns OpenTelemetry resource attributes by merging environment-based attributes
	 * and user-defined resource attributes.
	 * @return resource attributes
	 */
	public Map<String, String> asMap() {
		Map<String, String> attributes = getResourceAttributesFromEnv();
		attributes.putAll(this.resourceAttributes);
		return attributes;
	}

	/**
	 * Parses OpenTelemetry resource attributes from the {@link System#getenv()}. This
	 * method fetches attributes defined in the {@code OTEL_RESOURCE_ATTRIBUTES} and
	 * {@code OTEL_SERVICE_NAME} environment variables and provides them as key-value
	 * pairs.
	 * <p>
	 * If {@code service.name} is also provided in {@code OTEL_RESOURCE_ATTRIBUTES}, then
	 * {@code OTEL_SERVICE_NAME} takes precedence.
	 */
	private Map<String, String> getResourceAttributesFromEnv() {
		Map<String, String> attributes = new LinkedHashMap<>();
		for (String attribute : StringUtils.tokenizeToStringArray(getEnv("OTEL_RESOURCE_ATTRIBUTES"), ",")) {
			int index = attribute.indexOf('=');
			if (index > 0) {
				String key = attribute.substring(0, index);
				String value = attribute.substring(index + 1);
				attributes.put(key.trim(), value.trim());
			}
		}
		String otelServiceName = getEnv("OTEL_SERVICE_NAME");
		if (otelServiceName != null) {
			attributes.put("service.name", otelServiceName);
		}
		return attributes;
	}

	private String getEnv(String name) {
		return this.getEnv.apply(name);
	}

}
