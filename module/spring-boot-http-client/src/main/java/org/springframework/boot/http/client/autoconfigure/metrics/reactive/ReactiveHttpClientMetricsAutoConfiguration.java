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

package org.springframework.boot.http.client.autoconfigure.metrics.reactive;

import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.observation.MetricConfig;
import org.apache.hc.client5.http.observation.binder.ConnPoolMetersAsync;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.http.client.autoconfigure.reactive.ClientHttpConnectorBuilderCustomizer;
import org.springframework.boot.http.client.autoconfigure.reactive.ReactiveHttpClientAutoConfiguration;
import org.springframework.boot.http.client.reactive.HttpComponentsClientHttpConnectorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics related to reactive HTTP
 * clients.
 *
 * @author Dmytro Nosan
 * @since 4.2.0
 */
@AutoConfiguration(
		afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
		before = ReactiveHttpClientAutoConfiguration.class)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
public final class ReactiveHttpClientMetricsAutoConfiguration {

	@ConditionalOnClass({ HttpAsyncClientBuilder.class, ConnPoolMetersAsync.class })
	@Configuration(proxyBeanMethods = false)
	static class HttpComponentsClientHttpConnectorMetricsConfiguration {

		private final AtomicInteger id = new AtomicInteger();

		@Bean
		@Order(0)
		ClientHttpConnectorBuilderCustomizer<HttpComponentsClientHttpConnectorBuilder> httpComponentsClientHttpConnectorConnPoolCustomizer(
				MeterRegistry meterRegistry) {
			return (builder) -> builder.withHttpClientCustomizer(
					(httpClientBuilder) -> ConnPoolMetersAsync.bindTo(httpClientBuilder, meterRegistry,
							MetricConfig.builder()
								.addCommonTag("client.type", "reactive")
								.addCommonTag("client.name", "http-components")
								.addCommonTag("client.id", String.valueOf(this.id.getAndIncrement()))
								.build()));

		}

	}

}
