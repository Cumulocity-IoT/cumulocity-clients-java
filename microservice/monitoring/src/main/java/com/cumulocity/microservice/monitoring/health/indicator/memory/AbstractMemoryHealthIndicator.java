package com.cumulocity.microservice.monitoring.health.indicator.memory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;

import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public abstract class AbstractMemoryHealthIndicator extends AbstractHealthIndicator {

    private static final long UNDEFINED = -1;

    private static final Log logger = LogFactory.getLog(HeapMemoryHealthIndicator.class);

    private final AbstractMemoryHealthIndicatorProperties properties;

    /**
     * Create a new {@link AbstractMemoryHealthIndicator}.
     *
     * @param properties the memory properties
     */
    protected AbstractMemoryHealthIndicator(AbstractMemoryHealthIndicatorProperties properties) {
        this.properties = properties;
    }

    @Override
    protected final void doHealthCheck(Health.Builder builder) throws Exception {
        MemoryUsage memoryUsage = getMemoryUsage(this.properties.getMemoryMXBean());
        long available = memoryUsage.getMax() - memoryUsage.getUsed();
        long threshold = getThreshold(memoryUsage, this.properties.getThreshold());

        if (UNDEFINED == threshold || available > threshold) {
            builder.up();
        } else {
            logger.warn(String.format(
                    "Free memory below threshold. " + "Available: %d bytes (threshold: %d bytes)",
                    available, threshold));
            builder.down();
        }

        builder.withDetail("init", memoryUsage.getInit())
                .withDetail("used", memoryUsage.getUsed())
                .withDetail("committed", memoryUsage.getCommitted())
                .withDetail("max", memoryUsage.getMax())
                .withDetail("threshold", threshold);
    }

    /**
     * Returns the {@link MemoryUsage} to perform the health check on.
     *
     * @param memoryMXBean the configured {@link MemoryMXBean} to extract the {@code MemoryUsage} from
     * @return the {@code MemoryUsage} to perform the health check on
     */
    protected abstract MemoryUsage getMemoryUsage(MemoryMXBean memoryMXBean);

    private static long getThreshold(MemoryUsage memoryUsage, double threshold) {
        return UNDEFINED == memoryUsage.getMax() ? UNDEFINED : (long) (memoryUsage.getMax() * threshold);
    }

}
