/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.properties;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MapToProperty} with {@link ExtendWith}.
 *
 * @author Dmytro Nosan
 */
@ExtendWith(SpringExtension.class)
class MapToPropertyExtensionTests {

	@RegisterExtension
	public static final ExampleExtension extension = new ExampleExtension();

	@MapToProperty("class-name")
	private static final Supplier<String> className = extension::getClassName;

	@Autowired
	private Environment environment;

	@Test
	void hasProperty() {
		assertThat(this.environment.getProperty("class-name")).isEqualTo(MapToPropertyExtensionTests.class.getName());
	}

	public static final class ExampleExtension implements BeforeAllCallback {

		private ExtensionContext context;

		@Override
		public void beforeAll(ExtensionContext context) throws Exception {
			this.context = context;
		}

		String getClassName() {
			Assert.state(this.context != null, "Context must not be null");
			return this.context.getRequiredTestClass().getName();

		}

	}

}
