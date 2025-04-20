package com.abhi.trafficmate.bot;

import com.abhi.trafficmate.controller.TrafficService;
import com.abhi.trafficmate.model.LocationPoint;
import com.abhi.trafficmate.repo.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
@Slf4j
public class TrafficMateBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;
    private final BotCommandHandler commandHandler;


    public TrafficMateBot(String botUsername, String botToken, String orsApiKey, RestTemplate restTemplate, SubscriptionRepository subscriptionRepository) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        TrafficService trafficService = new TrafficService(orsApiKey, restTemplate);
        this.commandHandler = new BotCommandHandler(trafficService, subscriptionRepository);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onRegister() {
        List<BotCommand> commandList = List.of(
                new BotCommand("/start", "Start the bot"),
                new BotCommand("/traffic", "Get current weather"),
                new BotCommand("/subscribe", "Subscribe for weather updates"),
                new BotCommand("/unsubscribe", "Unsubscribe from updates"),
                new BotCommand("/updatelocation", "Update the saved location")
        );

        SetMyCommands setMyCommands = new SetMyCommands();
        setMyCommands.setCommands(commandList);
        setMyCommands.setScope(new BotCommandScopeDefault());

        try {
            this.execute(setMyCommands);
        } catch (TelegramApiException e) {
            log.info("Exception occurred");
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();

            if (message.hasText()) {
                commandHandler.handleTextMessage(chatId, message.getText(), this);
            } else if (message.hasLocation()) {
                Location loc = message.getLocation();
                commandHandler.handleLocation(chatId, new LocationPoint(loc.getLatitude(), loc.getLongitude()), this);
            }
        }
    }
}



