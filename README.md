# Contacts: generic bottom sheets

Course exercise: `CountryCodeBottomSheet` and `MessengersBottomSheet` reuse
`CommonBottomSheet<T>`, preserving their option content, multi-selection,
callbacks and dismissal behavior.

## Run

Open the project in Android Studio. Use JDK 21 and Android SDK 35; put the
local SDK path in `local.properties` (Android Studio creates it automatically).

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

With an Android emulator or device connected:

```sh
./gradlew :app:connectedDebugAndroidTest
```

The UI tests use deterministic contacts to check selection persistence,
removal, combined filters and sorting through the real screen. They do not
read the device's personal contacts.
