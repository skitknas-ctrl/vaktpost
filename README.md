# Vaktpost

En liten Android-app som tar ett foto med framkameran när någon skriver fel upplåsningskod på din telefon.
Korten sparas privat i appen och kan valfritt mejlas till dig. Ingen ljudinspelning, inga dolda funktioner mot
andra appar – bara "vem försökte låsa upp min telefon".

## Så funkar det

1. Öppna appen och ge **kamera-tillstånd**.
2. Tryck **Aktivera enhetsadministratör** och godkänn. Den enda rättighet appen ber om är att få veta när fel
   upplåsningskod skrivs in (`watch-login`). Appen kan inte låsa, radera eller styra telefonen.
3. Tryck **Slå på skyddet**.
4. Prova med **Ta ett testkort nu** så ser du att kameran fungerar och att kortet dyker upp i galleriet.

När skyddet är på tas ett foto varje gång någon skriver fel kod (eller efter N fel i rad, om du ställt in det).
Korten visas i appen med tidsstämpel; tryck på ett kort för att se det i full storlek, dela eller radera det.

### Fel fingeravtryck eller ansikte

Android berättar **inte** för appar när någon misslyckas med fingeravtryck eller ansikte på låsskärmen – det
är en medveten säkerhetsspärr i systemet. Bara fel **kod/mönster/lösenord** rapporteras till appar. (En envis
inkräktare tvingas dock till koden efter ett par misslyckade fingeravtryck, och då fångas hen.)

För att ändå fånga fel finger/ansikte direkt kan Vaktpost ta ett kort **varje gång skärmen tänds medan
telefonen är låst** (slås på/av under **Inställningar**, på som standard). För att du inte ska fyllas av
selfies på dig själv: om du själv låser upp inom **30 sekunder** raderas det kortet automatiskt. Ett kort från
någon som *inte* lyckas låsa upp blir alltså kvar, medan dina egna försvinner.

### Valfri e-post

Under **Inställningar** kan du fylla i ett SMTP-konto så att korten också mejlas till dig. Lämnar du det tomt
sparas korten bara i appen. För Gmail: server `smtp.gmail.com`, port `587`, och ett **app-lösenord** (inte ditt
vanliga lösenord).

## Begränsningar

- **iPhone finns inte.** Appen är bara för Android; Apple tillåter inte motsvarande.
- **Vissa telefoner tillåter inte att kameran öppnas medan skärmen är låst.** Då misslyckas kortet tyst. Det är
  en begränsning i Android/tillverkaren, inte i appen. Testa med "Ta ett testkort nu" och genom att själv skriva
  fel kod på låsskärmen.
- Android kräver att en app som använder kameran i bakgrunden visar en kort avisering. Den hålls så diskret som
  möjligt och försvinner när kortet är taget.

## Lagligt

Appen är tänkt som stöldskydd för din egen telefon: den tar ett foto av den som försöker låsa upp. Den spelar
**inte** in ljud och är inte byggd för att i smyg övervaka namngivna personer. Använd den så.

## Bygge

Ingen Android SDK behövs lokalt – GitHub Actions bygger en APK vid varje push till `main`. Senaste testbygge:
`https://github.com/<ägare>/vaktpost/releases/latest/download/vaktpost-debug.apk`

Paketnamn: `com.gr8eam.vaktpost` (testbyggen `.debug`).
