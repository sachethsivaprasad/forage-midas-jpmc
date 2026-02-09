package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);

    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final IncentiveApiClient incentiveApiClient;

    public TransactionProcessor(UserRepository userRepository,
                                TransactionRecordRepository transactionRecordRepository,
                                IncentiveApiClient incentiveApiClient) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.incentiveApiClient = incentiveApiClient;
    }

    /**
     * Validate and apply a transaction.
     * Valid if: sender exists, recipient exists, sender balance >= amount.
     * For valid transactions: post to Incentive API, store incentive, persist TransactionRecord,
     * debit sender by amount only, credit recipient by amount + incentive.
     */
    @Transactional
    public void process(Transaction transaction) {
        long senderId = transaction.getSenderId();
        long recipientId = transaction.getRecipientId();
        float amount = transaction.getAmount();

        UserRecord sender = userRepository.findById(senderId);
        UserRecord recipient = userRepository.findById(recipientId);

        if (sender == null || recipient == null) {
            logger.info("Discarding transaction {} -> {} for {}: invalid user(s)", senderId, recipientId, amount);
            return;
        }

        if (sender.getBalance() < amount) {
            logger.info("Discarding transaction {} -> {} for {}: insufficient funds (balance={})",
                    senderId, recipientId, amount, sender.getBalance());
            return;
        }

        Incentive incentiveResponse = incentiveApiClient.fetchIncentive(transaction);
        float incentive = incentiveResponse != null ? Math.max(0f, incentiveResponse.getAmount()) : 0f;

        TransactionRecord record = new TransactionRecord(sender, recipient, amount, incentive);
        transactionRecordRepository.save(record);

        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount + incentive);

        userRepository.save(sender);
        userRepository.save(recipient);

        logger.info("Processed transaction {} -> {} for {}, incentive {}. Balances: sender={}, recipient={}",
                senderId, recipientId, amount, incentive, sender.getBalance(), recipient.getBalance());
    }
}


