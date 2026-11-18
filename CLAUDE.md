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
  försöken och startar `CaptureService` när gränsen (Store.threshold, 1–5) nåtts. `onPasswordSucceeded` nollställer.
- `CaptureService` (förgrundstjänst, `foregroundServiceType="camera"`): tar Store.burst (1–3) kort via `CameraSnap`,
  sparar som JPEG, kör valfri `Mailer.send` på egen tråd, stänger sig själv. Aviseringen hålls minimal (IMPORTANCE_MIN,
  VISIBILITY_SECRET) – Android kräver att den visas.
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
- Android kräver en synlig (men diskret) avisering medan kameran används i förgrundstjänsten.

## Att göra / ej gjort

- Inte provbyggt (ingen SDK lokalt); första riktiga provet är CI-bygget efter första push. Bekräfta att android-mail
  länkar utan packaging-krockar och att Camera2-koden tar ett kort på en riktig telefon.
- Ingen ljudinspelning ska läggas till (Dicks fråga om det avböjdes).
