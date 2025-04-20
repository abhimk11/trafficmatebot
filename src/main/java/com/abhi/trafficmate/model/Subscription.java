package com.abhi.trafficmate.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "subscriptions")
@Data
public class Subscription {

    @Id
    private String id;
    private Long chatId;
    private double sourceLat;
    private double sourceLon;
    private double destinationLat;
    private double destinationLon;
}
