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

package org.springframework.boot.json;

/**
 * Default implementation of {@link JsonWriter}.
 *
 * @param <T> the type of the objects that this writer will serialize
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @author Dmytro Nosan
 */
final class DefaultJsonWriter<T> implements JsonWriter<T> {

	private final Configuration configuration;

	private final Members<T> members;

	/**
	 * Creates a {@link DefaultJsonWriter} with the specified configuration and members.
	 * @param configuration the configuration settings to be used for JSON writing
	 * @param members the members defining how the object properties should be serialized
	 */
	DefaultJsonWriter(Configuration configuration, Members<T> members) {
		this.configuration = configuration;
		this.members = members;
	}

	@Override
	public void write(T instance, Appendable out) {
		this.members.write(instance, new JsonValueWriter(this.configuration, out));
	}

}
