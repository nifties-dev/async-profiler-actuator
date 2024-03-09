package dev.nifties.integration.springframework.boot.actuate.profiler;

import one.profiler.AsyncProfiler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Spring {@link Condition} that checks availability of {@link AsyncProfiler} allowing to effectively skip all beans on
 * unsupported platforms.
 *
 * @author Andris Rauda
 */
public class AsyncProfilerAvailableCondition implements Condition {
    private static final Log log = LogFactory.getLog(AsyncProfilerAvailableCondition.class);

    private static final boolean AVAILABLE;
    static {
        boolean available;
        try {
            final AsyncProfiler instance = AsyncProfiler.getInstance();
            log.info("AsyncProfilerEndpoint activated with " + instance.getVersion());
            available = true;
        } catch (RuntimeException ex) {
            log.warn("AsyncProfilerEndpoint not available: " + ex.getMessage());
            available = false;
        }
        AVAILABLE = available;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return AVAILABLE;
    }
}
