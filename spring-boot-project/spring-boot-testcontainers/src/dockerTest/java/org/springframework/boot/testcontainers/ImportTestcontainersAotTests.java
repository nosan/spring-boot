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

package org.springframework.boot.testcontainers;

import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testsupport.container.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.ClassName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aot Tests for {@link ImportTestcontainers}.
 *
 * @author Dmytro Nosan
 */
@DisabledIfDockerUnavailable
@CompileWithForkedClassLoader
class ImportTestcontainersAotTests {

	private final AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

	private final TestGenerationContext generationContext = new TestGenerationContext();

	@AfterEach
	void close() {
		this.applicationContext.close();
	}

	@Test
	void importTestcontainersImportWithoutValue() {
		register(ImportTestcontainersWithoutValue.class);
		compile((context) -> {
			assertThat(context.getBean(PostgreSQLContainer.class))
				.isSameAs(ImportTestcontainersWithoutValue.postgresql);
			assertThat(context.getBean(MongoDBContainer.class)).isSameAs(ImportTestcontainersWithoutValue.mongodb);
			ConfigurableEnvironment environment = context.getEnvironment();
			assertThat(environment.getProperty("mongodb.port", Integer.class))
				.isEqualTo(ImportTestcontainersWithoutValue.mongodb.getFirstMappedPort());
			assertThat(environment.getProperty("postgresql.port", Integer.class))
				.isEqualTo(ImportTestcontainersWithoutValue.postgresql.getFirstMappedPort());
		});
	}

	@Test
	void importTestcontainersImportWithValue() {
		register(ImportTestcontainersWithValue.class);
		compile((context) -> {
			assertThat(context.getBean(PostgreSQLContainer.class)).isSameAs(PostgresqlContainers.postgresql);
			assertThat(context.getBean(MongoDBContainer.class)).isSameAs(MongodbContainers.mongodb);
			ConfigurableEnvironment environment = context.getEnvironment();
			assertThat(environment.getProperty("mongodb.port", Integer.class))
				.isEqualTo(MongodbContainers.mongodb.getFirstMappedPort());
			assertThat(environment.getProperty("postgresql.port", Integer.class))
				.isEqualTo(PostgresqlContainers.postgresql.getFirstMappedPort());
		});
	}

	@SuppressWarnings("unchecked")
	private void compile(Consumer<GenericApplicationContext> result) {
		ClassName className = processAheadOfTime();
		TestCompiler.forSystem().with(this.generationContext).printFiles(System.out).compile((compiled) -> {
			try (GenericApplicationContext context = new GenericApplicationContext()) {
				ApplicationContextInitializer<GenericApplicationContext> initializer = compiled
					.getInstance(ApplicationContextInitializer.class, className.toString());
				initializer.initialize(context);
				context.refresh();
				result.accept(context);
			}
		});
	}

	private void register(Class<?>... classes) {
		this.applicationContext.register(classes);
	}

	private ClassName processAheadOfTime() {
		ClassName className = new ApplicationContextAotGenerator().processAheadOfTime(this.applicationContext,
				this.generationContext);
		this.generationContext.writeGeneratedContent();
		return className;
	}

	@ImportTestcontainers
	static class ImportTestcontainersWithoutValue {

		static final PostgreSQLContainer<?> postgresql = TestImage.container(PostgreSQLContainer.class);

		static final MongoDBContainer mongodb = TestImage.container(MongoDBContainer.class);

		@DynamicPropertySource
		static void postgresqlPort(DynamicPropertyRegistry registry) {
			registry.add("postgresql.port", postgresql::getFirstMappedPort);
		}

		@DynamicPropertySource
		static void mongodbPort(DynamicPropertyRegistry registry) {
			registry.add("mongodb.port", mongodb::getFirstMappedPort);
		}

	}

	@ImportTestcontainers({ PostgresqlContainers.class, MongodbContainers.class })
	static class ImportTestcontainersWithValue {

	}

	static class MongodbContainers {

		private static final MongoDBContainer mongodb = TestImage.container(MongoDBContainer.class);

		@DynamicPropertySource
		private static void mongodbPort(DynamicPropertyRegistry registry) {
			registry.add("mongodb.port", mongodb::getFirstMappedPort);
		}

	}

	static class PostgresqlContainers {

		private static final PostgreSQLContainer<?> postgresql = TestImage.container(PostgreSQLContainer.class);

		@DynamicPropertySource
		private static void postgresqlPort(DynamicPropertyRegistry registry) {
			registry.add("postgresql.port", postgresql::getFirstMappedPort);
		}

	}

}
