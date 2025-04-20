package com.abhi.trafficmate.bot;

import com.abhi.trafficmate.controller.TrafficService;
import com.abhi.trafficmate.model.LocationPoint;
import com.abhi.trafficmate.model.RouteInfo;
import com.abhi.trafficmate.model.Subscription;
import com.abhi.trafficmate.repo.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Slf4j
public class BotCommandHandler {

    private final TrafficService trafficService;
    private final Map<Long, LocationPoint> sourceMap = new HashMap<>();
    private final Map<Long, LocationPoint> destinationMap = new HashMap<>();
    private final Map<Long, String> userStep = new HashMap<>();

    SubscriptionRepository subscriptionRepository;

    private final Map<String, String> travelModes = Map.of(
            "Car", "driving-car",
            "Bike", "cycling-regular",
            "Walk", "foot-walking"
    );

    private final Map<String, String> modeEmojis = Map.of(
            "Car", "🚗",
            "Bike", "🏍️",
            "Walk", "🚶"
    );

    public BotCommandHandler(TrafficService trafficService, SubscriptionRepository subscriptionRepository) {
        this.trafficService = trafficService;
        this.subscriptionRepository = subscriptionRepository;
    }

    public void handleTextMessage(Long chatId, String text, TrafficMateBot bot) {
        switch (text) {
            case "/start" ->
                    sendMessage(bot, chatId, "👋 Welcome to *TrafficMateBot*!\n\nUse /traffic to check travel time between two locations.", true);
            case "/traffic" -> {
                sendMessage(bot, chatId, "📍 Please share your *starting location* using Telegram location button.", true);
                userStep.put(chatId, "WAITING_FOR_SOURCE");
            }
            case "/subscribe" -> {
                LocationPoint source = sourceMap.get(chatId);
                LocationPoint destination = destinationMap.get(chatId);

                if (source != null && destination != null) {
                    if (subscriptionRepository.existsByChatId(chatId)) {
                        sendMessage(bot, chatId, "ℹ️ You are already subscribed for traffic updates.", true);
                    } else {
                        handleSubscribe(chatId, source, destination, bot);
                    }
                } else {
                    sendMessage(bot, chatId, "⚠️ Please use /traffic and share both source and destination before subscribing.", true);
                }
            }
            case "/updatelocation" -> {
                // Check if the user is subscribed before proceeding
                Optional<Subscription> subscription = subscriptionRepository.findByChatId(chatId);
                if (subscription.isPresent()) {
                    sendMessage(bot, chatId, "📍 Please share your new *starting location*.", true);
                    userStep.put(chatId, "WAITING_FOR_NEW_SOURCE");
                } else {
                    sendMessage(bot, chatId, "⚠️ You need to subscribe first to update your location. Use /subscribe to subscribe.", true);
                }
            }
            case "/unsubscribe" -> handleUnsubscribe(chatId, bot);

            default -> {
                sendMessage(bot, chatId, "⚠️ Sorry, I didn't recognize that command. Use /start for help.", true);
            }
        }
    }

    public void handleUnsubscribe(Long chatId, TrafficMateBot bot) {
        if (subscriptionRepository.existsByChatId(chatId)) {
            subscriptionRepository.deleteByChatId(chatId);
            sendMessage(bot, chatId, "❌ You have unsubscribed from traffic updates.", true);
        } else {
            sendMessage(bot, chatId, "ℹ️ You are not currently subscribed.", true);
        }
    }


    public void handleSubscribe(Long chatId, LocationPoint source, LocationPoint destination, TrafficMateBot bot) {
        Subscription subscription = new Subscription();
        subscription.setChatId(chatId);
        subscription.setSourceLat(source.lat);
        subscription.setSourceLon(source.lon);
        subscription.setDestinationLat(destination.lat);
        subscription.setDestinationLon(destination.lon);

        subscriptionRepository.save(subscription);
        sendMessage(bot, chatId, "✅ You have subscribed for traffic updates every 6 hours!", true);
    }

    public void handleLocation(Long chatId, LocationPoint location, TrafficMateBot bot) {
        String step = userStep.get(chatId);

        if ("WAITING_FOR_SOURCE".equals(step)) {
            // Save source location
            sourceMap.put(chatId, location);
            userStep.put(chatId, "WAITING_FOR_DESTINATION");
            sendMessage(bot, chatId, "✅ Source saved! Now share your *destination location*.", true);
        } else if ("WAITING_FOR_DESTINATION".equals(step)) {
            // Save destination location
            destinationMap.put(chatId, location);
            sendMessage(bot, chatId, "⏳ Calculating best travel option...", false);

            // Handle ETA comparison and send result to user
            handleETAComparison(chatId, bot);

            // Remove the user's step after processing
            userStep.remove(chatId);
        } else if ("WAITING_FOR_NEW_SOURCE".equals(step)) {
            // Check if user is subscribed before updating the location
            Optional<Subscription> subscription = subscriptionRepository.findByChatId(chatId);
            if (subscription.isPresent()) {
                // Update source location if the user is subscribed
                sourceMap.put(chatId, location);
                subscription.get().setSourceLat(location.lat);
                subscription.get().setSourceLon(location.lon);
                subscriptionRepository.save(subscription.get());  // Save the updated source location to the database

                sendMessage(bot, chatId, "✅ *Source location* updated successfully! Now share your *destination location*.", true);
                userStep.put(chatId, "WAITING_FOR_NEW_DESTINATION");
            } else {
                // Inform the user they need to subscribe first
                sendMessage(bot, chatId, "⚠️ You need to subscribe first to update your location. Use /subscribe to subscribe.", true);
            }
        } else if ("WAITING_FOR_NEW_DESTINATION".equals(step)) {
            // Check if user is subscribed before updating the location
            Optional<Subscription> subscription = subscriptionRepository.findByChatId(chatId);
            if (subscription.isPresent()) {
                // Update destination location if the user is subscribed
                destinationMap.put(chatId, location);
                subscription.get().setDestinationLat(location.lat);
                subscription.get().setDestinationLon(location.lon);
                subscriptionRepository.save(subscription.get());  // Save the updated destination location to the database

                sendMessage(bot, chatId, "✅ *Destination location* updated successfully! You will now receive updates for this new route.", true);
                userStep.remove(chatId);
            } else {
                // Inform the user they need to subscribe first
                sendMessage(bot, chatId, "⚠️ You need to subscribe first to update your location. Use /subscribe to subscribe.", true);
            }
        }
    }


    private void handleETAComparison(Long chatId, TrafficMateBot bot) {
        LocationPoint source = sourceMap.get(chatId);
        LocationPoint destination = destinationMap.get(chatId);
        Map<String, RouteInfo> results = new HashMap<>();

        for (Map.Entry<String, String> entry : travelModes.entrySet()) {
            try {
                results.put(entry.getKey(), trafficService.getRouteInfo(entry.getValue(), source, destination));
            } catch (Exception e) {
                sendMessage(bot, chatId, "❌ Failed to get data for " + entry.getKey(), false);
                log.info("Error for: {}", e.getMessage());
                return;
            }
        }

        String fastest = Collections.min(results.entrySet(), Comparator.comparingDouble(e -> e.getValue().etaMinutes)).getKey();

        StringBuilder response = new StringBuilder("🚦 *ETA Comparison*:\n\n");
        for (Map.Entry<String, RouteInfo> entry : results.entrySet()) {
            String mode = entry.getKey();
            RouteInfo info = entry.getValue();
            response.append(String.format("- %s %s: %.1f mins, %.1f km\n", modeEmojis.get(mode), mode, info.etaMinutes, info.distanceKm));
        }

        response.append(String.format("\n✅ *%s* is the fastest option right now!", fastest));
        response.append("\n\n🔔 Want automatic updates every 6 hours?\nUse /subscribe to enable them!");
        sendMessage(bot, chatId, response.toString(), true);
    }

    private void sendMessage(TrafficMateBot bot, Long chatId, String text, boolean markdown) {
        SendMessage msg = new SendMessage(chatId.toString(), text);
        if (markdown) msg.setParseMode("Markdown");
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            log.info("Exception Occurred: {}", e.getMessage());
        }
    }
}
