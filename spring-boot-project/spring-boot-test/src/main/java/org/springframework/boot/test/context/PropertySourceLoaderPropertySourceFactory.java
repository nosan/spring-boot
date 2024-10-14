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

package org.springframework.boot.test.context;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.StringUtils;

/**
 * An implementation of {@link PropertySourceFactory} that delegates the loading of
 * {@code PropertySource} to {@link PropertySourceLoader}. If the provided .yaml or
 * .properties file contains multiple documents (separated by either {@code ---} or
 * {@code #---}), the property sources from later documents will take precedence,
 * overriding any conflicting values defined in earlier documents.
 *
 * @author Dmytro Nosan
 * @since 3.4.0
 */
public class PropertySourceLoaderPropertySourceFactory implements PropertySourceFactory {

	private final List<PropertySourceLoader> propertySourceLoaders;

	private final PropertySourceFactory fallbackPropertySourceFactory = new DefaultPropertySourceFactory();

	PropertySourceLoaderPropertySourceFactory() {
		this.propertySourceLoaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class,
				getClass().getClassLoader());
	}

	@Override
	public PropertySource<?> createPropertySource(String name, EncodedResource encodedResource) throws IOException {
		Resource resource = encodedResource.getResource();
		String propertySourceName = getPropertySourceName(name, resource);
		for (PropertySourceLoader loader : this.propertySourceLoaders) {
			if (isLoadable(loader, resource)) {
				return createPropertySource(loader, propertySourceName, encodedResource);
			}
		}
		return this.fallbackPropertySourceFactory.createPropertySource(propertySourceName, encodedResource);
	}

	private static PropertySource<?> createPropertySource(PropertySourceLoader propertySourceLoader, String name,
			EncodedResource encodedResource) throws IOException {
		Resource resource = encodedResource.getResource();
		CompositePropertySource compositePropertySource = new CompositePropertySource(name);
		List<PropertySource<?>> propertySources = propertySourceLoader.load(name, resource);
		propertySources.forEach(compositePropertySource::addFirstPropertySource);
		return compositePropertySource;
	}

	private static boolean isLoadable(PropertySourceLoader loader, Resource resource) {
		String extension = StringUtils.getFilenameExtension(resource.getFilename());
		if (!StringUtils.hasText(extension)) {
			return false;
		}
		for (String fileExtension : loader.getFileExtensions()) {
			if (fileExtension.equalsIgnoreCase(extension)) {
				return true;
			}
		}
		return false;
	}

	private static String getPropertySourceName(String name, Resource resource) {
		if (StringUtils.hasText(name)) {
			return name;
		}
		String description = resource.getDescription();
		if (StringUtils.hasText(description)) {
			return description;
		}
		return resource.getClass().getSimpleName() + "@" + System.identityHashCode(resource);
	}

}
