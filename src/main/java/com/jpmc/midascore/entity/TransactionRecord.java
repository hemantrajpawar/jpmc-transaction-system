package com.jpmc.midascore.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class TransactionRecord {
    @Id
    @GeneratedValue
    private long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private UserRecord sender;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserRecord recipient;

    @Column(nullable = false)
    private float amount;

    @Column(nullable = false)
    private float incentive;  // NEW FIELD

    @Column(nullable = false)
    private LocalDateTime timestamp;

    protected TransactionRecord() {}

    public TransactionRecord(UserRecord sender, UserRecord recipient, float amount, float incentive, LocalDateTime timestamp) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.incentive = incentive;
        this.timestamp = timestamp;
    }

    // Getters
    public float getIncentive() {
        return incentive;
    }
}
