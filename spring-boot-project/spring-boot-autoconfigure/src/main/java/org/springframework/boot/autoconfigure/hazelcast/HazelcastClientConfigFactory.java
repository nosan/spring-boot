/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.client.config.XmlClientConfigLocator;
import com.hazelcast.client.config.YamlClientConfigBuilder;
import com.hazelcast.client.config.YamlClientConfigLocator;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Factory that can be used to create the Hazelcast {@link ClientConfig}.
 *
 * @author Dmytro Nosan
 * @since 2.3.0
 */
public final class HazelcastClientConfigFactory {

	private static final String YAML = ".yaml";

	private static final String YML = ".yml";

	private final Resource configLocation;

	private final List<HazelcastClientConfigCustomizer> customizers;

	/**
	 * Create a new {@link HazelcastClientConfigFactory} instance.
	 * @param configCustomizers any {@link HazelcastClientConfigFactory customizers} that
	 * should be applied when the {@link ClientConfig} is built
	 */
	public HazelcastClientConfigFactory(HazelcastClientConfigCustomizer... configCustomizers) {
		Assert.notNull(configCustomizers, "ConfigCustomizers must not be null");
		this.configLocation = null;
		this.customizers = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(configCustomizers)));
	}

	/**
	 * Create a new {@link HazelcastClientConfigFactory} instance for the specified
	 * configuration location.
	 * @param configLocation the location of the configuration file
	 * @param configCustomizers any {@link HazelcastClientConfigFactory customizers} that
	 * should be applied when the {@link ClientConfig} is built
	 */
	public HazelcastClientConfigFactory(Resource configLocation, HazelcastClientConfigCustomizer... configCustomizers) {
		Assert.notNull(configLocation, "ConfigLocation must not be null");
		Assert.notNull(configCustomizers, "ConfigCustomizers must not be null");
		this.configLocation = configLocation;
		this.customizers = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(configCustomizers)));
	}

	/**
	 * Creates a {@link ClientConfig} for the specified configuration location (if
	 * present), otherwise the default {@link ClientConfig} (if possible).
	 * @throws IOException if the configuration location could not be read
	 * @return a {@link ClientConfig}
	 */
	public ClientConfig getClientConfig() throws IOException {
		ClientConfig config = (this.configLocation != null) ? loadConfig(this.configLocation) : getDefaultConfig();
		for (HazelcastClientConfigCustomizer customizer : this.customizers) {
			customizer.customize(config);
		}
		return config;
	}

	private static ClientConfig loadConfig(Resource configLocation) throws IOException {
		URL configUrl = configLocation.getURL();
		String configFileName = configUrl.getPath();
		if (configFileName.endsWith(YAML) || configFileName.equals(YML)) {
			return new YamlClientConfigBuilder(configUrl).build();
		}
		return new XmlClientConfigBuilder(configUrl).build();
	}

	private static ClientConfig getDefaultConfig() {
		XmlClientConfigLocator xmlConfigLocator = new XmlClientConfigLocator();
		YamlClientConfigLocator yamlConfigLocator = new YamlClientConfigLocator();
		if (yamlConfigLocator.locateFromSystemProperty()) {
			return new YamlClientConfigBuilder(yamlConfigLocator).build();
		}
		if (xmlConfigLocator.locateFromSystemProperty()) {
			return new XmlClientConfigBuilder(xmlConfigLocator).build();
		}
		if (xmlConfigLocator.locateInWorkDirOrOnClasspath()) {
			return new XmlClientConfigBuilder(xmlConfigLocator).build();
		}
		if (yamlConfigLocator.locateInWorkDirOrOnClasspath()) {
			return new YamlClientConfigBuilder(yamlConfigLocator).build();
		}
		if (xmlConfigLocator.locateDefault()) {
			return new XmlClientConfigBuilder(xmlConfigLocator).build();
		}
		throw new IllegalStateException("Hazelcast ClientConfig does not exist");
	}

}
