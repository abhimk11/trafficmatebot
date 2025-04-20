package com.abhi.trafficmate.repo;

import com.abhi.trafficmate.model.Subscription;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SubscriptionRepository extends MongoRepository<Subscription, String> {
    Optional<Subscription> findByChatId(Long chatId);

    void deleteByChatId(Long chatId);

    boolean existsByChatId(Long chatId);
}

