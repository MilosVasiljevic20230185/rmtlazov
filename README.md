# Lažeš

Multiplayer kartaška igra — pojednostavljena verzija igre *Liar's Bar*.

Projekat na predmetu **Računarske mreže i telekomunikacije**, školska godina 2025/26.

| | |
|---|---|
| **Tim** | Muvrinovci |
| **Članovi** | Miloš Vasiljević 20230185, Aleksa Pačkovski 20230096, Strahinja Sokolović 20230014 |
| **Mentor** | Uroš Šošević |
| **Tehnologije** | Java 21, JavaFX 21, TCP sokceti, JSON (Gson), Maven |

---

## O igri

Igrači na početku dobijaju po **7 karata** iz kombinovanog špila od 2 standardna špila (104 karte). Igrač na potezu baca 1–8 karata **licem nadole** na centar stola i deklariše njihovu vrednost — pri čemu ne mora da govori istinu.

Ostali igrači imaju kratak prozor da ga **prozovu za laž**:

- ako je **lagao** → lažov kupi sve karte sa centra, prozivač je sledeći na potezu
- ako je **govorio istinu** → prozivač kupi sve karte, igrač na potezu igra ponovo
- ako **niko ne prozove** → red prelazi na sledećeg igrača, koji mora nastaviti istu vrednost runde

Umesto bacanja, igrač može i da **povuče kartu** sa centralnog špila.

**Pobeđuje prvi igrač koji ostane bez karata u ruci.**

---

## Zahtevi

- **JDK 21 ili noviji** (razvijano na JDK 25; kod se kompajlira za Java 21)
- **Maven 3.9+**

Provera:

```bash
java -version && mvn -v
```

---

## Build

```bash
mvn clean install
```

> Koristi se `install`, a ne `package`, jer moduli `server` i `client` zavise od modula `shared` — `install` ga smešta u lokalni Maven repozitorijum pa se posle mogu pokretati pojedinačno.

---

## Pokretanje

### 1. Server

```bash
java -jar server/target/lazes-server.jar
```

Server podrazumevano sluša na portu **5555**. Drugi port se prosleđuje kao argument:

```bash
java -jar server/target/lazes-server.jar 7777
```

Alternativno, preko Mavena:

```bash
mvn -pl server exec:java
```

### 2. Klijent

U **zasebnom terminalu za svakog igrača**:

```bash
java -jar client/target/lazes-client.jar
```

Alternativno, preko Mavena:

```bash
mvn -pl client javafx:run
```

Za partiju je potrebno pokrenuti najmanje **2 klijenta** (maksimalno 4). U glavnom meniju jedan igrač pravi sobu i dobija šestoznamenkasti kod, a ostali se pridružuju tim kodom.

---

## Igranje preko interneta

Deli se **samo server** — klijent svako pokreće kod sebe. Uputstvo sa nekoliko opcija (lokalna mreža, Tailscale, playit.gg, Azure for Students, Oracle Cloud) nalazi se u [deploy/DEPLOY.md](deploy/DEPLOY.md).

Za virtuelnu mašinu se ništa ne prenosi sa lokalnog računara — [`deploy/cloud-init.yaml`](deploy/cloud-init.yaml) pri prvom pokretanju sam klonira projekat sa GitHub-a, kompajlira ga i pokreće server kao servis.

> Igra koristi sirovi TCP soket, pa platforme koje javne servise rutiraju kroz HTTP proxy (Koyeb, Render) nisu pogodne bez posebne plaćene TCP opcije. Tuneli namenjeni game serverima i obične virtuelne mašine rade bez ograničenja.

---

## Rad u NetBeans-u

Projekat se može u potpunosti otvoriti, buildovati i pokrenuti iz NetBeans-a — nije potrebna nijedna dodatna alatka.

**1. Otvaranje**

`File → Open Project…` i izaberi **koren repozitorijuma** (`rmtlazov`), a ne pojedinačne module. NetBeans prepoznaje parent `pom.xml` i prikazuje `shared`, `server` i `client` kao podmodule.

**2. Build**

Desni klik na `rmtlazov` → **Build**. NetBeans pokreće `mvn install`, čime `shared` ulazi u lokalni Maven repozitorijum — bez toga `server` i `client` ne mogu da se pokrenu zasebno.

**3. Pokretanje servera**

Desni klik na modul `server` → **Run**. U Output prozoru se pojavi:

```
Lazes server je pokrenut i slusa na portu 5555
```

**4. Pokretanje klijenata**

Desni klik na modul `client` → **Run**. Za drugog igrača pokreni **Run još jednom** — NetBeans otvara novi Output tab i drugi prozor igre.

Ako ti je zgodnije, drugog igrača možeš pokrenuti i iz terminala:

```bash
java -jar client/target/lazes-client.jar
```

**Napomene**

- Glavna klasa klijenta je `Launcher`, **ne** `MainApp`. To je već podešeno u `client/nbactions.xml`. Ako NetBeans ikad zatraži da ručno izabereš glavnu klasu, izaberi `Launcher` — pokretanje preko `MainApp` puca porukom *„JavaFX runtime components are missing"*.
- JavaFX se povlači kao obična Maven zavisnost, pa **ne treba** ručno instalirati JavaFX SDK.
- Kod se kompajlira za **Java 21**, pa radi na svakom JDK-u 21 ili novijem. Ako NetBeans prijavi problem sa platformom, dodaj JDK kroz `Tools → Java Platforms`.
- Server i klijent su nezavisni procesi — server ostaje pokrenut i između partija.

---

## Struktura projekta

```
rmtlazov/
├── shared/    Model karata i mrezni protokol (deli ga server i klijent)
├── server/    Autoritativni game server — sobe, lobby, pravila igre, tajmeri
└── client/    JavaFX klijent — glavni meni, lobby i sto za igru
```

---

## Odluke donete tokom implementacije

Nekoliko stvari dokumentacija nije precizirala ili je bila protivrečna, pa su razrešene ovako:

| Pitanje | Odluka |
|---|---|
| Vrednosti karata | **1–13** (As=1 … Kralj=13), ukupno **104 karte**, bez džokera. Dokumentacija na dva mesta pominje opseg 1–14, što ne odgovara standardnom špilu od 13 rangova. |
| Identifikator karte | Pošto se igra sa dva špila, ista karta postoji dva puta, pa identifikator nosi i redni broj špila: `7H1`, `10D2`, `AS1`. |
| Vrednost runde | Postavlja je prvi potez runde; svi je moraju nastaviti. **Prozivanje zatvara rundu** i sledeći igrač bira novu vrednost. Vučenje karte je ne menja. |
| Prozivanje | Nakon svakog poteza otvara se prozor od **5 sekundi**. Server prihvata **samo prvu** pristiglu prozivku i odbacuje ostale. |
| Uslov pobede | Igrač koji odigra poslednje karte pobeđuje **tek kada istekne prozor za prozivanje**. Ako ga prozovu i lagao je, kupi ceo centar i nastavlja da igra. |
| Istek poteza | Po isteku 30 sekundi server automatski vuče kartu umesto igrača; ako je špil prazan, baca prvu kartu iz njegove ruke. |
| Grafika | Karte i sto se crtaju JavaFX oblicima i Unicode simbolima (♠♥♦♣), pa projekat ne zavisi ni od jednog slikovnog fajla. |

Protokol iz Dokumenta 3 dopunjen je porukama `start_game`, `room_joined`, `set_avatar`, `leave_room`, `card_drawn` i `error`, koje su bile neophodne za lobby i prikaz grešaka.

---

## Testovi

```bash
mvn test
```

Pokriveno je **31 test**: model karata i protokol, sva pravila igre kroz sve faze partije, i **5 integracionih testova** koji igraju partiju kroz pravi TCP soket.

---

## Dokumentacija

Projektna dokumentacija nalazi se u folderu `docs/`:

- `01_koncept_igre.docx` — koncept, pravila i opseg projekta
- `02_user_stories.docx` — korisnički zahtevi (MoSCoW)
- `03_tehnicka_specifikacija.docx` — arhitektura, protokol i tehnološki stek
