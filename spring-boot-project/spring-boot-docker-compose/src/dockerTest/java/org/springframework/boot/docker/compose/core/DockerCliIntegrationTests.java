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

package org.springframework.boot.docker.compose.core;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeConfig;
import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeDown;
import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposePs;
import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeStart;
import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeStop;
import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeUp;
import org.springframework.boot.docker.compose.core.DockerCliCommand.Inspect;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.testsupport.container.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.boot.testsupport.process.DisabledIfProcessUnavailable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerCli}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Dmytro Nosan
 */
@DisabledIfDockerUnavailable
@DisabledIfProcessUnavailable({ "docker", "compose" })
class DockerCliIntegrationTests {

	@TempDir
	private static Path tempDir;

	@Test
	void runBasicCommand() {
		DockerCli cli = new DockerCli(null, null, Collections.emptySet());
		List<DockerCliContextResponse> context = cli.run(new DockerCliCommand.Context());
		assertThat(context).isNotEmpty();
	}

	@Test
	void runLifecycle() throws IOException {
		File composeFile = createComposeFile("redis-compose.yaml");
		DockerCli cli = new DockerCli(null, DockerComposeFile.of(composeFile), Collections.emptySet());
		try {
			// Verify that no services are running (this is a fresh compose project)
			List<DockerCliComposePsResponse> ps = cli.run(new ComposePs());
			assertThat(ps).isEmpty();
			// List the config and verify that redis is there
			DockerCliComposeConfigResponse config = cli.run(new ComposeConfig());
			assertThat(config.services()).containsOnlyKeys("redis");
			// Run up
			cli.run(new ComposeUp(LogLevel.INFO, Collections.emptyList()));
			// Run ps and use id to run inspect on the id
			ps = cli.run(new ComposePs());
			assertThat(ps).hasSize(1);
			String id = ps.get(0).id();
			List<DockerCliInspectResponse> inspect = cli.run(new Inspect(List.of(id)));
			assertThat(inspect).isNotEmpty();
			assertThat(inspect.get(0).id()).startsWith(id);
			// Run stop, then run ps and verify the services are stopped
			cli.run(new ComposeStop(Duration.ofSeconds(10), Collections.emptyList()));
			ps = cli.run(new ComposePs());
			assertThat(ps).isEmpty();
			// Run start, verify service is there, then run down and verify they are gone
			cli.run(new ComposeStart(LogLevel.INFO, Collections.emptyList()));
			ps = cli.run(new ComposePs());
			assertThat(ps).hasSize(1);
			cli.run(new ComposeDown(Duration.ofSeconds(10), Collections.emptyList()));
			ps = cli.run(new ComposePs());
			assertThat(ps).isEmpty();
		}
		finally {
			// Clean up in any case
			quietComposeDown(cli);
		}
	}

	@Test
	void runLifecycleWithSpecifiedServices() throws IOException {
		File composeFile = createComposeFile("multiple-redis-compose.yaml");
		Set<String> services = Set.of("redis", "redis3");
		DockerCli cli = new DockerCli(null, DockerComposeFile.of(composeFile), Collections.emptySet());
		try {
			// Verify that no services are running (this is a fresh compose project)
			List<DockerCliComposePsResponse> ps = cli.run(new ComposePs(Collections.emptySet()));
			assertThat(ps).isEmpty();
			// List the config and verify that only redis, redis3 are there.
			DockerCliComposeConfigResponse config = cli.run(new ComposeConfig(services));
			assertThat(config.services()).containsOnlyKeys("redis", "redis3");
			// List the config without services should return all redis services.
			config = cli.run(new ComposeConfig());
			assertThat(config.services()).containsOnlyKeys("redis", "redis1", "redis2", "redis3");

			// Run up redis, redis3
			cli.run(new ComposeUp(LogLevel.INFO, Collections.emptyList(), services));
			// Run ps with specified services and use IDs to run inspect.
			ps = cli.run(new ComposePs(services));
			assertThat(ps).hasSize(2);
			List<String> ids = ps.stream().map(DockerCliComposePsResponse::id).toList();
			List<DockerCliInspectResponse> inspect = cli.run(new Inspect(ids));
			assertThat(inspect).hasSize(2);
			assertThat(inspect).allMatch(response -> ids.stream().anyMatch(id -> response.id().startsWith(id)));

			// Run ps should also return only two services.
			ps = cli.run(new ComposePs());
			assertThat(ps).hasSize(2);

			// Run stop on redis, then run ps and verify that only one service stopped.
			cli.run(new ComposeStop(Duration.ofSeconds(10), Collections.emptyList(), Set.of("redis")));
			ps = cli.run(new ComposePs());
			assertThat(ps).hasSize(1);
			// Run stop on redis3, then run ps and verify that nothing is left.
			cli.run(new ComposeStop(Duration.ofSeconds(10), Collections.emptyList(), Set.of("redis3")));
			ps = cli.run(new ComposePs());
			assertThat(ps).isEmpty();
			// Run start, verify services are there, then run down and verify they are
			// gone
			cli.run(new ComposeStart(LogLevel.INFO, Collections.emptyList(), services));
			ps = cli.run(new ComposePs(services));
			assertThat(ps).hasSize(2);
			// Run ps should also return only two services.
			ps = cli.run(new ComposePs());
			assertThat(ps).hasSize(2);
			// Run down on redis, then run ps and verify that only one service is down.
			cli.run(new ComposeDown(Duration.ofSeconds(10), Collections.emptyList(), Set.of("redis")));
			ps = cli.run(new ComposePs());
			assertThat(ps).hasSize(1);
			// Run down on redis3, then run ps and verify that verify they are gone
			cli.run(new ComposeDown(Duration.ofSeconds(10), Collections.emptyList(), Set.of("redis3")));
			ps = cli.run(new ComposePs());
			assertThat(ps).isEmpty();
		}
		finally {
			// Clean up in any case
			quietComposeDown(cli);
		}
	}

	@Test
	void shouldWorkWithMultipleComposeFiles() throws IOException {
		List<File> composeFiles = createComposeFiles();
		DockerCli cli = new DockerCli(null, DockerComposeFile.of(composeFiles), Collections.emptySet());
		try {
			// List the config and verify that both redis are there
			DockerCliComposeConfigResponse config = cli.run(new ComposeConfig());
			assertThat(config.services()).containsOnlyKeys("redis1", "redis2");
			// Run up
			cli.run(new ComposeUp(LogLevel.INFO, Collections.emptyList()));
			// Run ps and use id to run inspect on the id
			List<DockerCliComposePsResponse> ps = cli.run(new ComposePs());
			assertThat(ps).hasSize(2);
		}
		finally {
			// Clean up in any case
			quietComposeDown(cli);
		}
	}

	@Test
	void shouldWithMultipleComposeFilesWithSpecifiedServices() throws IOException {
		List<File> composeFiles = createComposeFiles();
		Set<String> services = Set.of("redis2");
		DockerCli cli = new DockerCli(null, DockerComposeFile.of(composeFiles), Collections.emptySet());
		try {
			// List the config and verify that only redis2 is returned.
			DockerCliComposeConfigResponse config = cli.run(new ComposeConfig(services));
			assertThat(config.services()).containsOnlyKeys("redis2");
			// List the config and verify that both redis are there
			config = cli.run(new ComposeConfig(Collections.emptySet()));
			assertThat(config.services()).containsOnlyKeys("redis1", "redis2");
			// Run up
			cli.run(new ComposeUp(LogLevel.INFO, Collections.emptyList(), services));
			// Run ps and use id to run inspect on the id
			List<DockerCliComposePsResponse> ps = cli.run(new ComposePs(services));
			assertThat(ps).hasSize(1);
			// Run ps should return also one service.
			ps = cli.run(new ComposePs());
			assertThat(ps).hasSize(1);
		}
		finally {
			// Clean up in any case
			quietComposeDown(cli);
		}
	}

	private static void quietComposeDown(DockerCli cli) {
		try {
			cli.run(new ComposeDown(Duration.ZERO, Collections.emptyList(), Collections.emptySet()));
		}
		catch (RuntimeException ex) {
			// Ignore
		}
	}

	private static File createComposeFile(String resource) throws IOException {
		File source = new ClassPathResource(resource, DockerCliIntegrationTests.class).getFile();
		File target = Path.of(tempDir.toString(), source.getName()).toFile();
		String content = FileCopyUtils.copyToString(new FileReader(source));
		content = content.replace("{imageName}", TestImage.REDIS.toString());
		try (FileWriter writer = new FileWriter(target)) {
			FileCopyUtils.copy(content, writer);
		}
		return target;
	}

	private static List<File> createComposeFiles() throws IOException {
		File file1 = createComposeFile("1.yaml");
		File file2 = createComposeFile("2.yaml");
		return List.of(file1, file2);
	}

}
