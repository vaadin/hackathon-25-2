package com.infraleap.sternmap.stern.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * View-model: one physical venue with all leaderboards/competitions running there.
 * Multiple Stern leaderboards may share the same location — we dedupe by location pk.
 */
public record PinballSpot(LbLocation location, List<String> leaderboardNames, Double distance) {

    public static List<PinballSpot> from(Collection<Leaderboard> leaderboards) {
        java.util.LinkedHashMap<Long, List<Leaderboard>> grouped = new java.util.LinkedHashMap<>();
        for (Leaderboard lb : leaderboards) {
            if (lb.lbLocation() == null || lb.lbLocation().latitude() == null) continue;
            grouped.computeIfAbsent(lb.lbLocation().pk(), k -> new ArrayList<>()).add(lb);
        }
        return grouped.values().stream()
                .map(group -> {
                    LbLocation loc = group.get(0).lbLocation();
                    Double dist = group.stream()
                            .map(Leaderboard::distance)
                            .filter(java.util.Objects::nonNull)
                            .min(Double::compareTo)
                            .orElse(null);
                    List<String> names = group.stream().map(Leaderboard::name).filter(n -> n != null && !n.isBlank()).distinct().toList();
                    return new PinballSpot(loc, names, dist);
                })
                .toList();
    }
}
