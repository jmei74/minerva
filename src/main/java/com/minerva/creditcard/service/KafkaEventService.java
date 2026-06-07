package com.minerva.creditcard.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.internals.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes credit-card domain events to Kafka topics.
 * Each journey type maps to a dedicated topic.
 */
@Service
public class KafkaEventService {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Topic names — match the 5 topics created in Kafka
    public static final String TOPIC_AUTH = "credit-card.authorization";
    public static final String TOPIC_TRANSACTION = "credit-card.transaction";
    public static final String TOPIC_REPAYMENT = "credit-card.repayment";
    public static final String TOPIC_CREDIT_LIMIT = "credit-card.credit-limit";
    public static final String TOPIC_ACCOUNT_STATUS = "credit-card.account-status";

    public KafkaEventService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // ── Low-level send ─────────────────────────────────

    public CompletableFuture<SendResult<String, Object>> send(String topic, String key, Object event) {
        return kafkaTemplate.send(topic, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[Kafka]❌ Failed to send to {} key={} → {}", topic, key, ex.getMessage());
                } else {
                    log.debug("[Kafka] ✅ Sent to {} partition={} offset={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }

    // ── Domain event builders ───────────────────────────

    public record AuthorizationEvent(
        String eventId,
        String authCode,
        String accountId,
        String cardNo,
        String txnType,
        double amount,
        String merchantId,
        String merchantName,
        String status,
        Instant timestamp
    ) {}

    public record TransactionEvent(
        String eventId,
        String txnId,
        String accountId,
        String txnType,
        double amount,
        double availableBefore,
        double availableAfter,
        String status,
        Instant timestamp
    ) {}

    public record RepaymentEvent(
        String eventId,
        String txnId,
        String accountId,
        double amount,
        double availableBefore,
        double availableAfter,
        String status,
        Instant timestamp
    ) {}

    public record CreditLimitEvent(
        String eventId,
        String adjId,
        String accountId,
        double previousLimit,
        double newLimit,
        String reason,
        String status,
        Instant timestamp
    ) {}

    public record AccountStatusEvent(
        String eventId,
        String accountId,
        String previousStatus,
        String newStatus,
        Instant timestamp
    ) {}

    // ── Typed publish helpers ───────────────────────────

    public void publishAuthorization(String authCode, String accountId, String cardNo,
            String txnType, double amount, String merchantId, String merchantName, String status) {
        var event = new AuthorizationEvent(
            UUID.randomUUID().toString(),
            authCode, accountId, cardNo, txnType, amount,
            merchantId, merchantName, status, Instant.now()
        );
        send(TOPIC_AUTH, authCode, event);
    }

    public void publishTransaction(String txnId, String accountId,
            String txnType, double amount,
            double availableBefore, double availableAfter, String status) {
        var event = new TransactionEvent(
            UUID.randomUUID().toString(),
            txnId, accountId, txnType, amount,
            availableBefore, availableAfter, status, Instant.now()
        );
        send(TOPIC_TRANSACTION, txnId, event);
    }

    public void publishRepayment(String txnId, String accountId, double amount,
            double availableBefore, double availableAfter, String status) {
        var event = new RepaymentEvent(
            UUID.randomUUID().toString(),
            txnId, accountId, amount, availableBefore, availableAfter, status, Instant.now()
        );
        send(TOPIC_REPAYMENT, txnId, event);
    }

    public void publishCreditLimitAdjustment(String adjId, String accountId,
            double previousLimit, double newLimit, String reason, String status) {
        var event = new CreditLimitEvent(
            UUID.randomUUID().toString(),
            adjId, accountId, previousLimit, newLimit, reason, status, Instant.now()
        );
        send(TOPIC_CREDIT_LIMIT, adjId, event);
    }

    public void publishAccountStatusChange(String accountId, String previousStatus, String newStatus) {
        var event = new AccountStatusEvent(
            UUID.randomUUID().toString(),
            accountId, previousStatus, newStatus, Instant.now()
        );
        send(TOPIC_ACCOUNT_STATUS, accountId, event);
    }
}