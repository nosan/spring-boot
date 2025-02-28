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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import io.opentelemetry.api.internal.PercentEscaper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link OpenTelemetryResourceAttributes}.
 *
 * @author Dmytro Nosan
 */
class OpenTelemetryResourceAttributesTests {

	private static Random random;

	private static final PercentEscaper escaper = PercentEscaper.create();

	@BeforeAll
	static void beforeAll() {
		long seed = new Random().nextLong();
		random = new Random(seed);
	}

	@Test
	void asMapShouldReturnEmptyMapByDefault() {
		assertThat(new OpenTelemetryResourceAttributes().asMap()).isEmpty();
	}

	@Test
	void asMapShouldReturnExistingAttributes() {
		OpenTelemetryResourceAttributes attributes = new OpenTelemetryResourceAttributes();
		attributes.putIfAbsent("key", () -> "value");
		assertThat(attributes.asMap()).containsEntry("key", "value");
	}

	@Test
	void forEachShouldIterateOverExistingAttributes() {
		OpenTelemetryResourceAttributes attributes = new OpenTelemetryResourceAttributes();
		attributes.putIfAbsent("key", () -> "value");
		attributes.putIfAbsent("key1", () -> "value1");
		Map<String, String> result = new LinkedHashMap<>();
		attributes.forEach(result::put);
		assertThat(result).containsEntry("key", "value").containsEntry("key1", "value1");
	}

	@Test
	void putValueShouldIgnoreInvalidKeysAndValues() {
		OpenTelemetryResourceAttributes attributes = new OpenTelemetryResourceAttributes();
		attributes.putIfAbsent("key", () -> "value");
		attributes.putIfAbsent("key1", () -> "value1");
		attributes.putIfAbsent("key2", () -> null);
		attributes.putIfAbsent(null, () -> "value3");
		attributes.putIfAbsent("", () -> "value4");
		assertThat(attributes.asMap()).hasSize(2).containsEntry("key", "value").containsEntry("key1", "value1");
	}

	@Test
	void putAllShouldOverrideExistingKeys() {
		Map<String, String> envVariables = new LinkedHashMap<>();
		Map<String, String> userAttributes = new LinkedHashMap<>();
		userAttributes.put("service.group", "custom-group");
		userAttributes.put("key2", "");
		envVariables.put("OTEL_SERVICE_NAME", "custom-service");
		envVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key1=value1,key2=value2");
		OpenTelemetryResourceAttributes attributes = OpenTelemetryResourceAttributes.fromEnv(envVariables::get);
		attributes.putAll(userAttributes);
		assertThat(attributes.asMap()).hasSize(4)
			.containsEntry("service.name", "custom-service")
			.containsEntry("service.group", "custom-group")
			.containsEntry("key1", "value1")
			.containsEntry("key2", "");
	}

	@Test
	void putAllShouldIgnoreInvalidKeysAndValues() {
		Map<String, String> envVariables = new LinkedHashMap<>();
		Map<String, String> userAttributes = new LinkedHashMap<>();
		userAttributes.put("service.group", null);
		userAttributes.put("service.name", null);
		userAttributes.put(null, "null-key");
		userAttributes.put("", "empty");
		userAttributes.put("  ", "non-empty");
		envVariables.put("OTEL_SERVICE_NAME", "custom-service");
		envVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key1=value1,key2=value2");
		OpenTelemetryResourceAttributes attributes = OpenTelemetryResourceAttributes.fromEnv(envVariables::get);
		attributes.putAll(userAttributes);
		assertThat(attributes.asMap()).hasSize(4)
			.containsEntry("service.name", "custom-service")
			.containsEntry("key1", "value1")
			.containsEntry("key2", "value2");
	}

	@Test
	void otelServiceNameShouldTakePrecedenceOverOtelResourceAttributes() {
		Map<String, String> envVariables = new LinkedHashMap<>();
		envVariables.put("OTEL_RESOURCE_ATTRIBUTES", "service.name=ignored");
		envVariables.put("OTEL_SERVICE_NAME", "otel-service");
		OpenTelemetryResourceAttributes attributes = OpenTelemetryResourceAttributes.fromEnv(envVariables::get);
		assertThat(attributes.asMap()).hasSize(1).containsEntry("service.name", "otel-service");
	}

	@Test
	void otelServiceNameWhenEmptyShouldTakePrecedenceOverOtelResourceAttributes() {
		Map<String, String> envVariables = new LinkedHashMap<>();
		envVariables.put("OTEL_RESOURCE_ATTRIBUTES", "service.name=ignored");
		envVariables.put("OTEL_SERVICE_NAME", "");
		OpenTelemetryResourceAttributes attributes = OpenTelemetryResourceAttributes.fromEnv(envVariables::get);
		assertThat(attributes.asMap()).hasSize(1).containsEntry("service.name", "");
	}

	@Test
	void shouldBeCreatedFromEnvironmentVariables() {
		Map<String, String> envVariables = new LinkedHashMap<>();
		envVariables.put("OTEL_RESOURCE_ATTRIBUTES",
				", ,,key1=value1,key2= value2, key3=value3,key4=,=value5,key6,=,key7=spring+boot,key8=ś,key9=%20A%20");
		OpenTelemetryResourceAttributes attributes = OpenTelemetryResourceAttributes.fromEnv(envVariables::get);
		assertThat(attributes.asMap()).hasSize(7)
			.containsEntry("key1", "value1")
			.containsEntry("key2", "value2")
			.containsEntry("key3", "value3")
			.containsEntry("key4", "")
			.containsEntry("key7", "spring+boot")
			.containsEntry("key8", "ś")
			.containsEntry("key9", " A ");
	}

	@Test
	void otelResourceAttributesShouldBeDecoded() {
		Stream.generate(this::generateRandomString).limit(10000).forEach((value) -> {
			Map<String, String> envVariables = Map.of("OTEL_RESOURCE_ATTRIBUTES", "key=" + escaper.escape(value));
			OpenTelemetryResourceAttributes attributes = OpenTelemetryResourceAttributes.fromEnv(envVariables::get);
			assertThat(attributes.asMap()).hasSize(1).containsEntry("key", value);
		});
	}

	@Test
	void shouldThrowIllegalArgumentExceptionWhenDecodingPercentIllegalHexChar() {
		Map<String, String> envVariables = Map.of("OTEL_RESOURCE_ATTRIBUTES", "key=abc%ß");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> OpenTelemetryResourceAttributes.fromEnv(envVariables::get))
			.withMessage("Failed to decode percent-encoded characters at index 3 in the value: 'abc%ß'");
	}

	@Test
	void shouldUseReplacementCharWhenDecodingNonUtf8Character() {
		Map<String, String> envVariables = Map.of("OTEL_RESOURCE_ATTRIBUTES", "key=%a3%3e");
		OpenTelemetryResourceAttributes attributes = OpenTelemetryResourceAttributes.fromEnv(envVariables::get);
		assertThat(attributes.asMap()).containsEntry("key", "\ufffd>");
	}

	@Test
	void shouldThrowIllegalArgumentExceptionWhenDecodingPercent() {
		Map<String, String> envVariables = Map.of("OTEL_RESOURCE_ATTRIBUTES", "key=%");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> OpenTelemetryResourceAttributes.fromEnv(envVariables::get))
			.withMessage("Failed to decode percent-encoded characters at index 0 in the value: '%'");
	}

	private String generateRandomString() {
		return random.ints(32, 127)
			.limit(64)
			.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
			.toString();
	}

}
