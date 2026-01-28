package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);

    /**
     * Kafka listener that consumes Transaction messages from the configured topic.
     * <p>
     * The topic name is provided via the {@code general.kafka-topic} property in {@code application.yml}.
     * For now, we simply deserialize and log the incoming transactions to verify integration.
     */
    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-core-consumer-group")
    public void onTransaction(Transaction transaction) {
        // Integration point: for now, just log the incoming transaction.
        logger.info("Received transaction from Kafka: {}", transaction);
    }
}


