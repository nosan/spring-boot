/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client.autoconfigure.metrics.imperative;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * Tests for {@link ImperativeHttpClientMetricsAutoConfiguration}.
 *
 * @author Dmytro Nosan
 */
class ImperativeHttpClientMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class,
				ImperativeHttpClientMetricsAutoConfiguration.class, ImperativeHttpClientAutoConfiguration.class))
		.withPropertyValues("spring.http.clients.imperative.factory=http-components");

	@Test
	void shouldBindMetricsForEachHttpComponentsClient() {
		this.contextRunner.withBean(SimpleMeterRegistry.class)
			.withPropertyValues("spring.http.clients.imperative.factory=http-components")
			.run((context) -> {
				MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
				ClientHttpRequestFactoryBuilder<?> builder = context.getBean(ClientHttpRequestFactoryBuilder.class);
				assertThat(builder).isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder.class);
				builder.build();
				builder.build();
				assertPoolMetricsBound(meterRegistry, "0");
				assertPoolMetricsBound(meterRegistry, "1");
			});
	}

	private static void assertPoolMetricsBound(MeterRegistry meterRegistry, String clientId) {
		assertThat(meterRegistry.get("http.client.pool.available")
			.tag("client.type", "imperative")
			.tag("client.name", "http-components")
			.tag("client.id", clientId)
			.gauge()).as("pool metrics should be bound for client %s", clientId).isNotNull();
	}

}
