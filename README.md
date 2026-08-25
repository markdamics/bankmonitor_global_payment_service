# Bankmonitor Global Payment Service

## AI használat

**Claude chat, Claude code** - Sonnet 5

## Tartalomjegyzék

- [Architektúra és döntések](#architektúra-és-döntések)
- [Hogyan álltam neki](#hogyan-álltam-neki)
- [Edge case-ek](#edge-case-ek)
- [TODO-lista](#todo-lista)
- [Éles üzem](#éles-üzem)
- [Futtatás](#futtatás)

## Architektúra és döntések

### Backend

| Terület     | Döntés                                   | Indoklás |
|-------------|-------------------------------------------|----------|
| Nyelv       | Java 21                                    | A feladat követelménye miatt |
| Framework   | Spring Boot 4.1.0                          | A feladat követelménye miatt |
| Build tool  | Maven                                      | A feladat követelménye miatt valamint Gradle-ben nincs tapasztalatom|
| Adatbázis   | H2 (in-memory)                             |  Ezen a szinten, kis felhasználásra felesleges egy SQL alapú adatbázis|
| Loggolás    | SLF4J                                      | Alapértelmezett Spring boot logolás, nem volt indok mást használni  |
| API dokumentáció / teszt | springdoc-openapi (Swagger UI) | Gyors kézi API-tesztelés Postman/curl nélkül, automatikusan generált a controllerekből |
| Tesztelés | Swagger                                      | Könnyebb tesztelni a backend endpointokat frontend nélkül |

### Frontend

| Terület          | Döntés                          | Indoklás |
|-------------------|----------------------------------|----------|
| Nyelv/keretrendszer | React 19 + TypeScript          | Feladat által kötelező. |
| Build tool        | Vite                             | Gyors dev-szerver, minimális konfiguráció |
| Routing           | react-router-dom                 | Három elkülönült képernyő — kliensoldali routing egyszerű megoldás erre a méretre. |
| Állapotkezelés    | React beépített state vagy redux, most még nem eldöntött |  |
| Styling           | Sima CSS           | Külső UI library használata felesleges a feladat mérete miatt valamit AI-al készített css styling egyszerűbb és gyorsabb |

### Amit tudatosan elvetettem

- **Gradle a Mavennel szemben:** mivel a gradle-ben nincs taopasztalatom igy egyertelmű választás volt a Maven.

## Hogyan álltam neki

1. Elolvastam a feladatleírást, és szétválasztottam **funkcionális** (számla/utalás CRUD, idempotencia,
   külső árfolyam-integráció, rendszerintegráció, 3 frontend képernyő) és **nem-funkcionális**
   (konkurencia, tesztelés, AI-dokumentáció) követelményeket.
2. Felállítottam a projektvázat: buildelhető Spring Boot backend (Maven, H2, layered/feature csomagszerkezet)
   és buildelhető React+TS frontend (Vite, routing 3 képernyőre), hogy a további munka azonnal
   inkrementálisan, futtatható állapotban tartható legyen.
3. A backend domain rétegével kezdtem.

## Edge case-ek

- **Saját magának küldött utalás:** ugyanaz a forrás- és célszámla `400 Bad Request`-et eredményez.
- **Fedezethiány:** ha a forrásszámla egyenlege kevesebb az utalt összegnél, ez nem hibaként, hanem
  várt üzleti kimenetként kezelt — a próbálkozás `FAILED` állapotú Transfer rekordként mentésre kerül
  (a válasz `201 Created`, a `status` mezőt kell nézni), az egyenlegek pedig változatlanok maradnak.
- **Idempotencia-kulcs újrafelhasználása azonos payloaddal:** a korábbi Transfer rekord kerül visszaadásra
  új mentés/üzleti logika futtatása nélkül, `200 OK` válasszal (szemben az első, `201 Created` válasszal).
  Ez azonos módon működik `COMPLETED` és `FAILED` transferekre is.
- **Idempotencia-kulcs újrafelhasználása eltérő payloaddal** (más számla vagy összeg): `409 Conflict`,
  mivel ez feltehetően kliensoldali hiba (kulcsütközés), nem egy legitim ismétlés.
- **Devizák eltérnek a két számla között:** egyelőre `400 Bad Request` — a konverziós logika a
  mockolt árfolyam API-val együtt kerül implementálásra (ld. TODO-lista #5).

## TODO-lista

1. **Backend** (Account, Transfer entitások) - done
2. **controller/service/repository rétegek** - Utalási logika implementálása - done
3. **Swagger és logolás** - done
4. **Idempotencia-kezelés** - done
5. **Mockolt árfolyam API + reziliencia** (retry/timeout) — ezután jöhet a devizakonverziós eset.
6. **Konkurencia-védelem**
7. **Frontend: Számlák képernyő**.
8. **Frontend: Tranzakciók képernyő.**
9. **Frontend: Utalások képernyő.**
10. **Tesztek** (backend unit + integrációs a konkurenciára és idempotenciára; frontend komponens/E2E) —
   párhuzamosan íródnak az egyes lépésekkel, nem a végén egyben.
11. **Nem implementált funkctiók amikre nem volt időm vagy csak production verzióban implementálnám leírása** - ??????

## Éles üzem


## Futtatás

### Előfeltételek

- Java 21+
- Maven (a repóban nincs Maven Wrapper — helyi Maven 3.9+ szükséges)
- Node.js 20+ és npm

### Backend

```bash
cd backend
mvn spring-boot:run
```

A backend alapértelmezett címe: `http://localhost:8080`.

API kézi teszteléséhez Swagger UI érhető el: `http://localhost:8080/swagger-ui/index.html`
(nyers OpenAPI leírás: `http://localhost:8080/v3/api-docs`).

Tesztek futtatása:

```bash
cd backend
mvn test
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

A dev szerver alapértelmezett címe: `http://localhost:5173`

Build és tesztek:

```bash
cd frontend
npm run build
npm run test
```
