# Bankmonitor Global Payment Service

## AI használat

**Claude chat** - ingyenes változat, Sonnet 5

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
| Adatbázis   | H2 (in-memory)                             |  |

Tervezett rétegek feature-önként: `*Controller` (REST) → `*Service` (üzleti logika, tranzakciókezelés) →
`*Repository` (Spring Data JPA) → `*Entity`/`*Dto`. Az `exchangerate` csomag felelős a mockolt külső
árfolyam API-ért és annak reziliens hívásáért; az `idempotency` csomag a kulcs-kezelésért; az `integration`
csomag a sikeres utalások külvilág felé történő továbbításáért (ld. [Edge case-ek](#edge-case-ek)).

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
3. A backend domain rétegével kezdtem (Account → Transfer → Idempotency → ExchangeRate client →
   Integration event)

## Edge case-ek


## TODO-lista

1. **Backend** (Account, Transfer entitások, repository-k, H2)
2. **controller/service/repository rétegek**
3. **Idempotencia-kezelés**
4. **Mockolt árfolyam API + reziliencia** (retry/timeout) — ezután jöhet a devizakonverziós eset.
5. **Konkurencia-védelem** (zárolás) — miután a happy path stabil, mert így tesztelhető is izoláltan.
6. **Frontend: Számlák képernyő**.
7. **Frontend: Tranzakciók képernyő.**
8. **Frontend: Utalások képernyő.**
9. **Tesztek** (backend unit + integrációs a konkurenciára és idempotenciára; frontend komponens/E2E) —
   párhuzamosan íródnak az egyes lépésekkel, nem a végén egyben.
10. **Nem implementált funkctiók amikre nem volt időm vagy csak production verzióban implementálnám leírása** - ??????

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
