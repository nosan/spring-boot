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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenTelemetryResourceAttributes}.
 *
 * @author Dmytro Nosan
 */
class OpenTelemetryResourceAttributesTests {

	private final Map<String, String> environmentVariables = new LinkedHashMap<>();

	private final Map<String, String> resourceAttributes = new LinkedHashMap<>();

	@Test
	void otelServiceNameShouldTakePrecedenceOverOtelResourceAttributes() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "service.name=ignored");
		this.environmentVariables.put("OTEL_SERVICE_NAME", "otel-service");

		OpenTelemetryResourceAttributes attributes = getAttributes();
		assertThat(attributes.asMap()).hasSize(1).containsEntry("service.name", "otel-service");
	}

	@Test
	void otelResourceAttributesShouldBeUsed() {
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES",
				" key1 = value1 ,key2= value2,key3,key4=,,=key5,service.name=otel-service,service.group=otel-group");
		OpenTelemetryResourceAttributes attributes = getAttributes();
		assertThat(attributes.asMap()).hasSize(5)
			.containsEntry("key1", "value1")
			.containsEntry("key2", "value2")
			.containsEntry("key4", "")
			.containsEntry("service.name", "otel-service")
			.containsEntry("service.group", "otel-group");
	}

	@Test
	void userResourceAttributesShouldBeMergedWithEnvironmentVariables() {
		this.resourceAttributes.put("service.group", "custom-group");
		this.environmentVariables.put("OTEL_SERVICE_NAME", "custom-service");
		this.environmentVariables.put("OTEL_RESOURCE_ATTRIBUTES", "key1=value1,key2=value2");

		OpenTelemetryResourceAttributes attributes = getAttributes();
		assertThat(attributes.asMap()).hasSize(4)
			.containsEntry("service.name", "custom-service")
			.containsEntry("service.group", "custom-group")
			.containsEntry("key1", "value1")
			.containsEntry("key2", "value2");
	}

	@Test
	void userResourceAttributesShouldBeUsed() {
		this.resourceAttributes.put("service.name", "custom-service");
		this.resourceAttributes.put("service.group", "custom-group");
		OpenTelemetryResourceAttributes attributes = getAttributes();
		assertThat(attributes.asMap()).hasSize(2)
			.containsEntry("service.name", "custom-service")
			.containsEntry("service.group", "custom-group");
	}

	private OpenTelemetryResourceAttributes getAttributes() {
		return new OpenTelemetryResourceAttributes(this.resourceAttributes, this.environmentVariables::get);
	}

}
