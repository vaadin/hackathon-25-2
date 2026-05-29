# Hackathon 25.2 — Stern Pinball Map

A Vaadin Platform 25.2 application that shows the user's surroundings on a map
and pins the nearest Stern Insider Connected venue, with the names of every
tournament/leaderboard currently running there. Data comes straight from
Stern's own backend, discovered by reverse-engineering the **Stern IC** iOS
app.

## What you see when you run it

1. Browser asks for your GPS location — grant it.
2. Map recenters on you and drops a "You" marker.
3. We call Stern's public `nearby_leaderboards` endpoint with your
   coordinates and plot a marker for the closest venue.
4. Sidebar shows the venue's name, type (Arcade, Brewery, Restaurant, …),
   distance from you, full address, the active tournament names, and a link to
   the venue's website.
5. Clicking the sidebar card pans the map to the venue.

## Reverse-engineering the Stern IC app

The Stern IC app at `/Applications/Stern IC.app` is a Flutter iOS app
(bundle id `com.stern.insiderconnected.ios`). `strings` against
`Wrapper/Runner.app/Frameworks/App.framework/App` (the AOT-compiled Dart
binary) gives us:

- API hosts `api.{dev,int,stg,prd}.sternpinball.io` (plus the
  shadow CMS host `cms.prd.sternpinball.io` that serves the same data)
- Web frontend `insider.sternpinball.com`
- Deep-link scheme `insider-connected://`
- Auth surface fragments: `/portal/user_auth/`, `/token/refresh/`, etc.

The full nearby-locations endpoint isn't visible as one string in the binary
(Flutter builds URLs at runtime from fragments). We cross-checked against the
companion project [`stern-home-leaderboards`](../stern-home-leaderboards)
whose [`STERN_API_REFERENCE.md`](../stern-home-leaderboards/STERN_API_REFERENCE.md)
already maps every endpoint of the Stern API. Auth + WebClient header set were
lifted from that project's `SternAuthService`. Credentials come from the same
`~/stern-home-leaderboards.properties` file as that project (we just import it
via `spring.config.import` so the file is reused as-is).

### The endpoint we use

```
GET https://cms.prd.sternpinball.io/api/v1/portal/nearby_leaderboards/?latitude=<lat>&longitude=<lon>
```

Public — no auth required. Returns leaderboards (tournaments / competitions)
sorted by distance from the supplied coordinates. Each row carries:

- `name` — the *tournament* name (e.g. "Pinball Wizards"), **not** the
  pinball machine model
- `lb_location` — the venue: name, full address, latitude/longitude as
  strings, type ("Arcade", "Brewery", …), website URL
- `distance` — kilometers from your coords

We dedupe rows by `lb_location.pk` to collapse the many tournaments per venue
into a single map marker.

### Why the demo focuses on just the closest venue

`/nearby_leaderboards/` returns *tournaments*, not *venues*. A busy arcade
like Gamestate Potsdamer Platz Berlin has accumulated **1500+ leaderboard
rows** (active weekly tournaments running back to 2022), and they all sort
ahead of any other venue in distance order. Pages 1–150 are all the same
venue. So pagination doesn't let us cheaply reach the next venue — we'd have
to walk thousands of rows.

### Auth: dynamic Next-Action discovery

Stern's login is a Next.js *Server Action* — a `POST /login` with a
content-hashed `Next-Action` header. The companion `stern-home-leaderboards`
project hardcodes the hash that was current when it was written
(`9d2cf818afff9e2c69368771b521d93585a10433`); that hash has since rotated
(Next.js re-hashes server actions every time the bundle is rebuilt) and now
returns the plain login *page* instead of the action response, so the
companion project's auth no longer works either — confirmed by running it
side-by-side against the same credentials.

To stay robust against future rotations, `SternAuthService.resolveLoginActionHash()`
discovers the current hash dynamically:

1. `GET https://insider.sternpinball.com/login`
2. Enumerate every `/_next/static/chunks/*.js` referenced by the page
3. Scan each chunk for the fingerprint
   `createServerReference)("<40-hex>",…,"performLogin")`
4. Use that hash for the actual login POST

At build time this resolved to `6019d9ac959a924fb98bf8bca486c1f893b70dcdce`
(in chunk `00xk5vvc4jxa6.js`) and the JWT + refresh-token + user-id cookies
came back fine. The hash is cached for the lifetime of the service and
re-resolved on demand if needed.

### Why we still don't list multiple machines per venue

`/game_locations/` rejects every HTTP method (405) on a plain `GET …/` and
on `…/{id}/` even with the Bearer token — so it's not actually a listing
endpoint despite what the companion project's `STERN_API_REFERENCE.md`
implies. The truly public venue-list endpoint that the Stern app uses for
its "Find a Machine" feature seems to live behind a Next.js server action
on the web frontend (not a REST endpoint on the backend); identifying that
action is a follow-up. Meanwhile we surface what `/nearby_leaderboards/`
unambiguously gives us: the closest Stern Insider Connected venue and the
list of tournaments currently running there.

## Architecture

```
com.infraleap.sternmap
├── Application.java                       Spring Boot + @StyleSheet(Lumo.STYLESHEET) + AppShellConfigurator
├── config/SternProperties.java            @ConfigurationProperties(prefix = "leaderboards")
├── stern/
│   ├── domain/
│   │   ├── NearbyLeaderboardsResponse     {count, next, previous, results}
│   │   ├── Leaderboard                    {pk, name, type, distance, lb_location}
│   │   ├── LbLocation                     venue with lat/lng as strings + address fields
│   │   └── PinballSpot                    view-model: dedupes leaderboards per venue
│   └── service/
│       ├── SternAuthService               lifted login flow (Next.js server action + Bearer cookie)
│       ├── SternLocationService           WebClient call to /nearby_leaderboards/, paginates+dedupes
│       └── SternSmokeTest                 CommandLineRunner — calls the API at startup so you
│                                          can verify the integration without opening a browser
└── ui/view/MapView.java                   @Route(""), browser geolocation → server → markers
```

## Running it

The properties file the app reads is the same one
`stern-home-leaderboards` uses, so you don't need to copy anything:

```properties
# ~/stern-home-leaderboards.properties
server.port=8888
leaderboards.stern-username=...
leaderboards.stern-password=...
```

Then:

```bash
mvn spring-boot:run
# wait for "Application running at http://localhost:8888/"
# open that URL → grant geolocation
```

On startup you'll see something like:

```
Stern smoke test — querying nearby_leaderboards around (52.505, 13.3805)
Stern returned 50 leaderboards across 5 page(s) → 1 unique venues (closest: Gamestate Potsdamer Platz Berlin @ 0.33)
Stern smoke test OK — closest 1 venues:
  - Gamestate Potsdamer Platz Berlin (Arcade) at Berlin  [0.33 km, 50 active tournament(s)]
```

That confirms the reverse-engineered path is live before you even open the
browser.

## Version note

The hackathon rules pin Platform `25.2.0-beta1`. At build time that version
hadn't been published to `https://maven.vaadin.com/vaadin-prereleases/` —
latest published 25.2 prerelease was `25.2.0-alpha8`. The `pom.xml` pins
`alpha8`; bump to `-beta1` once it ships.

## Hackathon meta

- Slack: `#hackathon-25-2`
- Branch: `enver-hackathon`
- Map component is commercial; uses the developer license already on
  `enver@vaadin.com`'s machine.
