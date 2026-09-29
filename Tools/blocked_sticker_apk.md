# Feature-branch debug APK

`.github/workflows/blocked-sticker-packs-apk.yml` packages the real Android app on
pushes to `tailgram/blocked-sticker-packs` and supports manual dispatch. It builds
an **arm64-v8a debug APK**, not a release-signed APK, and uploads only that APK as
`tailgram-blocked-sticker-packs-debug-arm64-v8a` (retained for seven days).

The APK is checked for its Android manifest, DEX code, Telegram arm64 native
library, and ZIP integrity before upload. Missing APKs fail the workflow. With
the injected ABI option, this AGP version writes the signed APK under
`TMessagesProj_App/build/intermediates/apk/debug`, not `build/outputs/apk`.

## Local equivalent

With JDK 21, the repository's Android SDK/NDK versions, and its submodules installed:

```sh
python3 Tools/test_blocked_sticker_packs.py
python3 Tools/test_verify_debug_apk.py
./gradlew :TMessagesProj_App:assembleDebug \
  -PstandaloneDebug=true -Pandroid.injected.build.abi=arm64-v8a
python3 Tools/verify_debug_apk.py TMessagesProj_App/build/intermediates/apk/debug/*-arm64-v8a.apk
```

## Limitations

- `standaloneDebug` is opt-in. Normal configured builds are unchanged.
- This mode uses standard Android debug signing and the existing `.beta`
  application-ID suffix. It is not signed with the production key.
- Maps configuration, Firebase configuration processing, and telemetry are
  disabled in this mode. No service credentials are fabricated or supplied by
  the workflow.
- Telegram sign-in requires valid developer API configuration. This workflow
  does not provide it; a credential-free APK is for build/UI inspection, not a
  production-ready distribution.
- The workflow does not create a GitHub Release or upload sources, signing
  material, or other build outputs.
