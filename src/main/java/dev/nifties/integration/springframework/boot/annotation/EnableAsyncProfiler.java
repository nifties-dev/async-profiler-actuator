package dev.nifties.integration.springframework.boot.annotation;

import dev.nifties.integration.springframework.boot.actuate.profiler.AsyncProfilerConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables AsyncProfiler Actuator.
 *
 * @author Andris Rauda
 * @since 1.0.0
 */
@Import(AsyncProfilerConfiguration.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableAsyncProfiler {
}
