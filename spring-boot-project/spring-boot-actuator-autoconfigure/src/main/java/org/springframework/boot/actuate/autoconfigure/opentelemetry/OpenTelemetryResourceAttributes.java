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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SupplierUtils;

/**
 * {@link OpenTelemetryResourceAttributes} is used for handling string-based OpenTelemetry
 * resource attributes.
 * <p>
 * This class is meant for internal use only and is not a replacement for the
 * OpenTelemetry Java Resource SDK.
 * <p>
 * <a href= "https://opentelemetry.io/docs/specs/otel/resource/sdk/">OpenTelemetry
 * Resource Specification</a>
 *
 * @author Dmytro Nosan
 * @since 3.5.0
 * @see #fromEnv()
 */
public final class OpenTelemetryResourceAttributes {

	private final Map<String, String> attributes = new LinkedHashMap<>();

	/**
	 * Creates an {@link OpenTelemetryResourceAttributes} instance based on environment
	 * variables. This method fetches attributes defined in the
	 * {@code OTEL_RESOURCE_ATTRIBUTES} and {@code OTEL_SERVICE_NAME} environment
	 * variables.
	 * <p>
	 * If {@code service.name} is also provided in {@code OTEL_RESOURCE_ATTRIBUTES}, then
	 * {@code OTEL_SERVICE_NAME} takes precedence.
	 * @return an {@link OpenTelemetryResourceAttributes}
	 */
	public static OpenTelemetryResourceAttributes fromEnv() {
		return fromEnv(System::getenv);
	}

	static OpenTelemetryResourceAttributes fromEnv(Function<String, String> getEnv) {
		OpenTelemetryResourceAttributes attributes = new OpenTelemetryResourceAttributes();
		for (String attribute : StringUtils.tokenizeToStringArray(getEnv.apply("OTEL_RESOURCE_ATTRIBUTES"), ",")) {
			int index = attribute.indexOf('=');
			if (index > 0) {
				String key = attribute.substring(0, index);
				String value = attribute.substring(index + 1);
				attributes.put(key.trim(), decode(value.trim()));
			}
		}
		String otelServiceName = getEnv.apply("OTEL_SERVICE_NAME");
		if (otelServiceName != null) {
			attributes.put("service.name", otelServiceName);
		}
		return attributes;
	}

	/**
	 * Return Resource attributes as a Map.
	 * @return the resource attributes as key-value pairs
	 */
	public Map<String, String> asMap() {
		return Collections.unmodifiableMap(this.attributes);
	}

	/**
	 * Performs the given action for each key-value pairs.
	 * @param consumer the operation to perform for each entry
	 */
	public void forEach(BiConsumer<String, String> consumer) {
		this.attributes.forEach(consumer);
	}

	/**
	 * Merge attributes with the provided resource attributes.
	 * <p>
	 * If a key exists in both, the value from provided resource takes precedence, even if
	 * it is empty.
	 * <p>
	 * <b>Keys that are null or empty will be skipped.</b>
	 * <p>
	 * <b>Values that are null will be skipped.</b>
	 * @param resourceAttributes resource attributes
	 */
	public void putAll(Map<String, String> resourceAttributes) {
		if (!CollectionUtils.isEmpty(resourceAttributes)) {
			resourceAttributes.forEach(this::put);
		}
	}

	/**
	 * Adds a key-value pair to the resource attributes.
	 * <p>
	 * <b>Key that is null or empty it will be skipped.</b>
	 * <p>
	 * <b>Value that is null will be skipped.</b>
	 * @param key the attribute key to add, must not be null or empty
	 * @param valueSupplier the attribute value supplier
	 */
	public void putIfAbsent(String key, Supplier<String> valueSupplier) {
		if (!this.attributes.containsKey(key)) {
			put(key, SupplierUtils.resolve(valueSupplier));
		}
	}

	private void put(String key, String value) {
		if (StringUtils.hasLength(key) && value != null) {
			this.attributes.put(key, value);
		}
	}

	/**
	 * Decodes a percent-encoded string. Converts sequences like '%HH' (where HH
	 * represents hexadecimal digits) back into their literal representations.
	 * <p>
	 * Inspired by {@code org.apache.commons.codec.net.PercentCodec}.
	 * @param value value to decode
	 * @return the decoded string
	 */
	private static String decode(String value) {
		if (value.indexOf('%') < 0) {
			return value;
		}
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];
			if (b != '%') {
				bos.write(b);
				continue;
			}
			int u = decodeHex(bytes, i + 1);
			int l = decodeHex(bytes, i + 2);
			if (u >= 0 && l >= 0) {
				bos.write((u << 4) + l);
			}
			else {
				throw new IllegalArgumentException(
						"Failed to decode percent-encoded characters at index %d in the value: '%s'".formatted(i,
								value));
			}
			i += 2;
		}
		return bos.toString(StandardCharsets.UTF_8);
	}

	private static int decodeHex(byte[] bytes, int index) {
		return (index < bytes.length) ? Character.digit(bytes[index], 16) : -1;
	}

}
