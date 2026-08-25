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

## Struktura projekta

```
rmtlazov/
├── shared/    Model karata i mrezni protokol (deli ga server i klijent)
├── server/    Autoritativni game server — sobe, lobby, pravila igre, tajmeri
└── client/    JavaFX klijent — glavni meni, lobby i sto za igru
```

---

## Dokumentacija

Projektna dokumentacija (Koncept igre, Korisnički zahtevi, Tehnička specifikacija) nalazi se u folderu `docs/`.
