package com.xiaoliang.simukraft.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 统一处理模组后台日志：
 * 1. 屏蔽非致命 WARN；
 * 2. 为 ERROR/FATAL 添加统一前缀。
 */
public final class SimukraftLogConfigurator {
    private static final String MOD_PACKAGE_PREFIX = "com.xiaoliang.simukraft";
    private static final String ERROR_PREFIX = "[EEE>_<] ";
    private static final String WRAPPER_NAME_PREFIX = "SimukraftWrapped-";
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private SimukraftLogConfigurator() {
    }

    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        LoggerConfig rootLogger = configuration.getRootLogger();
        Map<String, Appender> originalAppenders = new LinkedHashMap<>(rootLogger.getAppenders());

        for (Appender appender : originalAppenders.values()) {
            String wrapperName = WRAPPER_NAME_PREFIX + appender.getName();
            if (configuration.getAppender(wrapperName) != null) {
                continue;
            }

            FilteringPrefixAppender wrapper = new FilteringPrefixAppender(wrapperName, appender);
            wrapper.start();
            configuration.addAppender(wrapper);
            rootLogger.removeAppender(appender.getName());
            rootLogger.addAppender(wrapper, null, null);
        }

        context.updateLoggers();
    }

    private static boolean isSimukraftBackendLog(LogEvent event) {
        String loggerName = event.getLoggerName();
        return loggerName != null && loggerName.startsWith(MOD_PACKAGE_PREFIX);
    }

    private static boolean shouldSuppress(LogEvent event) {
        return isSimukraftBackendLog(event) && event.getLevel() == Level.WARN;
    }

    private static LogEvent prefixIfNeeded(LogEvent event) {
        if (!isSimukraftBackendLog(event) || !event.getLevel().isMoreSpecificThan(Level.ERROR)) {
            return event;
        }

        String originalMessage = event.getMessage() != null ? event.getMessage().getFormattedMessage() : "";
        if (originalMessage.startsWith(ERROR_PREFIX)) {
            return event;
        }

        return new Log4jLogEvent.Builder(event.toImmutable())
                .setMessage(new SimpleMessage(ERROR_PREFIX + originalMessage))
                .build();
    }

    private static final class FilteringPrefixAppender extends AbstractAppender {
        private final Appender delegate;

        private FilteringPrefixAppender(String name, Appender delegate) {
            super(name, null, (Layout<? extends Serializable>) null, true, null);
            this.delegate = delegate;
        }

        @Override
        public void append(LogEvent event) {
            if (event == null || shouldSuppress(event)) {
                return;
            }
            delegate.append(prefixIfNeeded(event));
        }

        @Override
        public void stop() {
            stop(0L, TimeUnit.MILLISECONDS);
        }

        @Override
        public boolean stop(long timeout, TimeUnit timeUnit) {
            setStopped();
            return true;
        }

        @Override
        public Filter getFilter() {
            return null;
        }
    }
}
