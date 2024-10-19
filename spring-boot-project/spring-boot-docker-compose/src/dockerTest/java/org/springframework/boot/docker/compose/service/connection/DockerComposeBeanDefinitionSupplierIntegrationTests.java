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

import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link DockerComposeBeanDefinitionSupplier}.
 *
 * @author Dmytro Nosan
 */
class DockerComposeBeanDefinitionSupplierIntegrationTests {

	@DockerComposeTest(composeFile = "bean-primary.yaml", image = TestImage.REDIS)
	void primaryConnectionDetails(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		containsRedisBeans(beanFactory);
		AutowireByType autowireByType = createBean(beanFactory, AutowireByType.class);
		assertThat(autowireByType.connectionDetails).isEqualTo(beanFactory.getBean("redis1RedisConnectionDetails"));
	}

	@DockerComposeTest(composeFile = "bean-fallback.yaml", image = TestImage.REDIS)
	void fallbackConnectionDetails(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		containsRedisBeans(beanFactory);
		AutowireByType autowireByType = createBean(beanFactory, AutowireByType.class);
		assertThat(autowireByType.connectionDetails).isEqualTo(beanFactory.getBean("redisRedisConnectionDetails"));
	}

	@DockerComposeTest(composeFile = "bean-autowire-candidate.yaml", image = TestImage.REDIS)
	void autowireCandidateConnectionDetails(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		containsRedisBeans(beanFactory);
		AutowireByType autowireByType = createBean(beanFactory, AutowireByType.class);
		assertThat(autowireByType.connectionDetails).isEqualTo(beanFactory.getBean("redisRedisConnectionDetails"));
	}

	@DockerComposeTest(composeFile = "bean-autowire-candidate-qualifier.yaml", image = TestImage.REDIS)
	void autowireCandidateAndQualifierConnectionDetails(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		containsRedisBeans(beanFactory);
		AutowireByType autowireByType = createBean(beanFactory, AutowireByType.class);
		assertThat(autowireByType.connectionDetails).isEqualTo(beanFactory.getBean("redisRedisConnectionDetails"));
		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
			.isThrownBy(() -> createBean(beanFactory, AutowireByQualifier.class));
	}

	@DockerComposeTest(composeFile = "bean-default-candidate.yaml", image = TestImage.REDIS)
	void defaultCandidateConnectionDetails(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		containsRedisBeans(beanFactory);
		AutowireByType autowireByType = createBean(beanFactory, AutowireByType.class);
		assertThat(autowireByType.connectionDetails).isEqualTo(beanFactory.getBean("redisRedisConnectionDetails"));
	}

	@DockerComposeTest(composeFile = "bean-default-candidate-qualifier.yaml", image = TestImage.REDIS)
	void defaultCandidateAndQualifierConnectionDetails(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		containsRedisBeans(beanFactory);
		AutowireByType autowireByType = createBean(beanFactory, AutowireByType.class);
		AutowireByQualifier autowireByQualifier = createBean(beanFactory, AutowireByQualifier.class);
		assertThat(autowireByType.connectionDetails).isEqualTo(beanFactory.getBean("redisRedisConnectionDetails"));
		assertThat(autowireByQualifier.connectionDetails)
			.isEqualTo(beanFactory.getBean("redis1RedisConnectionDetails"));
	}

	@DockerComposeTest(composeFile = "bean-qualifier.yaml", image = TestImage.REDIS)
	void qualifierConnectionDetails(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		containsRedisBeans(beanFactory);
		AutowireByQualifier autowireByQualifier = createBean(beanFactory, AutowireByQualifier.class);
		assertThat(autowireByQualifier.connectionDetails).isEqualTo(beanFactory.getBean("redisRedisConnectionDetails"));
	}

	@DockerComposeTest(composeFile = "bean-name.yaml", image = TestImage.REDIS)
	void namedConnectionDetails(ConfigurableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		containsRedisBeans(beanFactory);
		AutowireByName autowireByName = createBean(beanFactory, AutowireByName.class);
		assertThat(autowireByName.redisRedisConnectionDetails)
			.isEqualTo(beanFactory.getBean("redisRedisConnectionDetails"));
		assertThat(autowireByName.redis1RedisConnectionDetails)
			.isEqualTo(beanFactory.getBean("redis1RedisConnectionDetails"));
	}

	private void containsRedisBeans(ConfigurableListableBeanFactory beanFactory) {
		assertThat(beanFactory.getBeansOfType(RedisConnectionDetails.class)).hasSize(2);
		assertThat(beanFactory.containsBean("redisRedisConnectionDetails")).isTrue();
		assertThat(beanFactory.containsBean("redis1RedisConnectionDetails")).isTrue();
	}

	private <T> T createBean(ConfigurableListableBeanFactory beanFactory, Class<T> clazz) {
		return beanFactory.createBean(clazz);
	}

	static class AutowireByType {

		private final RedisConnectionDetails connectionDetails;

		AutowireByType(RedisConnectionDetails connectionDetails) {
			this.connectionDetails = connectionDetails;
		}

	}

	static class AutowireByQualifier {

		private final RedisConnectionDetails connectionDetails;

		AutowireByQualifier(@Qualifier("test") RedisConnectionDetails connectionDetails) {
			this.connectionDetails = connectionDetails;
		}

	}

	static class AutowireByName {

		private final RedisConnectionDetails redisRedisConnectionDetails;

		private final RedisConnectionDetails redis1RedisConnectionDetails;

		AutowireByName(RedisConnectionDetails redisRedisConnectionDetails,
				RedisConnectionDetails redis1RedisConnectionDetails) {
			this.redisRedisConnectionDetails = redisRedisConnectionDetails;
			this.redis1RedisConnectionDetails = redis1RedisConnectionDetails;
		}

	}

}
