# Objašnjenja koda — priprema za odbranu

Zbirka pitanja i odgovora o kodu igre **Lažeš**, sređena za članove tima. Dopunjuje [ARHITEKTURA.md](ARHITEKTURA.md): tamo je pregled sistema, ovde su konkretna „zašto" pitanja kakva mentor obično postavlja.

---

## Sadržaj

- [Java koncepti](#java-koncepti)
  - [Šta je `record` i zašto `Card` jeste record](#šta-je-record-i-zašto-card-jeste-record)
  - [Šta je kompaktni konstruktor (blok bez zagrada)](#šta-je-kompaktni-konstruktor)
  - [Šta radi `setDaemon`](#šta-radi-setdaemon)
- [Mrežni sloj](#mrežni-sloj)
  - [Šta određuje da koristimo TCP soket](#šta-određuje-da-koristimo-tcp-soket)
  - [Koje klase čine mrežni sloj](#koje-klase-čine-mrežni-sloj)
  - [Koje klijentske klase koriste JavaFX](#koje-klijentske-klase-koriste-javafx)
  - [Kako klase mrežnog sloja komuniciraju](#kako-klase-mrežnog-sloja-komuniciraju)
  - [Šta radi `run` u `ClientHandler`](#šta-radi-run-u-clienthandler)
  - [Šta radi `acceptLoop` u `GameServer`](#šta-radi-acceptloop-u-gameserver)
  - [Zašto se u `JsonCodec` stavlja i `MessageType` i klasa](#zašto-se-u-jsoncodec-stavlja-i-messagetype-i-klasa)
- [Konkurentnost i tajmeri](#konkurentnost-i-tajmeri)
  - [Šta je `actionToken`](#šta-je-actiontoken)

---

## Java koncepti

### Šta je `record` i zašto `Card` jeste record

`record` je vrsta klase (od Jave 16) za objekte koji **samo nose podatke**. Kad napišeš:

```java
public record Card(Rank rank, Suit suit, int deckIndex) { }
```

Java automatski generiše ono što bi kod obične klase morao ručno: privatna `final` polja, konstruktor sa sva tri parametra, „gettere" (`rank()`, `suit()`, `deckIndex()`), te `equals()`, `hashCode()` i `toString()`.

Dve ključne osobine:

- **Nepromenljiv (immutable)** — polja su `final`; jednom napravljena karta se ne menja.
- **Poredi se po vrednosti, ne po referenci** — dve karte istog ranga, boje i špila su `equals`, iako su dva objekta u memoriji.

**Zašto baš za `Card`:**

1. Karta je samo trojka vrednosti (rang + boja + špil), bez stanja koje se menja — tačno za šta je record napravljen.
2. **Poređenje po vrednosti nam treba.** U `GameEngine` se radi `hand.removeAll(played)`; da bi to radilo, Java mora da zna kad su dve karte „iste". Record to daje besplatno kroz `equals`. Obična klasa bez `equals` poredila bi po referenci i ne bi našla kartu.
3. Nepromenljivost sprečava bagove — karta putuje kroz mrežu, stoji u ruci, na centru; pošto je immutable, ne može se greškom izmeniti „na dva mesta".

> Record nije „prazan": sme da ima metode i validaciju, samo ne sme da menja polja. Naš `Card` tako dodaje `id()`, `value()`, `fromId()` i validaciju u konstruktoru.

**Jedna rečenica:** `Card` je record jer je nepromenljiv nosilac tri vrednosti; record nam besplatno daje konstruktor, gettere i — najvažnije — `equals`/`hashCode` po vrednosti, bez čega provera vlasništva i uklanjanje karata iz ruke ne bi radili.

---

### Šta je kompaktni konstruktor

Ovaj blok u `Card` je **konstruktor**, i to poseban oblik koji postoji samo kod record-a — **kompaktni kanonski konstruktor**:

```java
public Card {
    Objects.requireNonNull(rank, "rank");
    Objects.requireNonNull(suit, "suit");
    if (deckIndex < 1) {
        throw new IllegalArgumentException("Redni broj spila mora biti pozitivan: " + deckIndex);
    }
}
```

Nema liste parametara u zagradama jer Java **već zna** parametre — navedeni su u zaglavlju recorda. Ti pišeš samo provere, a **dodela polja (`this.rank = rank` itd.) se dešava automatski na kraju**.

Redosled pri `new Card(...)`:
1. Uđe u blok, gde su `rank`, `suit`, `deckIndex` već popunjeni prosleđenim vrednostima
2. Izvrše se provere
3. Java sama doda dodelu polja

Zato se koristi baš za **validaciju** — presretneš vrednosti pre nego što objekat nastane, pa objekat sa besmislenim stanjem nikad ni ne nastane.

---

### Šta radi `setDaemon`

`setDaemon(true)` označava nit kao **daemon (pozadinsku)**. Pravilo:

> JVM se gasi čim se završe sve *obične* niti. Na daemon niti ne čeka — nasilno ih prekine.

U `GameServer` su daemon i `acceptThread` (prihvata konekcije) i svaka nit iz pool-a (nit po igraču). Te niti su **beskonačne petlje** — `acceptThread` večno čeka nove konekcije, niti igrača vise u `readLine()`.

Da su obične, posle `Ctrl+C` program se **ne bi ugasio** — JVM bi čekao da te večne niti završe, što se nikad ne dešava, pa bi server ostao kao proces-zombi. Sa `setDaemon(true)` JVM vidi da su ostale samo daemon niti i odmah se gasi.

**Bitno:** `setDaemon` se sme pozvati **samo pre** `start()`; posle toga baca `IllegalThreadStateException`. Zato u kodu uvek ide: napravi nit → `setDaemon(true)` → `start()`.

**Analogija:** daemon nit je kao grejanje u zgradi — dok ima ljudi (običnih niti) radi; kad svi izađu, gasi se s njima.

---

## Mrežni sloj

### Šta određuje da koristimo TCP soket

Nigde ne piše reč „TCP" — protokol je određen **izborom klasa**. `java.net.Socket` i `java.net.ServerSocket` **su** TCP po definiciji. Za UDP bi se koristila druga klasa (`DatagramSocket`).

Server — `GameServer.java`:
```java
serverSocket = new ServerSocket(port);   // TCP
Socket socket = serverSocket.accept();   // TCP
```

Klijent — `NetworkClient.java`:
```java
socket = new Socket();                                        // TCP
socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
```

| Ako koristiš… | Dobiješ |
|---|---|
| `Socket` / `ServerSocket` | **TCP** (pouzdan, redosled zagarantovan) |
| `DatagramSocket` / `DatagramPacket` | UDP (bez garancija) |

Pošto u celom projektu piše `Socket`/`ServerSocket`, a nigde `DatagramSocket` — igra je TCP.

**Dva prateća dokaza:**
- Nad soketom koristimo **tokove**: `socket.getInputStream()` / `getOutputStream()`. To postoji jer je TCP neprekidan tok bajtova; UDP šalje zasebne pakete.
- `CONNECT_TIMEOUT_MS` u `connect(...)` — samo TCP ima fazu uspostavljanja veze (handshake) koja može da istekne.

---

### Koje klase čine mrežni sloj

Treba razlikovati dva nivoa:

**Klase koje stvarno rade sa soketom (jezgro):**

| Strana | Klasa | Uloga |
|---|---|---|
| Server | `GameServer` | Drži `ServerSocket`, prihvata konekcije |
| Server | `ClientHandler` | Jedna po konekciji; čita i šalje preko soketa |
| Klijent | `NetworkClient` | Jedini soket na klijentu; nit za čitanje + `send` |

**Zajednički protokol (`shared`, koristi ga obe strane):** sve u paketu `protocol` — `JsonCodec`, `Message`, `MessageType`, `ProtocolException`, `ErrorCode` i 20 DTO klasa. To je format poruka.

**Most ka igri:** `ServerPlayer.send()` samo prosledi poziv `ClientHandler`-u. `Room` i `RoomManager` nisu mrežni sloj u užem smislu — kad šalju poruku, idu kroz `ServerPlayer.send` → `ClientHandler.send`, a sami ne diraju soket.

---

### Koje klijentske klase koriste JavaFX

| Koristi JavaFX | Ne koristi (čist Java kod) |
|---|---|
| `MainApp`, `ViewNavigator` | `ScreenController` (interfejs) |
| `MainMenuController`, `LobbyController`, `TableController` | `GameSession` |
| `CardView` | `Launcher` |
| `NetworkClient` | `Avatars` |

Dve napomene:
- **`NetworkClient` koristi JavaFX**, iako je mrežna klasa — ali samo zbog `Platform.runLater`, da primljene poruke prebaci u JavaFX nit. Ne crta ništa.
- **`Launcher` namerno *ne* koristi JavaFX** — to je zaobilaznica za grešku „JavaFX runtime components are missing"; samo pozove `MainApp.main()`.

---

### Kako klase mrežnog sloja komuniciraju

Tok jedne poruke:

1. **Klijent šalje.** Kontroler pozove `network.send(poruka)`. `NetworkClient` je preda `JsonCodec.encode` (dobije JSON tekst) i upiše `println`-om u soket.
2. **Server čita.** `ClientHandler` tog igrača u petlji `readLine()` pročita red, preda ga `JsonCodec.decode` (dobije `Message`), pa `dispatch` odluči: `create_room`/`join_room` obradi sam, ostalo prosledi u `Room` (kroz `room.submit`).
3. **Server odgovara.** `Room` promeni stanje i pozove `player.send(...)`. `ServerPlayer.send` prosledi `ClientHandler.send`, koji kroz `JsonCodec.encode` pretvori poruku u JSON i upiše u soket.
4. **Klijent prima.** `NetworkClient` u nit-za-čitanje pročita red, dekoduje ga i preko `Platform.runLater` prosledi aktivnom kontroleru (`onMessage`), koji osveži prikaz.

**Ključno:** `ClientHandler` i `NetworkClient` se **ne poznaju direktno** — komuniciraju isključivo preko soketa i zajedničkog `JsonCodec`-a. Obe strane zavise samo od protokola iz `shared`-a, ne jedna od druge.

> Vizuelna šema ovih veza je u [ARHITEKTURA.md, poglavlje 3](ARHITEKTURA.md#3-mrežni-sloj) (slika `img/mapa-klasa.png`).

---

### Šta radi `run` u `ClientHandler`

`ClientHandler implements Runnable`, i za svaku konekciju se pokreće `run` u **zasebnoj niti**. Znači: jedan igrač = jedna konekcija = jedna nit = jedan `run`, koji živi dok traje veza.

```java
public void run() {
    Log.info("Nova konekcija sa %s", socket.getRemoteSocketAddress());
    try {
        String line;
        while ((line = reader.readLine()) != null) {   // čeka i čita poruke
            if (line.isBlank()) continue;
            dispatch(line);
        }
    } catch (IOException e) {
        Log.info("Konekcija %s prekinuta: %s", ...);    // veza pukla
    } finally {
        disconnect("connection_lost");                  // UVEK očisti stanje
    }
}
```

- `readLine()` **blokira** dok poruka ne stigne; kad stigne, obradi je i čeka sledeću.
- Vraća `null` kad klijent **uredno** zatvori vezu → petlja izađe.
- Ako veza **pukne**, baca `IOException` → hvata ga `catch`, bez rušenja servera.
- `finally` se izvršava **u oba slučaja** i zove `disconnect(...)` — izbaci igrača iz sobe (ostali dobiju `player_disconnected`) i zatvori soket. Zato nijedan način prekida ne ostavlja „duha" za stolom.

**Zašto zasebna nit:** `readLine()` blokira; da je sve u jednoj niti, dok server čeka poruku od igrača A, igrač B se ne bi mogao ni povezati.

---

### Šta radi `acceptLoop` u `GameServer`

`acceptLoop` je kapija servera — jedina petlja koja prima nove igrače i za svakog otvara nit.

```java
private void acceptLoop() {
    while (!serverSocket.isClosed()) {
        try {
            Socket socket = serverSocket.accept();                 // čeka konekciju
            connections.submit(new ClientHandler(socket, roomManager));  // preda niti
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                Log.error("Neuspesno prihvatanje konekcije: %s", e.getMessage());
            }
        }
    }
}
```

- `accept()` **blokira** dok se neko ne poveže; kad se poveže, vrati `Socket` — zasebnu cev ka baš tom klijentu.
- `connections.submit(...)` pokreće `ClientHandler.run()` u zasebnoj niti i `acceptLoop` se **odmah vraća** na `accept()` za sledećeg. Zato se više igrača može povezati istovremeno.
- `catch` je **unutar** petlje: jedan neuspešan pokušaj se zabeleži i petlja se nastavi — ne obara server.
- Trik u `if`-u: kad neko pozove `stop()` (zatvori `serverSocket`), `accept()` takođe baci `IOException`, ali je to **očekivano** pa se ćuti; prava greška uz otvoren server se loguje.

**Podela odgovornosti:** `acceptLoop` **samo prima**, `ClientHandler` **obrađuje**. Postoji jedan server-soket koji sluša na portu i mnogo običnih soketa — po jedan za svakog igrača.

---

### Zašto se u `JsonCodec` stavlja i `MessageType` i klasa

Registar spaja par: string i klasu.

```java
Map.entry(MessageType.PLAY_CARDS, PlayCardsMessage.class)
//         ključ: "play_cards"      vrednost: klasa
```

**Problem:** poruka stigne kao tekst — `{"type":"play_cards",...}`. Gson ume da popuni Java objekat, ali samo ako mu kažeš **koju klasu**; sam ne može da pogodi da ovaj tekst treba da postane `PlayCardsMessage`.

- **Ključ `"play_cards"`** je ono što **putuje kroz mrežu** (polje `type` u JSON-u). Obe strane dele tekst, ne Java objekte.
- **Vrednost `PlayCardsMessage.class`** je Java klasa — postoji samo u kodu, ne može mrežom; to je „kalup" u koji Gson sipa podatke.

Oba su potrebna jer registar **premošćuje dva sveta**: string sa mreže → Java klasa u kodu. Iz golog stringa se klasa ne može izvesti; neko mora eksplicitno da napravi spisak parova. To je kao **rečnik**: strana reč (`"play_cards"`) → prevod (`PlayCardsMessage`).

```java
String type = object.get("type").getAsString();     // "play_cards"
Class<? extends Message> target = REGISTRY.get(type); // PlayCardsMessage.class
return GSON.fromJson(object, target);                 // popuni tu klasu
```

**Zašto konstanta `MessageType.PLAY_CARDS` a ne goli string:** i klijent (kad šalje) i server (kad čita) koriste **istu konstantu iz `shared`-a**. Da su kucali goli string, greška u kucanju (`"play_card"`) prošla bi kroz kompajler i pukla tek u igri. Ovako se ne bi ni kompajliralo.

---

## Konkurentnost i tajmeri

### Šta je `actionToken`

`actionToken` je brojač (`long`) u `Room` koji rešava problem **zakasnelog tajmera**.

**Problem:** tajmer poteza je zakazan na 30 s. U 29.99 s igrač sam odigra. Kod pozove `cancel()` — ali kasno: zadatak je već krenuo. Rezultat bi bio da server odigra **još jedan** potez umesto igrača koji je već odigrao.

**Rešenje:** svaka promena stanja uvećava brojač, a zakazani zadatak pamti vrednost od kad je zakazan:

```java
private void startTurnTimer() {
    cancelTimer();
    long token = ++actionToken;                 // token za OVAJ tajmer
    pendingTimer = scheduler.schedule(
            () -> submit(() -> onTurnTimeout(token)), TURN_SECONDS, SECONDS);
}

private void onTurnTimeout(long token) {
    if (token != actionToken || state != RoomState.IN_GAME) {
        return;                                 // stanje se promenilo — odustani
    }
    ...
}
```

Kako hvata zakasneli tajmer:

| Trenutak | `actionToken` | Šta se dešava |
|---|---|---|
| Zakazan tajmer | `5` | tajmer pamti `token = 5` |
| Igrač odigra u 29.99 s | `6` | `startCallWindow` uradi `++actionToken` |
| Tajmer se ipak okine | `6` | `token(5) != actionToken(6)` → **odustaje** |

Provera se dešava **u niti sobe** (kroz `submit`), sekvencijalno — nema trke. Isti `++actionToken` štiti i prozor za prozivanje i odbrojavanje na početku (`beginFirstTurn`).

**Stručni naziv:** *fencing token* ili *generation counter*.

**Jedna rečenica:** `actionToken` je brojač generacije stanja — svaka promena ga uvećava, a svaki tajmer pamti vrednost od kad je zakazan i odbija akciju ako se brojač promenio; tako se zakasneli tajmer, koji `cancel()` nije stigao da zaustavi, sam poništava.
