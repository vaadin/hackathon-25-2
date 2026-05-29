package com.infraleap.sternmap.stern.service;

import com.infraleap.sternmap.stern.domain.PinballSpot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Calls the Stern API once at startup so the lifted code paths are exercised
 * even before anyone opens the browser. Logs the closest few venues so you can
 * see immediately whether reverse engineering + auth still work.
 *
 * <p>Coordinates default to Vaadin Berlin office (Stresemannstr. 11, 10963 Berlin).
 */
@Component
public class SternSmokeTest implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SternSmokeTest.class);

    private final SternLocationService locationService;
    private final SternAuthService authService;

    public SternSmokeTest(SternLocationService locationService, SternAuthService authService) {
        this.locationService = locationService;
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        // Trigger auth so we see "Stern authentication successful" or its failure mode
        // before the public-endpoint call runs.
        boolean authed = authService.login();
        log.info("Stern auth at startup: {} (token present={})",
                authed ? "OK" : "FAILED",
                authService.getToken() != null);

        double lat = 52.5050;
        double lon = 13.3805;
        log.info("Stern smoke test — querying nearby_leaderboards around ({}, {})", lat, lon);
        List<PinballSpot> spots = locationService.findNearby(lat, lon, 20);
        if (spots.isEmpty()) {
            log.warn("Stern smoke test returned 0 spots — API call failed or filter dropped everything.");
            return;
        }
        int show = Math.min(10, spots.size());
        log.info("Stern smoke test OK — closest {} venues:", show);
        spots.stream().limit(show).forEach(s ->
                log.info("  - {} ({}) at {}  [{} km, {} active tournament(s)]",
                        s.location().name(),
                        s.location().locationTypeName(),
                        s.location().city(),
                        s.distance() == null ? "?" : String.format("%.2f", s.distance()),
                        s.leaderboardNames().size()));
    }
}
