package fraud.fraud.Monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class CustomMetricsService {

    private final Counter customMetricCounter;
    private final Counter totalApiRequestCounter;
    private final Counter rateLimitedRequestCounter;


    public CustomMetricsService(MeterRegistry meterRegistry) {
        customMetricCounter = Counter.builder("custom_metric_name")
                .description("Description of custom metric")
                .tags("environment", "development")
                .register(meterRegistry);
        totalApiRequestCounter = Counter.builder("total_api_requests")
                .description("total_api_requests")
                .register(meterRegistry);
        rateLimitedRequestCounter = Counter.builder("total_rate_limited_blocked_requests")
                .description("total_rate_limited_blocked_requests")
                .register(meterRegistry);
    }

    public void incrementCustomMetric() {
        customMetricCounter.increment();
    }
    public void incrementTotalApiRequests() {
        totalApiRequestCounter.increment();
    }
    public void incrementRateLimitedRequests() {
        rateLimitedRequestCounter.increment();
    }
}
