package com.infraleap.sternmap.stern.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Unified view-model for a venue rendered on the map. A venue can belong to
 * multiple sources at once — e.g. COSMOSArcade Hamburger Meile is both a Stern
 * IC venue (8 networked machines in Stern's v2 catalogue) and a Stern Army
 * venue (Pinball Map's {@code is_stern_army=true} flag, also displayed on
 * sternpinball.com's marketing locator). The {@link #sources} set captures
 * both.
 */
public record VenueOnMap(
        long id,
        String name,
        String address,
        double lat,
        double lon,
        String websiteUrl,
        String type,
        List<String> machineNames,
        Set<Source> sources
) {
    public enum Source { STERN_IC, STERN_ARMY }

    public VenueOnMap {
        // Defensive copy + ensure non-null/immutable
        sources = sources == null ? EnumSet.noneOf(Source.class) : EnumSet.copyOf(sources);
        machineNames = machineNames == null ? List.of() : List.copyOf(machineNames);
    }

    public boolean isSternIc() { return sources.contains(Source.STERN_IC); }
    public boolean isSternArmy() { return sources.contains(Source.STERN_ARMY); }

    public int machineCount() { return machineNames.size(); }

    /** Return a new VenueOnMap with the given source added to the sources set. */
    public VenueOnMap withSource(Source extra) {
        if (sources.contains(extra)) return this;
        EnumSet<Source> ns = EnumSet.copyOf(sources);
        ns.add(extra);
        return new VenueOnMap(id, name, address, lat, lon, websiteUrl, type, machineNames, ns);
    }
}
