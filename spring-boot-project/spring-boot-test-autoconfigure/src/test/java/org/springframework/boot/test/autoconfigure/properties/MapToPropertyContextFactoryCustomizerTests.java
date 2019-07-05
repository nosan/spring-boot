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

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MapToPropertyContextFactoryCustomizer}.
 *
 * @author Dmytro Nosan
 */
class MapToPropertyContextFactoryCustomizerTests {

	private final MapToPropertyContextFactoryCustomizer factory = new MapToPropertyContextFactoryCustomizer();

	@Test
	void shouldCreateContextCustomizer() {
		ContextCustomizer contextCustomizer = this.factory.createContextCustomizer(ExampleStaticFinal.class,
				Collections.emptyList());
		assertThat(contextCustomizer).isNotNull();
	}

	@Test
	void shouldNotCreateContextCustomizerNoFields() {
		ContextCustomizer contextCustomizer = this.factory.createContextCustomizer(ExampleEmpty.class,
				Collections.emptyList());
		assertThat(contextCustomizer).isNull();
	}

	@Test
	void shouldFailCreateContextCustomizeNonStaticField() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.factory.createContextCustomizer(ExampleNonstatic.class, Collections.emptyList()))
				.withMessageContaining("@MapToProperty cannot be used with non-static fields");
	}

	@Test
	void shouldFailCreateContextCustomizeNonFinalField() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.factory.createContextCustomizer(ExampleStatic.class, Collections.emptyList()))
				.withMessageContaining("@MapToProperty cannot be used with non-final fields");
	}

	@Test
	void shouldFailCreateContextCustomizeFieldHasObjectType() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.factory.createContextCustomizer(ExampleObject.class, Collections.emptyList()))
				.withMessageContaining("@MapToProperty cannot be used with [class java.lang.Object]");
	}

	static class ExampleEmpty {

	}

	static class ExampleStaticFinal {

		@MapToProperty
		private static final String value = "";

	}

	static class ExampleStatic {

		@MapToProperty
		private static String value = "";

	}

	static class ExampleNonstatic {

		@MapToProperty
		private final String value = "";

	}

	static class ExampleObject {

		@MapToProperty
		private static final Object value = "";

	}

}
