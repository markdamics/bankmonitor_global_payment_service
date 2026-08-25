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
