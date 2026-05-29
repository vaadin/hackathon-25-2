package com.infraleap.sternmap.stern.service;

import com.infraleap.sternmap.config.SternProperties;
import com.infraleap.sternmap.stern.domain.Leaderboard;
import com.infraleap.sternmap.stern.domain.NearbyLeaderboardsResponse;
import com.infraleap.sternmap.stern.domain.PinballSpot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Calls Stern's <code>/api/v1/portal/nearby_leaderboards/</code> endpoint
 * (discovered by static analysis of the Stern IC Flutter app + cross-checked
 * via the existing stern-home-leaderboards project). The endpoint takes
 * <code>latitude</code> + <code>longitude</code> query params and returns
 * leaderboards sorted by distance from those coords.
 * <p>
 * The endpoint works without authentication for the basic results; we still
 * send the auth headers when available since some fields may be richer.
 */
@Service
public class SternLocationService {

    private static final Logger log = LoggerFactory.getLogger(SternLocationService.class);
    private static final String CMS_BASE = "https://cms.prd.sternpinball.io/api/v1/portal";

    private final SternAuthService authService;
    private final WebClient webClient;
    private final String locationHeader;

    public SternLocationService(SternAuthService authService, SternProperties props) {
        this.authService = authService;
        this.locationHeader = "{\"country\":\"" + props.defaultCountry()
                + "\",\"continent\":\"" + props.defaultContinent() + "\"}";
        this.webClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                .defaultHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:142.0) Gecko/20100101 Firefox/142.0")
                .defaultHeader("Accept", "application/json, text/plain, */*")
                .defaultHeader("Referer", "https://insider.sternpinball.com/")
                .defaultHeader("Origin", "https://insider.sternpinball.com")
                .build();
    }

    /**
     * Find pinball venues near the given coordinates, sorted by distance.
     * Deduplicates locations that host multiple leaderboards.
     *
     * <p>Stern's API caps the response at 10 leaderboards per page (the
     * {@code page_size} param appears to be ignored), and many leaderboards
     * share a venue, so we paginate until we have enough unique venues or
     * Stern runs out of "next" pages.
     */
    public List<PinballSpot> findNearby(double latitude, double longitude, int targetVenues) {
        List<Leaderboard> all = new ArrayList<>();
        Set<Long> seenVenuePks = new HashSet<>();

        String url = CMS_BASE + "/nearby_leaderboards/?latitude=" + latitude
                + "&longitude=" + longitude;
        int pages = 0;
        // Stern caps each page at 10 rows AND a single busy venue (e.g. Gamestate
        // Potsdamer Platz Berlin) can have 1500+ active tournaments which fully
        // dominate the dataset — pages 1..150 all belong to the same venue. So
        // pagination doesn't help us surface "different" venues; we just take
        // the closest one and its tournaments, capped at a few pages.
        int maxPages = 5;
        while (url != null && pages < maxPages && seenVenuePks.size() < targetVenues) {
            pages++;
            log.info("GET {}", url);
            NearbyLeaderboardsResponse resp = fetch(url);
            if (resp == null || resp.results() == null) break;
            for (Leaderboard lb : resp.results()) {
                if (lb.lbLocation() == null || lb.lbLocation().latitude() == null
                        || lb.lbLocation().longitude() == null) continue;
                all.add(lb);
                seenVenuePks.add(lb.lbLocation().pk());
            }
            url = resp.next();
        }

        List<PinballSpot> spots = PinballSpot.from(all);
        log.info("Stern returned {} leaderboards across {} page(s) → {} unique venues{}",
                all.size(), pages, spots.size(),
                spots.isEmpty() ? "" : " (closest: " + spots.get(0).location().name()
                        + " @ " + spots.get(0).distance() + ")");
        return spots;
    }

    private NearbyLeaderboardsResponse fetch(String url) {
        WebClient.RequestHeadersSpec<?> spec = webClient.get().uri(url);
        String token = authService.getToken();
        String cookies = authService.getCookies();
        if (token != null) spec = spec.header("Authorization", "Bearer " + token);
        if (cookies != null) spec = spec.header("Cookie", cookies);
        spec = spec.header("Location", locationHeader);
        try {
            return spec.retrieve()
                    .bodyToMono(NearbyLeaderboardsResponse.class)
                    .block(Duration.ofSeconds(20));
        } catch (Exception e) {
            log.error("Failed to fetch {}: {}", url, e.toString());
            return null;
        }
    }
}
