package com.minerva.creditcard.repository;

import com.minerva.creditcard.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    
    Optional<Card> findByCardId(String cardId);
    
    Optional<Card> findByToken(String token);
    
    List<Card> findByAccountId(Long accountId);
    
    List<Card> findByAccountIdAndStatus(Long accountId, Card.CardStatus status);
}
