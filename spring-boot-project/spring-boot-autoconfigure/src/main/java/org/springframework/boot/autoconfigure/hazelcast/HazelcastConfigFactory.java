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

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.config.XmlConfigLocator;
import com.hazelcast.config.YamlConfigBuilder;
import com.hazelcast.config.YamlConfigLocator;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Factory that can be used to create the Hazelcast {@link Config}.
 *
 * @author Dmytro Nosan
 * @since 2.3.0
 */
public final class HazelcastConfigFactory {

	private static final String YAML = ".yaml";

	private static final String YML = ".yml";

	private final Resource configLocation;

	private final List<HazelcastConfigCustomizer> customizers;

	/**
	 * Create a new {@link HazelcastConfigFactory} instance.
	 * @param configCustomizers any {@link HazelcastConfigFactory customizers} that should
	 * be applied when the {@link Config} is built
	 */
	public HazelcastConfigFactory(HazelcastConfigCustomizer... configCustomizers) {
		Assert.notNull(configCustomizers, "ConfigCustomizers must not be null");
		this.configLocation = null;
		this.customizers = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(configCustomizers)));
	}

	/**
	 * Create a new {@link HazelcastConfigFactory} instance for the specified
	 * configuration location.
	 * @param configLocation the location of the configuration file
	 * @param configCustomizers any {@link HazelcastConfigFactory customizers} that should
	 * be applied when the {@link Config} is built
	 */
	public HazelcastConfigFactory(Resource configLocation, HazelcastConfigCustomizer... configCustomizers) {
		Assert.notNull(configLocation, "ConfigLocation must not be null");
		Assert.notNull(configCustomizers, "ConfigCustomizers must not be null");
		this.configLocation = configLocation;
		this.customizers = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(configCustomizers)));
	}

	/**
	 * Creates a {@link Config} for the specified configuration location (if present),
	 * otherwise the default {@link Config} (if possible).
	 * @throws IOException if the configuration location could not be read
	 * @return a {@link Config}
	 */
	public Config getConfig() throws IOException {
		Config config = (this.configLocation != null) ? loadConfig(this.configLocation) : getDefaultConfig();
		for (HazelcastConfigCustomizer customizer : this.customizers) {
			customizer.customize(config);
		}
		return config;
	}

	private static Config loadConfig(Resource configLocation) throws IOException {
		URL configUrl = configLocation.getURL();
		String configFileName = configUrl.getPath();
		if (configFileName.endsWith(YAML) || configFileName.equals(YML)) {
			return new YamlConfigBuilder(configUrl).build();
		}
		return new XmlConfigBuilder(configUrl).build();
	}

	private static Config getDefaultConfig() {
		XmlConfigLocator xmlConfigLocator = new XmlConfigLocator();
		YamlConfigLocator yamlConfigLocator = new YamlConfigLocator();
		if (yamlConfigLocator.locateFromSystemProperty()) {
			return new YamlConfigBuilder(yamlConfigLocator).build();
		}
		if (xmlConfigLocator.locateFromSystemProperty()) {
			return new XmlConfigBuilder(xmlConfigLocator).build();
		}
		if (xmlConfigLocator.locateInWorkDirOrOnClasspath()) {
			return new XmlConfigBuilder(xmlConfigLocator).build();
		}
		if (yamlConfigLocator.locateInWorkDirOrOnClasspath()) {
			return new YamlConfigBuilder(yamlConfigLocator).build();
		}
		if (xmlConfigLocator.locateDefault()) {
			return new XmlConfigBuilder(xmlConfigLocator).build();
		}
		throw new IllegalStateException("Hazelcast Config does not exist");
	}

}
