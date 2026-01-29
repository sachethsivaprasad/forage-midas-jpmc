package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
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

    public TransactionProcessor(UserRepository userRepository,
                                TransactionRecordRepository transactionRecordRepository) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    /**
     * Validate and apply a transaction.
     * A transaction is valid if:
     * - sender exists
     * - recipient exists
     * - sender has balance >= amount
     *
     * For valid transactions, we:
     * - persist a TransactionRecord
     * - debit the sender
     * - credit the recipient
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

        // Record the transaction
        TransactionRecord record = new TransactionRecord(sender, recipient, amount);
        transactionRecordRepository.save(record);

        // Apply balance changes
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount);

        userRepository.save(sender);
        userRepository.save(recipient);

        logger.info("Processed transaction {} -> {} for {}. New balances: sender={}, recipient={}",
                senderId, recipientId, amount, sender.getBalance(), recipient.getBalance());
    }
}


