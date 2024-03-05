package dev.nifties.integration.springframework.boot.actuate.profiler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring context configuration for creating AsyncProfiler Actuator beans.
 *
 * @author Andris Rauda
 * @since 1.0.0
 */
@Configuration
public class AsyncProfilerConfiguration {

    @Bean
    @ConditionalOnWebApplication
    public AsyncProfilerWebEndpoint asyncProfilerWebEndpoint() {
        return new AsyncProfilerWebEndpoint();
    }
}
