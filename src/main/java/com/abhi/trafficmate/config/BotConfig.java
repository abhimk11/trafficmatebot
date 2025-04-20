package com.abhi.trafficmate.config;

import com.abhi.trafficmate.bot.TrafficMateBot;
import com.abhi.trafficmate.repo.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BotConfig {

    @Value("${telegram.bot.username}")
    private String username;

    @Value("${telegram.bot.token}")
    private String token;

    @Value("${ors.api.key}")
    private String orsApiKey;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Bean
    public TrafficMateBot trafficMateBot() {
        return new TrafficMateBot(username, token, orsApiKey, restTemplate(), subscriptionRepository);
    }
}