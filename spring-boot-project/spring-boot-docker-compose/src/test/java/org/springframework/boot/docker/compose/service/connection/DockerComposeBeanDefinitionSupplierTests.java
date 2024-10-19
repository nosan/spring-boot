/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.docker.compose.core.ImageReference;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.util.function.SupplierUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DockerComposeBeanDefinitionSupplier}.
 *
 * @author Dmytro Nosan
 */
class DockerComposeBeanDefinitionSupplierTests {

	private final RunningService runningService = mock(RunningService.class);

	private final Map<String, String> labels = new LinkedHashMap<>();

	private final ConnectionDetails connectionDetails = new TestConnectionDetails();

	DockerComposeBeanDefinitionSupplierTests() {
		given(this.runningService.name()).willReturn("t-redis-1");
		given(this.runningService.image()).willReturn(ImageReference.of("redis:latest"));
		given(this.runningService.labels()).willReturn(this.labels);
	}

	@Test
	void shouldCreateBeanDefinitionWithBeanNameBasedOnContainerNameAndConnectionDetailsType() {
		BeanDefinitionHolder beanDefinitionHolder = createBeanDefinitionHolder();
		assertCommonProperties(beanDefinitionHolder.getBeanDefinition());
		assertThat(beanDefinitionHolder.getBeanName()).isEqualTo("tRedis1ConnectionDetails");
	}

	@Test
	void shouldCreateBeanDefinitionWithBeanNameBasedOnBeanPrefixNameAndConnectionDetailsType() {
		this.labels.put("org.springframework.boot.bean.name-prefix", "redis-container");
		BeanDefinitionHolder beanDefinitionHolder = createBeanDefinitionHolder();
		assertCommonProperties(beanDefinitionHolder.getBeanDefinition());
		assertThat(beanDefinitionHolder.getBeanName()).isEqualTo("redisContainerConnectionDetails");
	}

	@Test
	void shouldCreateBeanDefinitionWithPrimary() {
		this.labels.put("org.springframework.boot.bean.primary", "true");
		BeanDefinition beanDefinition = createBeanDefinition();
		assertCommonProperties(beanDefinition);
		assertThat(beanDefinition.isPrimary()).isTrue();
	}

	@Test
	void shouldCreateBeanDefinitionWithFallback() {
		this.labels.put("org.springframework.boot.bean.fallback", "true");
		BeanDefinition beanDefinition = createBeanDefinition();
		assertCommonProperties(beanDefinition);
		assertThat(beanDefinition.isFallback()).isTrue();

	}

	@Test
	void shouldCreateBeanDefinitionWithQualifier() {
		this.labels.put("org.springframework.boot.bean.qualifier", "test");
		BeanDefinition beanDefinition = createBeanDefinition();
		assertCommonProperties(beanDefinition);
		RootBeanDefinition rootBeanDefinition = (RootBeanDefinition) beanDefinition;
		AutowireCandidateQualifier qualifier = rootBeanDefinition.getQualifier(Qualifier.class.getTypeName());
		assertThat(qualifier).isNotNull();
		assertThat(qualifier.getAttribute(AutowireCandidateQualifier.VALUE_KEY)).isEqualTo("test");
	}

	@Test
	void shouldCreateBeanDefinitionWithAutowireCandidate() {
		this.labels.put("org.springframework.boot.bean.autowire-candidate", "true");
		BeanDefinition beanDefinition = createBeanDefinition();
		assertCommonProperties(beanDefinition);
		assertThat(beanDefinition.isAutowireCandidate()).isTrue();
	}

	@Test
	void shouldCreateBeanDefinitionWithDefaultCandidate() {
		this.labels.put("org.springframework.boot.bean.default-candidate", "true");
		BeanDefinition beanDefinition = createBeanDefinition();
		assertCommonProperties(beanDefinition);
		RootBeanDefinition rootBeanDefinition = (RootBeanDefinition) beanDefinition;
		assertThat(rootBeanDefinition.isDefaultCandidate()).isTrue();
	}

	@Test
	void shouldCreateBeanDefinitionWithDefaults() {
		BeanDefinitionHolder beanDefinitionHolder = createBeanDefinitionHolder();
		BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();
		assertCommonProperties(beanDefinition);
		RootBeanDefinition rootBeanDefinition = (RootBeanDefinition) beanDefinition;
		assertThat(beanDefinitionHolder.getBeanName()).isEqualTo("tRedis1ConnectionDetails");
		assertThat(rootBeanDefinition.isPrimary()).isFalse();
		assertThat(rootBeanDefinition.isFallback()).isFalse();
		assertThat(rootBeanDefinition.isAutowireCandidate()).isTrue();
		assertThat(rootBeanDefinition.isDefaultCandidate()).isTrue();
		assertThat(rootBeanDefinition.getQualifiers()).isNullOrEmpty();
	}

	private void assertCommonProperties(BeanDefinition beanDefinition) {
		assertThat(beanDefinition).isInstanceOf(RootBeanDefinition.class);
		RootBeanDefinition rootBeanDefinition = (RootBeanDefinition) beanDefinition;
		assertThat(rootBeanDefinition.getBeanClass()).isEqualTo(TestConnectionDetails.class);
		assertThat(SupplierUtils.resolve(rootBeanDefinition.getInstanceSupplier())).isEqualTo(this.connectionDetails);
		assertThat(ContainerImageMetadata.isPresent(beanDefinition)).isTrue();
		assertThat(ContainerImageMetadata.getFrom(beanDefinition).imageName()).endsWith("redis:latest");
	}

	private BeanDefinition createBeanDefinition() {
		return createBeanDefinitionHolder().getBeanDefinition();
	}

	private BeanDefinitionHolder createBeanDefinitionHolder() {
		DockerComposeBeanDefinitionSupplier supplier = new DockerComposeBeanDefinitionSupplier(this.runningService,
				this.connectionDetails, ConnectionDetails.class);
		return supplier.get();
	}

	private static final class TestConnectionDetails implements ConnectionDetails {

	}

}
