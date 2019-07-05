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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Map;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

/**
 * Indicates that a <strong>static final</strong> field's value should be added to the
 * {@link Environment} with the given name. <pre class="code">
 * &#064;DataNeo4jTest
 * &#064;Testcontainers
 * class DataNeo4jTestIntegrationTests {
 *
 *    &#064;Container
 *    static final Neo4jContainer&lt;?&gt; neo4j = new Neo4jContainer&lt;&gt;();
 *
 *    &#064;MapToProperty("spring.data.neo4j.uri")
 *    static final Supplier&lt;String&gt; neo4jUri = neo4j::getBoltUrl;
 *
 * }
 *</pre>
 * @author Dmytro Nosan
 * @since 2.2.0
 * @see TestPropertySource
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface MapToProperty {

	/**
	 * Defines the property mapping. When used at the {@link Map}, {@link Collection},
	 * {@link TestPropertyValues} or {@code Array}, this value will be used as a prefix
	 * for all mapped attributes.
	 * @return the name
	 */
	String value() default "";

}
