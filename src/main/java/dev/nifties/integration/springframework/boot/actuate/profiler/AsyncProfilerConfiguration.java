package dev.nifties.integration.springframework.boot.actuate.profiler;

import one.profiler.AsyncProfiler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.*;

/**
 * Spring context configuration for creating AsyncProfiler Actuator beans.
 *
 * @author Andris Rauda
 * @since 1.0.0
 */
@Configuration
@Conditional(AsyncProfilerAvailableCondition.class)
public class AsyncProfilerConfiguration {

    /**
     * Exposes AsyncProfiler instance as a Spring bean, so it would automatically be exposed as a JMX MBean.
     */
    @Bean
    public static AsyncProfiler asyncProfiler() {
        return AsyncProfiler.getInstance();
    }

    @Bean
    @ConditionalOnWebApplication
    public AsyncProfilerWebEndpoint asyncProfilerWebEndpoint(AsyncProfiler asyncProfiler) {
        return new AsyncProfilerWebEndpoint(asyncProfiler);
    }

}
