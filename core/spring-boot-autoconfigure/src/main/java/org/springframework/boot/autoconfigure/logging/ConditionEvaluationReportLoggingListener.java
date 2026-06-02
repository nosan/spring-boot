/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.autoconfigure.logging;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.logging.LogLevel;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

/**
 * {@link ApplicationContextInitializer} that writes the {@link ConditionEvaluationReport}
 * to the log. Reports are logged at the {@link LogLevel#DEBUG DEBUG} level. A crash
 * report triggers an info output suggesting the user runs again with debug enabled to
 * display the report.
 * <p>
 * This initializer is not intended to be shared across multiple application context
 * instances.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class ConditionEvaluationReportLoggingListener
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private final LogLevel logLevel;

	public ConditionEvaluationReportLoggingListener() {
		this(LogLevel.DEBUG);
	}

	private ConditionEvaluationReportLoggingListener(LogLevel logLevel) {
		Assert.isTrue(isInfoOrDebug(logLevel), "'logLevel' must be INFO or DEBUG");
		this.logLevel = logLevel;
	}

	private boolean isInfoOrDebug(LogLevel logLevelForReport) {
		return LogLevel.INFO.equals(logLevelForReport) || LogLevel.DEBUG.equals(logLevelForReport);
	}

	/**
	 * Static factory method that creates a
	 * {@link ConditionEvaluationReportLoggingListener} which logs the report at the
	 * specified log level.
	 * @param logLevelForReport the log level to log the report at
	 * @return a {@link ConditionEvaluationReportLoggingListener} instance.
	 * @since 3.0.0
	 */
	public static ConditionEvaluationReportLoggingListener forLogLevel(LogLevel logLevelForReport) {
		return new ConditionEvaluationReportLoggingListener(logLevelForReport);
	}

	@Override
	public void initialize(ConfigurableApplicationContext context) {
		ConditionEvaluationReportListener listener = new ConditionEvaluationReportListener(context, this.logLevel);
		context.addApplicationListener(listener);
		context.getBeanFactory().addBeanPostProcessor((DestructionAwareBeanPostProcessor) (bean, beanName) -> {
			if (AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME.equals(beanName)) {
				listener.logReport(true);
			}
		});
	}

	private static final class ConditionEvaluationReportListener implements ApplicationListener<ContextRefreshedEvent> {

		private final ConfigurableApplicationContext context;

		private final ConditionEvaluationReportLogger logger;

		private final AtomicBoolean alreadyLogged = new AtomicBoolean(false);

		private ConditionEvaluationReportListener(ConfigurableApplicationContext context, LogLevel logLevel) {
			this.context = context;
			if (context instanceof GenericApplicationContext) {
				// Get the report early when the context allows early access to the bean
				// factory in case the context subsequently fails to load
				ConditionEvaluationReport report = ConditionEvaluationReport.get(context.getBeanFactory());
				this.logger = new ConditionEvaluationReportLogger(logLevel, () -> report);
			}
			else {
				this.logger = new ConditionEvaluationReportLogger(logLevel,
						() -> ConditionEvaluationReport.get(context.getBeanFactory()));
			}
		}

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			if (event.getApplicationContext() == this.context) {
				logReport(false);
			}
		}

		private void logReport(boolean isCrashReport) {
			if (this.alreadyLogged.compareAndSet(false, true)) {
				this.logger.logReport(isCrashReport);
			}
		}

	}

}
