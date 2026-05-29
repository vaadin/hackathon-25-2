package com.infraleap.sternmap.ui;

import com.vaadin.flow.component.map.configuration.style.Icon;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * SVG marker icons baked as inline {@code data:} URLs.
 * <ul>
 *   <li>{@link #youMarker()} — map-pin with person silhouette, for the user's GPS.</li>
 *   <li>{@link #sternIcMarker()} — top-down pinball-machine silhouette (red backbox,
 *       blue playfield with pop bumpers + silver ball + yellow flippers) for venues
 *       returned by Stern's native v2 endpoint.</li>
 *   <li>{@link #sternArmyMarker()} — gold/yellow pinball machine with a white star
 *       on the backbox, for venues from {@code sternpinball.com/stern-army-locator/}
 *       (Pinball Map filtered to {@code is_stern_army=true}).</li>
 * </ul>
 * All icons use anchor at horizontal-centre / vertical-bottom so the visual point
 * sits on the geographic coordinate.
 */
public final class MapIcons {

    private MapIcons() {}

    private static final String YOU_SVG =
            "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 40' width='32' height='40'>"
            + "<path d='M16 2C8.27 2 2 8.27 2 16c0 10.5 14 22 14 22s14-11.5 14-22c0-7.73-6.27-14-14-14z'"
            + " fill='#1976d2' stroke='white' stroke-width='2'/>"
            + "<circle cx='16' cy='13' r='3.5' fill='white'/>"
            + "<path d='M10.5 23c0-3 2.5-5 5.5-5s5.5 2 5.5 5z' fill='white'/>"
            + "</svg>";

    /**
     * Top-down pinball-machine silhouette. Reads as a pinball table: backbox at the
     * top with a light, rounded playfield body, three pop bumpers and a silver ball
     * in the middle, and yellow flippers angled at the bottom.
     */
    private static String pinballMachineSvg(String cabinetFill, String backboxFill,
                                            String bulbFill, String accentFill,
                                            String starSvg) {
        return "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 44' width='32' height='44'>"
                + "<ellipse cx='16' cy='42' rx='8' ry='1.5' fill='black' opacity='0.18'/>"
                // Backbox (head)
                + "<rect x='8' y='2' width='16' height='7' rx='1.2' fill='" + backboxFill + "' stroke='#1a1a1a' stroke-width='1'/>"
                // Bulb / star on the backbox
                + (starSvg != null && !starSvg.isEmpty()
                        ? starSvg
                        : "<circle cx='16' cy='5.5' r='1.4' fill='" + bulbFill + "'/>")
                // Cabinet body — slight inward taper toward the bottom, rounded at the apron
                + "<path d='M7 9 L25 9 L26 33 Q26 38 22 38 L10 38 Q6 38 6 33 Z'"
                + " fill='" + cabinetFill + "' stroke='#1a1a1a' stroke-width='1'/>"
                // Pop bumpers
                + "<circle cx='12' cy='15' r='1.6' fill='" + accentFill + "' stroke='#1a1a1a' stroke-width='0.4'/>"
                + "<circle cx='20' cy='15' r='1.6' fill='" + accentFill + "' stroke='#1a1a1a' stroke-width='0.4'/>"
                + "<circle cx='16' cy='20' r='1.6' fill='" + accentFill + "' stroke='#1a1a1a' stroke-width='0.4'/>"
                // Ball (silver)
                + "<circle cx='16' cy='27' r='1.9' fill='#eceff1' stroke='#1a1a1a' stroke-width='0.4'/>"
                + "<circle cx='15.3' cy='26.3' r='0.55' fill='white'/>"
                // Flippers — angled inward, meeting at the drain
                + "<path d='M9 33 L14.5 35.5 L14.5 33.8 Z' fill='" + accentFill + "' stroke='#1a1a1a' stroke-width='0.4'/>"
                + "<path d='M23 33 L17.5 35.5 L17.5 33.8 Z' fill='" + accentFill + "' stroke='#1a1a1a' stroke-width='0.4'/>"
                + "</svg>";
    }

    /** Five-point white star centred on the backbox, used for the Stern Army variant. */
    private static final String STAR =
            "<polygon points='16,2.8 17,5 19.3,5 17.4,6.5 18.1,8.8 16,7.4 13.9,8.8 14.6,6.5 12.7,5 15,5'"
            + " fill='white' stroke='#1a1a1a' stroke-width='0.4'/>";

    public static Icon youMarker() {
        return iconFromSvg(YOU_SVG);
    }

    public static Icon sternIcMarker() {
        // Blue cabinet, red backbox, yellow accents — the classic Stern colour scheme.
        return iconFromSvg(pinballMachineSvg("#1565c0", "#e53935", "#ffeb3b", "#ffeb3b", null));
    }

    public static Icon sternArmyMarker() {
        // Gold cabinet + black backbox with a white star — Stern Army branding palette.
        return iconFromSvg(pinballMachineSvg("#f9a825", "#212121", "#ffeb3b", "#212121", STAR));
    }

    private static Icon iconFromSvg(String svg) {
        String b64 = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        Icon.Options opts = new Icon.Options();
        opts.setSrc("data:image/svg+xml;base64," + b64);
        opts.setAnchor(new Icon.Anchor(0.5, 1.0));
        opts.setScale(1.0);
        return new Icon(opts);
    }
}
