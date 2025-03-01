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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenTelemetry.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 */
@AutoConfiguration
@ConditionalOnClass(OpenTelemetrySdk.class)
@EnableConfigurationProperties(OpenTelemetryProperties.class)
public class OpenTelemetryAutoConfiguration {

	/**
	 * Default value for application name if {@code spring.application.name} is not set.
	 */
	private static final String DEFAULT_APPLICATION_NAME = "unknown_service";

	@Bean
	@ConditionalOnMissingBean(OpenTelemetry.class)
	OpenTelemetrySdk openTelemetry(ObjectProvider<SdkTracerProvider> tracerProvider,
			ObjectProvider<ContextPropagators> propagators, ObjectProvider<SdkLoggerProvider> loggerProvider,
			ObjectProvider<SdkMeterProvider> meterProvider) {
		OpenTelemetrySdkBuilder builder = OpenTelemetrySdk.builder();
		tracerProvider.ifAvailable(builder::setTracerProvider);
		propagators.ifAvailable(builder::setPropagators);
		loggerProvider.ifAvailable(builder::setLoggerProvider);
		meterProvider.ifAvailable(builder::setMeterProvider);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	Resource openTelemetryResource(Environment environment, OpenTelemetryProperties properties) {
		Resource resource = Resource.getDefault();
		return resource.merge(toResource(environment, properties));
	}

	private Resource toResource(Environment environment, OpenTelemetryProperties properties) {
		ResourceBuilder builder = Resource.builder();
		OpenTelemetryResourceAttributes attributes = OpenTelemetryResourceAttributes.fromEnv();
		attributes.putAll(properties.getResourceAttributes());
		attributes.putIfAbsent("service.name", () -> getApplicationName(environment));
		attributes.putIfAbsent("service.group", () -> getApplicationGroup(environment));
		attributes.forEach(builder::put);
		return builder.build();
	}

	private String getApplicationName(Environment environment) {
		return environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
	}

	private String getApplicationGroup(Environment environment) {
		String applicationGroup = environment.getProperty("spring.application.group");
		return (StringUtils.hasLength(applicationGroup)) ? applicationGroup : null;
	}

}
