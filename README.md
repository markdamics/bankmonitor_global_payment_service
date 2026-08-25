# Bankmonitor Global Payment Service

## AI használat

**Claude chat, Claude code** - Sonnet 5

## Tartalomjegyzék

- [Architektúra és döntések](#architektúra-és-döntések)
- [Hogyan álltam neki](#hogyan-álltam-neki)
- [Edge case-ek](#edge-case-ek)
- [TODO-lista](#todo-lista)
- [Tesztelés](#tesztelés)
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
| Külső árfolyam API mockolása | Saját `/mock/exchange-rate` endpoint + `RestClient` | Valódi HTTP-hívást és reziliencia-logikát (retry/backoff/timeout) gyakorol, nem csak in-process szimulációt |

### Frontend

| Terület          | Döntés                          | Indoklás |
|-------------------|----------------------------------|----------|
| Nyelv/keretrendszer | React 19 + TypeScript          | Feladat által kötelező. |
| Build tool        | Vite                             | Gyors dev-szerver, minimális konfiguráció |
| Routing           | react-router-dom                 | Három elkülönült képernyő — kliensoldali routing egyszerű megoldás erre a méretre. |
| Állapotkezelés    | React beépített state vagy redux, most még nem eldöntött |  |
| Styling           | Sima CSS           | Külső UI library használata felesleges a feladat mérete miatt valamit AI-al készített css styling egyszerűbb és gyorsabb |
| Backend elérés dev módban | Spring `WebMvcConfigurer` CORS engedélyezés `/api/**`-re a Vite dev origin (5173) felé | Egyszerűbb, mint egy Vite dev-proxy, és ugyanaz a kliens kód működik dev és production build esetén is |

### Amit eldöntöttem

- **Utalás fedezethiány esetén** Annak ellenére, hogy az utalás nem lehetséges fedezethiány miatt, belekerül az   utalások táblába `Failed` státusszal azért, hogy ennek is legyen nyoma

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

- Saját magának küldött utalás
- Fedezethiány
- Idempotencia-kulcs újrafelhasználása azonos payloaddal
- Idempotencia-kulcs újrafelhasználása eltérő payloaddal
- Devizák eltérnek a két számla között
- Ugyanazt a számlát egyszerre érintő konkurens utalások
- Két konkurens kérés ugyanazzal, még nem látott idempotencia-kulccsal

## TODO-lista

1. **Backend** (Account, Transfer entitások) - done
2. **controller/service/repository rétegek** - Utalási logika implementálása - done
3. **Swagger és logolás** - done
4. **Idempotencia-kezelés** - done
5. **Mockolt árfolyam API + reziliencia** - retry/timeout - done
6. **Konkurencia-védelem** - done
7. **Frontend: Számlák képernyő** - done
8. **Frontend: Tranzakciók képernyő** - done
9. **Frontend: Utalások képernyő** - done
10. **Tesztek** (backend unit + integrációs a konkurenciára és idempotenciára; frontend komponens/E2E) —
   párhuzamosan íródnak az egyes lépésekkel, nem a végén egyben.
11. **RendszerIntegráció**
12. **Nem implementált funkctiók amikre nem volt időm vagy csak production verzióban implementálnám leírása** 
- ??????

## Tesztelés

**Backend:** Manuálisan, Swagger UI-n teszteltem

**Frontend:** mindhárom képernyőt (Számlák, Utalás, Tranzakciók) manuálisan, headless böngészőn
(Playwright) keresztül vezérelt forgatókönyvvel teszteltem, konzolhiba nélkül.

## Éles üzem

- **Idempotencia-kulcs azonnali lefoglalása:** jelenleg egy konkurens, ugyanazzal a (még nem látott)
  kulccsal érkező kérés csak a mentés pillanatában (az adatbázis unique constraint-jén) bukik el
  `409`-cel, nem a kérés legelején. Éles rendszerben egy külön, azonnal commitolt "lefoglalás" lépést
  (pl. saját idempotencia-kulcs tábla PENDING/COMPLETED/FAILED státusszal, vagy egy elosztott lock
  Redis-ben TTL-lel) vezetnék be, hogy a vesztes kérés ne is fusson bele a teljes üzleti logikába.
- **Megfigyelhetőség:** strukturált (JSON) logolás, metrikák (pl. Micrometer + Prometheus) és
  disztribúiós tracing hiányzik éles üzemhez.
- **Adatbázis:** H2 in-memory helyett egy valódi, replikált SQL adatbázis (pl. PostgreSQL) kellene,
  migrációkezeléssel
- **Autentikáció/autorizáció:** jelenleg bárki bármely számlát elérheti — éles rendszerben ez
  megköveteli a felhasználó-számla tulajdonjog ellenőrzését minden végponton.

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
