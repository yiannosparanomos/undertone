# Release checklist

## Before the first release (one time)

- [ ] Google Play developer account created and identity verified
- [ ] Repo Settings → Pages: deploy from `main`, folder `/docs`; check the privacy policy URL
      works: https://yiannosparanomos.github.io/undertone/privacy-policy
- [ ] Upload key created **on your own machine** (`keytool -genkeypair ...`), stored in a
      password manager, never committed. Play App Signing enabled.
- [ ] Store listing filled from `docs/play-store/listing.md`, graphics uploaded
- [ ] Data safety, content rating and app access filled from `docs/play-store/`
- [ ] Closed test track created; 14 testers invited (12 must stay opted in for 14 days)

## Every release

1. [ ] `main` is green: CI and the latest emulator run
2. [ ] Google Play's current target-API requirement is met (`targetSdk` in the version catalog)
3. [ ] `versionCode` +1 and `versionName` bumped in `app/build.gradle.kts`
4. [ ] `CHANGELOG.md`: Unreleased moved under the new version
5. [ ] Permissions in the merged manifest match `docs/play-store/permissions.md`; no
       `INTERNET` (CI checks this too)
6. [ ] Real-phone "Release test run" issue completed (`docs/testing/device-checklist.md`)
7. [ ] Tag `vX.Y.Z` on main (triggers emulator tests)
8. [ ] Build the signed bundle locally: `./gradlew bundleRelease` with your upload key
9. [ ] Upload to the closed testing track first; promote to production after testers confirm
10. [ ] Release notes from the changelog
