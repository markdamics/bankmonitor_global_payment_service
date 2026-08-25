# AI-használat dokumentációja

## Eszközkészlet

- Claude Chat
- Claude Code via VS code extension

## Munkamenetek

1/Kérés: "Setup the project skeleton with Java 21 and Spring boot and typescript with Vite, React. Design the readme with only paragrahs titles based on the task pdf"

1/Válasz: Zip fájlban be lett rakva a projekt skeleton, ami futtatható volt, meg a readme-ben ott voltak az elvárt bekezdések címekkel későbbi kitöltésre. Ezen nem kellett változtatnom.

---

2/Kérés: "Add exception handling to the services"

2/Válasz: Létrehozott runtime típusu exception-ket, valamint a két service-ben hozzáadott egyes metódusokhó exception handlinget. Egy global rest api exception handlert is létrehozott. Ezen ezen a ponton nem kellett változtatni

---

3/Kérés: "Add logging to the services"

3/Válasz: A service-khez hozzá lettek adva a logok, ebben az esetben csak info és debug logok lettek hozzá adva

---

4/Kérés: "Do the todo item number 5, Implement the mock API based on the assignment pdf"

4/Válasz: Létrehozott egy mockolt, „flaky” külső árfolyam API-t (véletlenszerű 503-ak, 100–900 ms
késleltetés) egy reziliens klienssel (retry exponenciális backoff-fal, timeout-tal), majd ezt bekötötte
a TransferService-be a valódi devizakonverzióhoz. Lásd: [exchangerate/](backend/src/main/java/hu/bankmonitor/paymentservice/exchangerate/).

---

5/Kérés: "In the frontend create the accounts page with a ccounts table, adding new account action, write the backend endpoints calling function in client.ts and style the page"

5/Válasz: Elkészítette a Számlák képernyőt (lista + új számla létrehozása form), bekötve a backend
`/api/accounts` végpontjaira, plusz a szükséges backend CORS engedélyt a Vite dev szerver origin-jéhez.[AccountsPage.tsx](frontend/src/pages/AccountsPage.tsx). - nem kellett valtoztatnom semmin

---

6/Kérés: "Create the transfers page with a transfers table, write the backend endpoints calling function in client.ts and style the page"

6/Válasz: Elkészítette a Tranzakciók képernyőt: lekéri a `/api/transfers` és `/api/accounts` végpontokat,
a számla-azonosítókat tulajdonos névre oldja fel, devizakonverziós utalásnál nyíllal jelzi az
összeg-átváltást és az árfolyamot, a `COMPLETED`/`FAILED` státuszt pedig színes jelölővel jeleníti meg.
Lásd: [TransactionsPage.tsx](frontend/src/pages/TransactionsPage.tsx). - nem kellett valtoztatnom semmin

---

7/Kérés: "Create the transfer page with selectable source and target accounts, an input field for the amounnt, write the backend endpoints calling function in client.ts and style the page"

7/Válasz: Elkészítette az Utalás képernyőt: forrás- és célszámla-választó (egyenleggel feltüntetve),
összeg megadása, a kérés idempotencia-kulcsát a kliens generálja (`crypto.randomUUID()`). A `POST
/api/transfers` válasza alapján zölden a sikeres (devizakonverzió esetén az árfolyammal együtt), pirosan
a fedezethiány miatt sikertelen kimenetet jelzi. A szám- és dátumformázókat pedig egy közös `utils/format.ts`-be.
[TransferPage.tsx](frontend/src/pages/TransferPage.tsx). - nem kellett valtoztatnom semmin

---

8/Kérés: "Create the tests for backend nd , todo 10"

8/Válasz: Backend: unit tesztek (`AccountServiceTest`, `TransferServiceTest`, Mockito) az üzleti
szabályokra, plusz integrációs tesztek MockMvc-vel és valódi `ExecutorService`-szel indított
párhuzamos szálakkal a konkurencia- és idempotencia-védelemre (`TransferIntegrationTest`), illetve
a mockolt árfolyam API reziliencia-logikájára valódi futó szerverrel
(`ExchangeRateClient*IntegrationTest`) — összesen 26 teszt. Útközben talált és kijavított egy Spring
Boot 4-es csomagáthelyezést (`AutoConfigureMockMvc` új helye, külön `spring-boot-webmvc-test` függőség
kellett hozzá). Frontend: Vitest + Testing Library beállítása (eddig nem volt bekötve), majd 20
komponens teszt mindhárom képernyőre, a `client.ts` hívásait mockolva. Egy valós Vite/Vitest
verzióütközést (`vite.config.ts` típushiba) oldott meg. Az automatizált, repóba bekötött E2E-t
(Playwright Test futtatóval) tudatosan kihagyta — ezt a README Tesztelés szekciója rögzíti indoklással.
Lásd: [TransferServiceTest.java](backend/src/test/java/hu/bankmonitor/paymentservice/transfer/TransferServiceTest.java),
[TransferIntegrationTest.java](backend/src/test/java/hu/bankmonitor/paymentservice/transfer/TransferIntegrationTest.java).
