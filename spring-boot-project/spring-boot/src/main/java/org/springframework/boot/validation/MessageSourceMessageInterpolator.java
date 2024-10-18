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

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.validation.MessageInterpolator;
import jakarta.validation.metadata.ConstraintDescriptor;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Resolves any message parameters through {@link MessageSource} and then interpolates a
 * message using the underlying {@link MessageInterpolator}.
 *
 * @author Dmytro Nosan
 * @author Scott Frederick
 */
class MessageSourceMessageInterpolator implements MessageInterpolator {

	private static final String DEFAULT_MESSAGE = MessageSourceMessageInterpolator.class.getName();

	private final MessageSource messageSource;

	private final MessageInterpolator messageInterpolator;

	MessageSourceMessageInterpolator(MessageSource messageSource, MessageInterpolator messageInterpolator) {
		this.messageSource = messageSource;
		this.messageInterpolator = messageInterpolator;
	}

	@Override
	public String interpolate(String messageTemplate, Context context) {
		return interpolate(messageTemplate, context, LocaleContextHolder.getLocale());
	}

	@Override
	public String interpolate(String messageTemplate, Context context, Locale locale) {
		String message = replaceParameters(messageTemplate, context, locale);
		return this.messageInterpolator.interpolate(message, context, locale);
	}

	/**
	 * Recursively replaces all message parameters.
	 * <p>
	 * The message parameter prefix <code>&#123;</code> and suffix <code>&#125;</code> can
	 * be escaped using {@code \}, e.g. <code>\&#123;escaped\&#125;</code>.
	 * @param message the message containing the parameters to be replaced
	 * @param context contextual information related to the interpolation
	 * @param locale the locale to use when resolving replacements
	 * @return the message with parameters replaced
	 */
	private String replaceParameters(String message, Context context, Locale locale) {
		return replaceParameters(message, context, locale, new LinkedHashSet<>(4));
	}

	private String replaceParameters(String message, Context context, Locale locale, Set<String> visitedParameters) {
		StringBuilder buf = new StringBuilder(message);
		int parentheses = 0;
		int startIndex = -1;
		int endIndex = -1;
		for (int i = 0; i < buf.length(); i++) {
			if (buf.charAt(i) == '\\') {
				i++;
				continue;
			}
			// EL Expression
			if (buf.charAt(i) == '$' && next(buf, i, '{')) {
				i++;
				continue;
			}
			if (buf.charAt(i) == '{') {
				if (startIndex == -1) {
					startIndex = i;
				}
				parentheses++;
			}
			if (buf.charAt(i) == '}') {
				if (parentheses > 0) {
					parentheses--;
				}
				endIndex = i;
			}
			if (parentheses == 0 && startIndex < endIndex) {
				String parameter = buf.substring(startIndex + 1, endIndex);
				if (!visitedParameters.add(parameter)) {
					throw new IllegalArgumentException("Circular reference '{" + String.join(" -> ", visitedParameters)
							+ " -> " + parameter + "}'");
				}
				String value = replaceParameter(parameter, context, locale, visitedParameters);
				if (value != null) {
					buf.replace(startIndex, endIndex + 1, value);
					i = startIndex + value.length() - 1;
				}
				visitedParameters.remove(parameter);
				startIndex = -1;
				endIndex = -1;
			}
		}
		return buf.toString();
	}

	private String replaceParameter(String parameter, Context context, Locale locale, Set<String> visitedParameters) {
		parameter = replaceParameters(parameter, context, locale, visitedParameters);
		if (isBeanValidationAttribute(parameter, context)) {
			return null;
		}
		String value = this.messageSource.getMessage(parameter, null, DEFAULT_MESSAGE, locale);
		if (value == null || value.equals(DEFAULT_MESSAGE)) {
			return null;
		}
		return replaceParameters(value, context, locale, visitedParameters);
	}

	private boolean isBeanValidationAttribute(String parameter, Context context) {
		ConstraintDescriptor<?> constraintDescriptor = context.getConstraintDescriptor();
		Map<String, Object> attributes = constraintDescriptor.getAttributes();
		return attributes.containsKey(parameter);
	}

	private boolean next(StringBuilder buf, int index, char next) {
		return (index + 1) < buf.length() && buf.charAt(index + 1) == next;
	}

}
