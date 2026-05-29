package com.infraleap.sternmap.ui.view;

import com.infraleap.sternmap.stern.domain.PinballSpot;
import com.infraleap.sternmap.stern.service.SternLocationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.map.Map;
import com.vaadin.flow.component.map.configuration.Coordinate;
import com.vaadin.flow.component.map.configuration.feature.MarkerFeature;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route("")
@PageTitle("Stern Pinball Near Me")
public class MapView extends HorizontalLayout {

    private final SternLocationService locationService;
    private final Map map = new Map();
    private final VerticalLayout sidebar = new VerticalLayout();
    private final Div statusLine = new Div();
    private final VerticalLayout list = new VerticalLayout();

    public MapView(SternLocationService locationService) {
        this.locationService = locationService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // ---- Sidebar ----
        sidebar.setPadding(true);
        sidebar.setSpacing(false);
        sidebar.setWidth("360px");
        sidebar.getStyle()
                .set("background", "var(--vaadin-background-container, #f5f5f5)")
                .set("border-right", "1px solid var(--vaadin-border-color, #ddd)")
                .set("overflow", "hidden");
        sidebar.setHeightFull();

        H1 title = new H1("Stern Pinball Near Me");
        title.getStyle().set("font-size", "1.4rem").set("margin", "0 0 0.25rem 0");
        Paragraph sub = new Paragraph("Closest public Stern Insider Connected venues, with active tournaments");
        sub.getStyle().set("margin", "0 0 1rem 0").set("color", "var(--vaadin-text-color-secondary, #666)");

        statusLine.setText("Requesting your location…");
        statusLine.getStyle().set("font-size", "0.875rem").set("color", "var(--vaadin-text-color-secondary, #666)");

        list.setPadding(false);
        list.setSpacing(false);
        list.setWidthFull();
        Scroller scroller = new Scroller(list);
        scroller.setSizeFull();
        scroller.getStyle().set("margin-top", "0.75rem");

        sidebar.add(title, sub, statusLine, scroller);
        sidebar.expand(scroller);

        // ---- Map ----
        map.setSizeFull();
        // Default view: world-ish, will recenter once we have the user's location.
        map.setCenter(new Coordinate(10.0, 50.0));
        map.setZoom(3);

        add(sidebar, map);
        setFlexGrow(0, sidebar);
        setFlexGrow(1, map);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Ask the browser for the user's location. The Promise resolves on the
        // client and Vaadin pipes the result back here.
        attachEvent.getUI().getPage()
                .executeJs("return new Promise((resolve) => {" +
                        "  if (!navigator.geolocation) { resolve(''); return; }" +
                        "  navigator.geolocation.getCurrentPosition(" +
                        "    p => resolve(p.coords.latitude + ',' + p.coords.longitude)," +
                        "    e => resolve('err:' + e.code + ':' + e.message)," +
                        "    {enableHighAccuracy: true, timeout: 10000, maximumAge: 60000}" +
                        "  );" +
                        "})")
                .then(String.class, this::onLocation);
    }

    private void onLocation(String result) {
        if (result == null || result.isBlank()) {
            statusLine.setText("Geolocation unavailable in this browser.");
            Notification.show("Geolocation not available", 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        if (result.startsWith("err:")) {
            statusLine.setText("Couldn't get your location — " + result.substring(4));
            Notification.show("Couldn't get your location: " + result.substring(4),
                            6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        String[] parts = result.split(",");
        if (parts.length != 2) {
            statusLine.setText("Bad geolocation payload: " + result);
            return;
        }
        double lat = Double.parseDouble(parts[0]);
        double lon = Double.parseDouble(parts[1]);
        statusLine.setText(String.format("You are at %.4f, %.4f — searching Stern API…", lat, lon));

        // Recenter map + drop a "you are here" marker.
        map.setCenter(new Coordinate(lon, lat));
        map.setZoom(11);
        MarkerFeature you = new MarkerFeature(new Coordinate(lon, lat));
        you.setText("You");
        map.getFeatureLayer().addFeature(you);

        // Fetch nearby venues from Stern and plot them.
        List<PinballSpot> spots = locationService.findNearby(lat, lon, 40);
        renderSpots(spots);
    }

    private void renderSpots(List<PinballSpot> spots) {
        list.removeAll();
        if (spots.isEmpty()) {
            statusLine.setText("Stern API returned no nearby venues.");
            return;
        }
        statusLine.setText("Showing " + spots.size() + " Stern pinball venues near you.");

        for (int i = 0; i < spots.size(); i++) {
            PinballSpot spot = spots.get(i);
            MarkerFeature marker = new MarkerFeature(
                    new Coordinate(spot.location().lon(), spot.location().lat()));
            marker.setText(spot.location().name());
            map.getFeatureLayer().addFeature(marker);
            list.add(buildCard(i + 1, spot));
        }
    }

    private Component buildCard(int index, PinballSpot spot) {
        Div card = new Div();
        card.getStyle()
                .set("padding", "0.75rem")
                .set("border-bottom", "1px solid var(--vaadin-border-color, #e5e5e5)")
                .set("cursor", "pointer");
        card.getElement().addEventListener("click", e -> {
            map.setCenter(new Coordinate(spot.location().lon(), spot.location().lat()));
            map.setZoom(15);
        });

        H4 name = new H4("#" + index + " — " + spot.location().name());
        name.getStyle().set("margin", "0 0 0.25rem 0").set("font-size", "1rem");
        card.add(name);

        if (spot.location().locationTypeName() != null && !spot.location().locationTypeName().isBlank()) {
            Span type = new Span(spot.location().locationTypeName());
            type.getElement().getThemeList().add("badge");
            type.getStyle().set("margin-right", "0.5rem").set("font-size", "0.75rem");
            card.add(type);
        }
        if (spot.distance() != null) {
            Span dist = new Span(String.format("%.1f km", spot.distance()));
            dist.getStyle().set("font-size", "0.75rem").set("color", "var(--vaadin-text-color-secondary, #666)");
            card.add(dist);
        }

        Paragraph addr = new Paragraph(spot.location().fullAddress());
        addr.getStyle().set("margin", "0.25rem 0").set("font-size", "0.85rem");
        card.add(addr);

        if (!spot.leaderboardNames().isEmpty()) {
            int shown = Math.min(spot.leaderboardNames().size(), 4);
            String list = String.join(", ", spot.leaderboardNames().subList(0, shown));
            int rest = spot.leaderboardNames().size() - shown;
            if (rest > 0) list += " (+" + rest + " more)";
            Span boards = new Span(spot.leaderboardNames().size()
                    + " active tournament" + (spot.leaderboardNames().size() == 1 ? "" : "s")
                    + ": " + list);
            boards.getStyle().set("font-size", "0.75rem")
                    .set("color", "var(--vaadin-text-color-secondary, #888)");
            card.add(boards);
        }

        if (spot.location().websiteUrl() != null && !spot.location().websiteUrl().isBlank()) {
            Anchor link = new Anchor(spot.location().websiteUrl(), "Website ↗");
            link.setTarget("_blank");
            link.getStyle().set("display", "block").set("margin-top", "0.25rem").set("font-size", "0.8rem");
            card.add(link);
        }
        return card;
    }
}
