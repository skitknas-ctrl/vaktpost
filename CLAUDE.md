# Vaktpost – Android-app (stöldskydd med framkamera)

Tar ett foto med framkameran när någon skriver fel upplåsningskod. Korten sparas privat i appen (filesDir/shots)
och kan valfritt mejlas. INGEN ljudinspelning och inget försök att dölja kamerabruket för andra appar – byggd som
stöldskydd, inte som dold övervakning av namngivna personer (Dick 2026-11-18, efter att ha avböjt den delen).

- Ägare: Dick (GitHub `skitknas-ctrl`). Repo-förslag: `vaktpost`, publikt. Paketnamn `com.gr8eam.vaktpost` (testbyggen `.debug`).
- Arbetskopia: `P:\Appar\Vaktpost\`. Ren Kotlin (ingen WebView, inga layout-XML – gränssnittet byggs i kod).
- Gränssnittet och all text till användaren är på svenska. Kommentarer i koden på svenska. Svara Dick på svenska.
- Ingen JDK/Android SDK på Dicks dator. GitHub Actions bygger APK vid push (samma uppsättning som SpringTrain/Kalejdo):
  testbygge med publik debug-nyckel, rullande release `0.1.0-test.<n>`, stabil länk `.../releases/latest/download/vaktpost-debug.apk`.
  Play-AAB byggs med `workflow_dispatch` eller `v*`-tagg och secrets RELEASE_KEYSTORE_*.

## Så hänger det ihop

- `AdminReceiver` (DeviceAdminReceiver, policy `watch-login` i `res/xml/device_admin.xml`): `onPasswordFailed` räknar
  försöken och ber `WatchService` ta kort (action FAIL) när gränsen (Store.threshold, 1–5) nåtts. `onPasswordSucceeded`
  nollställer; `onDisabled` stänger även WatchService. OBS: `onPasswordFailed` fyr­as BARA av fel kod/mönster/lösenord –
  Android rapporterar INTE fel fingeravtryck/ansikte på låsskärmen till appar (verifierat 2026-11-18; AOSP räknar
  biometrifel i egen räknare, separat fallback-tröskel innan PIN tvingas fram).
- `WatchService` (KVARVARANDE förgrundstjänst, `foregroundServiceType="camera"`, START_STICKY): körs så länge skyddet är
  på. (a) action FAIL → serie "fail"-kort + valfri mail. (b) dynamiskt registrerad receiver för ACTION_SCREEN_ON +
  ACTION_USER_PRESENT: vid skärmtändning med låst keyguard tas ETT "wake"-kort (enda sättet att fånga fel finger/ansikte;
  gate: Store.screenTrigger, default på; 8 s debounce; en kamera i taget via AtomicBoolean). Vid upplåsning (USER_PRESENT)
  raderas "wake"-kort yngre än Store.WAKE_KEEP_MS (30 s) – egna selfies försvinner, inkräktarens blir kvar. Eftersom
  tjänsten redan är en aktiv kamera-FGS slipper skärmtriggern "starta kamera-FGS från bakgrunden"-spärren på Android 14+.
  Startas av MainActivity (slå på + onResume-säkring) och BootReceiver (om armed; kan nekas efter omstart → öppna appen).
- `CaptureService` (kortlivad förgrundstjänst, `foregroundServiceType="camera"`): används nu bara för "Ta ett testkort nu";
  tar Store.burst kort, stänger sig själv. Kortfilerna taggas i filnamnet: `fail-…`/`wake-…` (Store.KIND_*). Testkort = fail.
- `CameraSnap`: Camera2, framkamera, ImageReader (JPEG) utan förhandsvisning, egen HandlerThread, 5 s säkerhetsventil,
  onDone exakt en gång. Vissa OEM tillåter inte kameran medan skärmen är låst → onDone(null), tyst miss (dokumenterat).
- `Store`: SharedPreferences (armed, threshold, burst, mail*, fails) + filhantering av korten (filesDir/shots).
- `Mailer`: valfri SMTP via `com.sun.mail:android-mail:1.6.7` (paket `javax.mail`; `android-activation` för javax.activation).
  STARTTLS, eller SSL på port 465. Gmail kräver app-lösenord. META-INF-licensfiler exkluderas i packaging (javamails
  tjänstefiler måste dock följa med – exkludera inte META-INF brett).
- `MainActivity`: status (kamera/administratör/skydd), slå på/av, ge kamera-tillstånd, aktivera administratör
  (ACTION_ADD_DEVICE_ADMIN), "Ta ett testkort nu" (startar CaptureService direkt), Inställningar, galleri (3 kolumner,
  nedsamplade miniatyrer), radera alla. `SettingsActivity` (tröskel, burst, e-post), `ViewerActivity` (visa/dela via
  FileProvider `${applicationId}.files`/radera). POST_NOTIFICATIONS begärs på Android 13+.

## Begränsningar (dokumenterade för användaren)

- Bara Android. iPhone tillåter inte motsvarande.
- Bakgrundskamera på låsskärm är best-effort och varierar per Android-version/tillverkare.
- Fel fingeravtryck/ansikte går inte att fånga direkt (Android spärrar det) – fångas via skärmtändningstriggern + 30 s-regeln.
- Android kräver en synlig (men diskret) avisering medan kameran används i förgrundstjänsten.

## Att göra / ej gjort

- Inte provbyggt (ingen SDK lokalt); första riktiga provet är CI-bygget efter första push. Bekräfta att android-mail
  länkar utan packaging-krockar och att Camera2-koden tar ett kort på en riktig telefon.
- Ingen ljudinspelning ska läggas till (Dicks fråga om det avböjdes).
