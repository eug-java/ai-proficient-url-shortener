# GeoIP (MaxMind)

Mount `GeoLite2-Country.mmdb` into this directory (or set `GEO_MAXMIND_HOST_PATH`).

The app reads `GEO_MAXMIND_DB` (default `/data/GeoLite2-Country.mmdb`). If the file is
missing, redirects still work and country falls back to `CF-IPCountry` / `X-Country-Code`.
