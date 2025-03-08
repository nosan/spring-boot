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

package org.springframework.boot.actuate.autoconfigure.metrics.http;

import java.util.Collection;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.httpcomponents.hc5.PoolingHttpClientConnectionManagerMetricsBinder;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.http.HttpClientMetricsAutoConfiguration.HttpComponentsClientHttpRequestFactoryBuilderMetricsPostProcessor;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.SimpleClientHttpRequestFactoryBuilder;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link HttpClientMetricsAutoConfiguration}.
 *
 * @author Dmytro Nosan
 */
class HttpClientMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(HttpClientAutoConfiguration.class, HttpClientMetricsAutoConfiguration.class));

	@Test
	void httpComponentsMetricsAreRegistered() {
		this.contextRunner.with(MetricsRun.simple())
			.withPropertyValues("spring.http.client.factory=http-components")
			.run((context) -> {
				assertThat(context).hasSingleBean(MeterRegistry.class)
					.hasSingleBean(HttpComponentsClientHttpRequestFactoryBuilder.class)
					.hasSingleBean(HttpComponentsClientHttpRequestFactoryBuilderMetricsPostProcessor.class);
				MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
				doesNotHaveGaugeMetric(meterRegistry, "httpcomponents.httpclient.pool.total.connections");
				context.getBean(HttpComponentsClientHttpRequestFactoryBuilder.class).build();
				hasGaugeMetric(meterRegistry, "httpcomponents.httpclient.pool.total.connections");
			});
	}

	@Test
	void httpComponentsMetricsAreRegisteredForWhenMultiplePoolingHttpClientConnectionManagers() {
		this.contextRunner.with(MetricsRun.simple())
			.withBean("httpBuilder", ClientHttpRequestFactoryBuilder.class,
					ClientHttpRequestFactoryBuilder::httpComponents)
			.run((context) -> {
				assertThat(context).hasSingleBean(MeterRegistry.class)
					.hasSingleBean(HttpComponentsClientHttpRequestFactoryBuilder.class)
					.hasSingleBean(HttpComponentsClientHttpRequestFactoryBuilderMetricsPostProcessor.class);
				MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
				HttpComponentsClientHttpRequestFactoryBuilder httpClientBuilder = context
					.getBean(HttpComponentsClientHttpRequestFactoryBuilder.class);
				doesNotHaveGaugeMetric(meterRegistry, "httpcomponents.httpclient.pool.total.connections");
				httpClientBuilder.build();
				hasGaugeMetric(meterRegistry, "httpcomponents.httpclient.pool.total.connections", "httpclient",
						"httpBuilder.pool-0");
				doesNotHaveGaugeMetric(meterRegistry, "httpcomponents.httpclient.pool.total.connections", "httpclient",
						"httpBuilder.pool-1");
				httpClientBuilder.build();
				hasGaugeMetric(meterRegistry, "httpcomponents.httpclient.pool.total.connections", "httpclient",
						"httpBuilder.pool-1");
			});
	}

	@Test
	void httpComponentsMetricsAreNotRegisteredWhenMultipleHttpComponentsClientHttpRequestFactoryBuilder() {
		this.contextRunner.with(MetricsRun.simple())
			.withBean("httpBuilder1", ClientHttpRequestFactoryBuilder.class,
					ClientHttpRequestFactoryBuilder::httpComponents)
			.withBean("httpBuilder2", ClientHttpRequestFactoryBuilder.class,
					ClientHttpRequestFactoryBuilder::httpComponents)
			.run((context) -> {
				assertThat(context).hasSingleBean(MeterRegistry.class)
					.hasSingleBean(HttpComponentsClientHttpRequestFactoryBuilderMetricsPostProcessor.class);
				MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
				Collection<HttpComponentsClientHttpRequestFactoryBuilder> httpClientBuilders = context
					.getBeansOfType(HttpComponentsClientHttpRequestFactoryBuilder.class)
					.values();
				assertThat(httpClientBuilders).hasSize(2);
				doesNotHaveGaugeMetric(meterRegistry, "httpcomponents.httpclient.pool.total.connections");
				httpClientBuilders.forEach(ClientHttpRequestFactoryBuilder::build);
				hasGaugeMetric(meterRegistry, "httpcomponents.httpclient.pool.total.connections", "httpclient",
						"httpBuilder1.pool-0");
				hasGaugeMetric(meterRegistry, "httpcomponents.httpclient.pool.total.connections", "httpclient",
						"httpBuilder2.pool-0");
			});
	}

	@Test
	void httpComponentsMetricsAreNotRegisteredWhenSimpleClientHttpRequestFactoryBuilder() {
		this.contextRunner.with(MetricsRun.simple())
			.withPropertyValues("spring.http.client.factory=simple")
			.run((context) -> {
				assertThat(context).hasSingleBean(MeterRegistry.class);
				assertThat(context).hasSingleBean(ClientHttpRequestFactoryBuilder.class)
					.hasSingleBean(SimpleClientHttpRequestFactoryBuilder.class);
				MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
				SimpleClientHttpRequestFactoryBuilder httpClientBuilder = context
					.getBean(SimpleClientHttpRequestFactoryBuilder.class);
				doesNotHaveGaugeMetric(meterRegistry, "httpcomponents.httpclient.pool.total.connections");
				httpClientBuilder.build();
				doesNotHaveGaugeMetric(meterRegistry, "httpcomponents.httpclient.pool.total.connections");
			});
	}

	@Test
	void httpComponentsMetricsAreNotRegisteredWhenMeterRegistryBeanIsNotPresent() {
		this.contextRunner.run((context) -> assertThat(context)
			.doesNotHaveBean(HttpComponentsClientHttpRequestFactoryBuilderMetricsPostProcessor.class));
	}

	@Test
	void httpComponentsMetricsAreNotRegisteredWhenPoolingHttpClientConnectionManagerClassIsNotPresent() {
		this.contextRunner.with(MetricsRun.simple())
			.withClassLoader(new FilteredClassLoader(PoolingHttpClientConnectionManager.class))
			.run((context) -> assertThat(context)
				.doesNotHaveBean(HttpComponentsClientHttpRequestFactoryBuilderMetricsPostProcessor.class));
	}

	@Test
	void httpComponentsMetricsAreNotRegisteredWhenPoolingHttpClientConnectionManagerMetricsBinderClassIsNotPresent() {
		this.contextRunner.with(MetricsRun.simple())
			.withClassLoader(new FilteredClassLoader(PoolingHttpClientConnectionManagerMetricsBinder.class))
			.run((context) -> assertThat(context)
				.doesNotHaveBean(HttpComponentsClientHttpRequestFactoryBuilderMetricsPostProcessor.class));
	}

	private void hasGaugeMetric(MeterRegistry meterRegistry, String name, String... tags) {
		assertThatNoException().isThrownBy(() -> meterRegistry.get(name).tags(tags).gauge());
	}

	private void doesNotHaveGaugeMetric(MeterRegistry meterRegistry, String name, String... tags) {
		assertThatExceptionOfType(MeterNotFoundException.class)
			.isThrownBy(() -> meterRegistry.get(name).tags(tags).gauge());
	}

}
