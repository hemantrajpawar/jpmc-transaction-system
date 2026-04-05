package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Component
public class TransactionProcessor {

    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final DatabaseConduit databaseConduit;
    private final RestTemplate restTemplate;  // NEW

    public TransactionProcessor(UserRepository userRepository,
                                TransactionRecordRepository transactionRecordRepository,
                                DatabaseConduit databaseConduit,
                                RestTemplate restTemplate) {  // ADDED
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.databaseConduit = databaseConduit;
        this.restTemplate = restTemplate;
    }

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void processTransaction(Transaction transaction) {
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        if (sender == null) return;

        UserRecord recipient = userRepository.findById(transaction.getRecipientId());
        if (recipient == null) return;

        if (sender.getBalance() < transaction.getAmount()) return;

        // NEW: Call incentives API after validation
        Incentive incentive = restTemplate.postForObject("http://localhost:8080/incentive", transaction, Incentive.class);
        float incentiveAmount = (incentive != null) ? incentive.getAmount() : 0.0f;

        // Update balances: deduct from sender, add transaction + incentive to recipient
        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

        databaseConduit.save(sender);
        databaseConduit.save(recipient);

        // Save record with incentive
        TransactionRecord record = new TransactionRecord(sender, recipient,
                transaction.getAmount(), incentiveAmount, LocalDateTime.now());
        transactionRecordRepository.save(record);
    }
}
