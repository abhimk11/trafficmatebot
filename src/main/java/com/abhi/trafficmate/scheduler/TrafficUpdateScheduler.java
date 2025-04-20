package com.abhi.trafficmate.scheduler;

import com.abhi.trafficmate.bot.TrafficMateBot;
import com.abhi.trafficmate.controller.TrafficService;
import com.abhi.trafficmate.model.LocationPoint;
import com.abhi.trafficmate.model.RouteInfo;
import com.abhi.trafficmate.model.Subscription;
import com.abhi.trafficmate.repo.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

@Component
public class TrafficUpdateScheduler {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private TrafficService trafficService;

    @Autowired
    private TrafficMateBot bot;

    // Every 6 hours (at 0, 6, 12, 18)
    @Scheduled(cron = "0 0 0/6 * * *")
    public void sendScheduledUpdates() {
        List<Subscription> subs = subscriptionRepository.findAll();

        for (Subscription sub : subs) {
            try {
                LocationPoint source = new LocationPoint(sub.getSourceLat(), sub.getSourceLon());
                LocationPoint dest = new LocationPoint(sub.getDestinationLat(), sub.getDestinationLon());

                RouteInfo car = trafficService.getRouteInfo("driving-car", source, dest);
                RouteInfo bike = trafficService.getRouteInfo("cycling-regular", source, dest);
                RouteInfo walk = trafficService.getRouteInfo("foot-walking", source, dest);

                String fastest = "Car";
                double minEta = car.etaMinutes;

                if (bike.etaMinutes < minEta) {
                    fastest = "Bike";
                    minEta = bike.etaMinutes;
                }
                if (walk.etaMinutes < minEta) {
                    fastest = "Walk";
                }

                String message = String.format(
                        "🕕 *Scheduled Traffic Update*\n\n" +
                                "🚗 Car: %.1f mins, %.1f km\n" +
                                "🏍️ Bike: %.1f mins, %.1f km\n" +
                                "🚶 Walk: %.1f mins, %.1f km\n\n" +
                                "✅ *%s* is the fastest option right now!",
                        car.etaMinutes, car.distanceKm,
                        bike.etaMinutes, bike.distanceKm,
                        walk.etaMinutes, walk.distanceKm,
                        fastest
                );

                SendMessage sendMessage = new SendMessage(sub.getChatId().toString(), message);
                sendMessage.setParseMode("Markdown");
                bot.execute(sendMessage);

            } catch (Exception e) {
                e.printStackTrace(); // or log properly
            }
        }
    }
}
