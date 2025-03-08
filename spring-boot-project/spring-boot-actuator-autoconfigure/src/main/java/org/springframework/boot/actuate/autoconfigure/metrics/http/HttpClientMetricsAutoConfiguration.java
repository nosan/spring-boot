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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.httpcomponents.hc5.PoolingHttpClientConnectionManagerMetricsBinder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.util.function.SingletonSupplier;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for HTTP Clients created by
 * {@link ClientHttpRequestFactoryBuilder}.
 *
 * @author Dmytro Nosan
 * @since 3.5.0
 */
@AutoConfiguration(after = { HttpClientAutoConfiguration.class, MetricsAutoConfiguration.class,
		CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnBean({ MeterRegistry.class, ClientHttpRequestFactoryBuilder.class })
public class HttpClientMetricsAutoConfiguration {

	@Bean
	@ConditionalOnClass({ PoolingHttpClientConnectionManager.class,
			PoolingHttpClientConnectionManagerMetricsBinder.class })
	static HttpComponentsClientHttpRequestFactoryBuilderMetricsPostProcessor httpComponentsClientHttpRequestFactoryBuilderMetricsPostProcessor(
			ObjectProvider<MeterRegistry> meterRegistry) {
		return new HttpComponentsClientHttpRequestFactoryBuilderMetricsPostProcessor(meterRegistry);
	}

	static class HttpComponentsClientHttpRequestFactoryBuilderMetricsPostProcessor
			implements BeanPostProcessor, Ordered {

		private final Supplier<MeterRegistry> meterRegistry;

		private final Map<String, Integer> poolCounter = new ConcurrentHashMap<>();

		HttpComponentsClientHttpRequestFactoryBuilderMetricsPostProcessor(ObjectProvider<MeterRegistry> meterRegistry) {
			this.meterRegistry = SingletonSupplier.of(meterRegistry::getObject);
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof HttpComponentsClientHttpRequestFactoryBuilder builder) {
				return builder.withConnectionManagerPostConfigurer(
						(connectionManager) -> bindToMeterRegistry(beanName, connectionManager));
			}
			return bean;
		}

		private void bindToMeterRegistry(String beanName, PoolingHttpClientConnectionManager connectionManager) {
			String name = beanName + ".pool-" + this.poolCounter.compute(beanName, (k, v) -> (v != null) ? v + 1 : 0);
			new PoolingHttpClientConnectionManagerMetricsBinder(connectionManager, name)
				.bindTo(this.meterRegistry.get());
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

	}

}
