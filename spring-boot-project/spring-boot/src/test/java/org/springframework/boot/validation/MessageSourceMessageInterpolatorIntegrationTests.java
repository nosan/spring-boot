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

package org.springframework.boot.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Integration tests for {@link MessageSourceMessageInterpolator}.
 *
 * @author Dmytro Nosan
 */
class MessageSourceMessageInterpolatorIntegrationTests {

	@NotNull
	private String defaultMessage;

	@NotNull(message = "{null}")
	private String nullable;

	@NotNull(message = "{blank}")
	private String blank;

	@NotNull(message = "{unknown}")
	private String unknown;

	@NotNull(message = "{recursion}")
	private String recursion;

	@NotNull(message = "\\{null}")
	private String escapePrefix;

	@NotNull(message = "{null\\}")
	private String escapeSuffix;

	@NotNull(message = "\\{null\\}")
	private String escapePrefixSuffix;

	@NotNull(message = "\\\\{null}")
	private String escapeEscape;

	@Positive(message = "Value ${validatedValue} must be positive")
	private int elExpression = -100;

	@Size(min = 2, max = 10, message = "{attributes}")
	private String attributes = "Bean Validation Attributes";

	@Test
	void defaultMessage() {
		assertThat(validate("defaultMessage")).containsExactly("must not be null");
	}

	@Test
	void nullable() {
		assertThat(validate("nullable")).containsExactly("must not be null");
	}

	@Test
	void blank() {
		assertThat(validate("blank")).containsExactly("must not be null or must not be blank");
	}

	@Test
	void recursion() {
		assertThatException().isThrownBy(() -> validate("recursion"))
			.withStackTraceContaining("Circular reference '{recursion -> middle -> recursion}'");
	}

	@Test
	void unknown() {
		assertThat(validate("unknown")).containsExactly("{unknown}");
	}

	@Test
	void escapePrefix() {
		assertThat(validate("escapePrefix")).containsExactly("\\{null}");
	}

	@Test
	void escapeSuffix() {
		assertThat(validate("escapeSuffix")).containsExactly("{null\\}");
	}

	@Test
	void escapePrefixSuffix() {
		assertThat(validate("escapePrefixSuffix")).containsExactly("{null}");
	}

	@Test
	void escapeEscape() {
		assertThat(validate("escapeEscape")).containsExactly("\\must not be null");
	}

	@Test
	void elExpression() {
		assertThat(validate("elExpression")).containsExactly("Value -100 must be positive");
	}

	@Test
	void beanValidationAttributes() {
		assertThat(validate("attributes")).containsExactly("The value between 2 and 10");
	}

	private List<String> validate(String property) {
		return withEnglishLocale(() -> {
			Validator validator = buildValidator();
			List<String> messages = new ArrayList<>();
			Set<ConstraintViolation<Object>> constraints = validator.validateProperty(this, property);
			for (ConstraintViolation<Object> constraint : constraints) {
				messages.add(constraint.getMessage());
			}
			return messages;
		});
	}

	private static Validator buildValidator() {
		Locale locale = LocaleContextHolder.getLocale();
		StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.setUseCodeAsDefaultMessage(true);
		messageSource.addMessage("validatedValue", locale, "${validatedValue} should be ignored");
		messageSource.addMessage("min", locale, "{min} should be ignored");
		messageSource.addMessage("max", locale, "{max} should be ignored");
		messageSource.addMessage("attributes", locale, "The value between {min} and {max}");
		messageSource.addMessage("blank", locale, "{null} or {jakarta.validation.constraints.NotBlank.message}");
		messageSource.addMessage("null", locale, "{jakarta.validation.constraints.NotNull.message}");
		messageSource.addMessage("recursion", locale, "{middle}");
		messageSource.addMessage("middle", locale, "{recursion}");
		MessageInterpolatorFactory messageInterpolatorFactory = new MessageInterpolatorFactory(messageSource);
		try (LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean()) {
			validatorFactory.setMessageInterpolator(messageInterpolatorFactory.getObject());
			validatorFactory.afterPropertiesSet();
			return validatorFactory.getValidator();
		}
	}

	private static <T> T withEnglishLocale(Supplier<T> supplier) {
		Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(Locale.ENGLISH);
			return supplier.get();
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}

}
