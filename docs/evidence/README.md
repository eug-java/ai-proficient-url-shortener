# Verification evidence

Captured locally before assessment submission.

## Commands

```bash
./mvnw clean verify | tee docs/evidence/mvn-verify.log
grep -R "Tests run:" target/surefire-reports | tee docs/evidence/surefire-summary.txt
cp -R target/site/jacoco docs/evidence/jacoco
```

## Latest captured run (2026-07-23)

| Check | Result |
|---|---|
| Maven | Wrapper `./mvnw` (Apache Maven 3.9.x via wrapper) |
| Build | **BUILD SUCCESS** |
| Tests | **33** run, **0** failures, **0** errors, **0** skipped |
| Failsafe | Not used (all tests via Surefire / `*Test`) |
| JaCoCo gate | Line coverage ≥ 80% — **passed** |
| JaCoCo instructions | **89%** (158 of 1,513 missed) — see `jacoco/index.html` |

Artifacts in this directory:

- `mvn-verify.log` — full Maven verify output
- `surefire-summary.txt` — per-class `Tests run:` lines
- `failsafe-summary.txt` — empty / N/A note
- `jacoco/` — HTML coverage report (open `jacoco/index.html`)

Optional (manual):

- Screenshot of green GitHub Actions workflow
- `curl` transcripts of create → Location → redirect → analytics

Do not commit secrets. Logs must not contain database passwords.
