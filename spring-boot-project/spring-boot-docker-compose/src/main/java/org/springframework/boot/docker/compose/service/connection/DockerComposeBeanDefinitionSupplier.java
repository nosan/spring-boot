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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.docker.compose.core.ImageReference;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Bean Definition supplier for Docker Compose {@link ConnectionDetails}.
 *
 * @author Dmytro Nosan
 */
final class DockerComposeBeanDefinitionSupplier implements Supplier<BeanDefinitionHolder> {

	private static final String BEAN_NAME_PREFIX_LABEL = "org.springframework.boot.bean.name-prefix";

	private static final String PRIMARY_LABEL = "org.springframework.boot.bean.primary";

	private static final String FALLBACK_LABEL = "org.springframework.boot.bean.fallback";

	private static final String DEFAULT_CANDIDATE_LABEL = "org.springframework.boot.bean.default-candidate";

	private static final String AUTOWIRE_CANDIDATE_LABEL = "org.springframework.boot.bean.autowire-candidate";

	private static final String QUALIFIER_LABEL = "org.springframework.boot.bean.qualifier";

	private final RunningService runningService;

	private final ConnectionDetails connectionDetails;

	private final Class<?> connectionDetailsType;

	/**
	 * Create a {@link DockerComposeBeanDefinitionSupplier}.
	 * @param runningService the source {@link RunningService} is used for additional bean
	 * definition properties.
	 * @param connectionDetails the connection details source is being used as the bean
	 * instance supplier and for determining the bean class.
	 * @param connectionDetailsType the connection details type is being used to create
	 * the bean name.
	 */
	DockerComposeBeanDefinitionSupplier(RunningService runningService, ConnectionDetails connectionDetails,
			Class<?> connectionDetailsType) {
		this.runningService = runningService;
		this.connectionDetails = connectionDetails;
		this.connectionDetailsType = connectionDetailsType;
	}

	@Override
	public BeanDefinitionHolder get() {
		String beanName = getBeanName();
		RootBeanDefinition bd = new RootBeanDefinition();
		addContainerMetadata(bd);
		bd.setBeanClass(this.connectionDetails.getClass());
		bd.setInstanceSupplier(() -> this.connectionDetails);
		PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
		mapper.from(getPrimary()).to(bd::setPrimary);
		mapper.from(getFallback()).to(bd::setFallback);
		mapper.from(getQualifier()).to(bd::addQualifier);
		mapper.from(getAutowireCandidate()).to(bd::setAutowireCandidate);
		mapper.from(getDefaultCandidate()).to(bd::setDefaultCandidate);
		return new BeanDefinitionHolder(bd, beanName);
	}

	private String getBeanName() {
		String beanNamePrefix = getLabel(BEAN_NAME_PREFIX_LABEL);
		List<String> parts = new ArrayList<>();
		if (beanNamePrefix != null) {
			parts.addAll(Arrays.asList(beanNamePrefix.split("-")));
		}
		else {
			parts.addAll(Arrays.asList(this.runningService.name().split("-")));
		}
		parts.add(ClassUtils.getShortNameAsProperty(this.connectionDetailsType));
		return StringUtils.uncapitalize(parts.stream().map(StringUtils::capitalize).collect(Collectors.joining()));
	}

	private void addContainerMetadata(RootBeanDefinition bd) {
		ImageReference image = this.runningService.image();
		ContainerImageMetadata containerMetadata = new ContainerImageMetadata(image.toString());
		containerMetadata.addTo(bd);
	}

	private Boolean getPrimary() {
		return getLabel(PRIMARY_LABEL, Boolean::parseBoolean);
	}

	private Boolean getFallback() {
		return getLabel(FALLBACK_LABEL, Boolean::parseBoolean);
	}

	private AutowireCandidateQualifier getQualifier() {
		return getLabel(QUALIFIER_LABEL, (value) -> new AutowireCandidateQualifier(Qualifier.class, value));
	}

	private Boolean getAutowireCandidate() {
		return getLabel(AUTOWIRE_CANDIDATE_LABEL, Boolean::parseBoolean);
	}

	private Boolean getDefaultCandidate() {
		return getLabel(DEFAULT_CANDIDATE_LABEL, Boolean::parseBoolean);
	}

	private <T> T getLabel(String name, Function<String, T> mapper) {
		String value = getLabel(name);
		return (value != null) ? mapper.apply(value) : null;
	}

	private String getLabel(String name) {
		return this.runningService.labels().get(name);
	}

}
