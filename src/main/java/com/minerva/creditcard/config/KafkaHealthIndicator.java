package com.minerva.creditcard.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Kafka 集群健康检查 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(KafkaHealthIndicator.class);

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            var desc = client.describeCluster(
                new DescribeClusterOptions().timeoutMs(2000));
            String clusterId = desc.clusterId().get(2000, TimeUnit.MILLISECONDS);
            int brokers = desc.nodes().get(2000, TimeUnit.MILLISECONDS).size();
            return Health.up()
                .withDetail("clusterId", clusterId)
                .withDetail("nodes", brokers)
                .build();
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}