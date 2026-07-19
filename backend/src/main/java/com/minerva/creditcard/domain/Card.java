package com.minerva.creditcard.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
public class Card {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "card_id", unique = true, nullable = false, length = 36)
    private String cardId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @Column(name = "token", unique = true, nullable = false, length = 64)
    private String token;
    
    @Column(name = "masked_pan", length = 20)
    private String maskedPan;
    
    @Column(name = "card_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CardType cardType;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CardStatus status;
    
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;
    
    @Column(name = "cvv", length = 4)
    private String cvv;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = CardStatus.INACTIVE;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getMaskedPan() { return maskedPan; }
    public void setMaskedPan(String maskedPan) { this.maskedPan = maskedPan; }
    
    public CardType getCardType() { return cardType; }
    public void setCardType(CardType cardType) { this.cardType = cardType; }
    
    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Builder pattern
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final Card card = new Card();
        
        public Builder cardId(String cardId) { card.cardId = cardId; return this; }
        public Builder account(Account account) { card.account = account; return this; }
        public Builder token(String token) { card.token = token; return this; }
        public Builder maskedPan(String maskedPan) { card.maskedPan = maskedPan; return this; }
        public Builder cardType(CardType cardType) { card.cardType = cardType; return this; }
        public Builder status(CardStatus status) { card.status = status; return this; }
        public Builder expiryDate(LocalDate expiryDate) { card.expiryDate = expiryDate; return this; }
        public Builder cvv(String cvv) { card.cvv = cvv; return this; }
        public Card build() { return card; }
    }
    
    public enum CardType {
        PHYSICAL, VIRTUAL
    }
    
    public enum CardStatus {
        INACTIVE, ACTIVE, SUSPENDED, BLOCKED, EXPIRED
    }
}
