package com.abhi.trafficmate.controller;

import com.abhi.trafficmate.model.LocationPoint;
import com.abhi.trafficmate.model.RouteInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class TrafficService {

    private final String orsApiKey;

    private final RestTemplate restTemplate;

    public TrafficService(@Value("${ors.api.key}") String orsApiKey, RestTemplate restTemplate) {
        this.orsApiKey = orsApiKey;
        this.restTemplate = restTemplate;
    }

    public RouteInfo getRouteInfo(String mode, LocationPoint start, LocationPoint end) throws IOException {
        String url = String.format(
                "https://api.openrouteservice.org/v2/directions/%s?api_key=%s&start=%f,%f&end=%f,%f",
                mode, orsApiKey, start.lon, start.lat, end.lon, end.lat
        );

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.getBody());
        JsonNode summary = root.path("features").get(0).path("properties").path("summary");

        double durationSeconds = summary.path("duration").asDouble();
        double distanceMeters = summary.path("distance").asDouble();

        return new RouteInfo(durationSeconds / 60.0, distanceMeters / 1000.0); // min, km
    }
}

