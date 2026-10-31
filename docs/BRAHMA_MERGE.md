# Brahma native merge

## Ported natively in the APK

- `DeviceController` uses Android AudioManager, Settings.System, CameraManager torch, ringer mode, AlarmClock intents, BatteryManager, and settings panels.
- `CommandRouter` recognizes English and Malayalam command keywords and routes app launch, accessibility actions, calls, SMS, WhatsApp share, media, and device controls. Unmatched text remains available to the AI brain.
- `NotificationSentinelService` stores the most recent 100 notifications and can speak urgent/VIP alerts when enabled.
- `AssistantScreen` exposes default-assistant, accessibility, notification access, device test, saved Home Assistant, saved PC gateway, and provider settings.
- Provider configuration, identity, language mode, and mix mode are local JSON settings.
- The procedural avatar and lip-sync flow are native OpenGL ES 2.0/coroutines with no 3D dependency.

## Backend-only or future work

The blueprint's FastAPI concepts are reference-only here. This APK makes no Home Assistant or PC gateway network call. Cross-device synchronization, authenticated gateway transport, Discord bot webhooks, live wallpaper service, biometric sentinel lock, native document generation, and full smart-home action execution need additional backend/service modules.

## User-supplied access

Provider API keys, optional custom models/base URLs, Home Assistant URL/token, Android notification-listener access, accessibility access, overlay access, write-settings access, default-assistant selection, phone/SMS permissions, and any future PC gateway credentials must be supplied or approved by the user.
