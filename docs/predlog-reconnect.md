# Predlog: ponovno povezivanje igrača (reconnect)

Interni predlog za tim. Opisuje kako bi igrač mogao da se vrati u istu partiju posle prekida veze — bilo relaunch-om na istoj mašini, bilo prelaskom na drugi uređaj — **bez uvođenja accounta**.

Status: **predlog, nije implementirano.** Cilj dokumenta je da tim izabere pristup pre nego što se piše kod.

---

## Šta se traži

1. **Grace period + auto-igra.** Kad se klijent odspoji (zatvori prozor, pukne net), mesto mu se čuva ~2 minuta i server automatski igra umesto njega. Ako se vrati u tom roku, nastavlja gde je stao. (Slično auto-odgovoru u Slagalici.)
2. **Povratak istog klijenta** u istu sobu posle relaunch-a.
3. **Cross-device** — isti igrač izađe sa računara i uđe sa telefona u istu partiju. Sve to **bez accounta**.

---

## Ključni uvid: dva nezavisna dela

Svako rešenje se sastoji iz dve odvojene stvari:

1. **Držanje mesta (seat-hold)** — server ne izbacuje igrača na disconnect, nego mu čuva mesto i auto-igra. **Isto je za sve varijante.**
2. **Identitet pri povratku** — kako server prepozna „ovo je taj isti igrač". Tu se rešenja granaju i tu se odlučuje da li radi cross-device.

> **Problem cross-device = problem relaunch-a + token koji se može preneti na drugi uređaj.** Ako identitet napravimo prenosivim, cross-device dolazi besplatno.

---

## Deo zajednički za sve: držanje mesta

Ovo se radi bez obzira na izbor identiteta. **Kod već auto-igra** na isteku poteza (`onTurnTimeout` u [Room.java](../server/src/main/java/com/muvrinovci/lazes/server/Room.java): vuče kartu ili baca najnižu), pa je „automatski igra dok te nema" skoro gotovo.

**Sada:** disconnect → `Room.leave` → `engine.removePlayer(id)` → mesto nestaje, karte izlaze iz igre.

**Treba:** disconnect → mesto se označi `DISCONNECTED`, pokrene se 2-min tajmer (preko postojećeg `scheduler`-a), igrač ostaje u partiji i potezi mu se auto-rešavaju. Vrati se pre isteka → tajmer se otkaže, nova konekcija se zakači na isto `ServerPlayer` mesto, pošalje mu se puno stanje. Istekne 2 min → tek tada pravi `removePlayer`.

### Izazovi (važe za sva rešenja)

| Izazov | Zašto |
|---|---|
| `ServerPlayer.send()` zove `ClientHandler` — a mesto neko vreme nema aktivnu konekciju | Dozvoliti „mesto bez soketa"; poruke se tada samo ne šalju (ili baferuju) |
| Soba se briše kad je `players` prazna | Ne brisati dok makar jedno mesto „visi" na tajmeru |
| Povratak mora da vrati celo stanje (ruka, potez, centar, špil) | Nova poruka-snapshot, ili ponoviti postojeće `hand_update` + `turn_update` |
| `engine.removePlayer` pomera indekse | Umesto uklanjanja: „preskoči `DISCONNECTED` mesto", pa ga vrati kad se igrač vrati |
| Ista tajna se poveže dok stara konekcija još živi (dva uređaja) | Odlučiti: nova preuzima, stara ispada (baš to hoćemo za cross-device) |

---

## Problem A — isti klijent se vrati (relaunch na istoj mašini)

Identitet mora da preživi gašenje aplikacije.

### A1. Reconnect-token u lokalnom fajlu na klijentu
Server pri ulasku izda tajni `reconnectToken` (npr. UUID) vezan za mesto. Klijent ga upiše u fajl (`~/.lazes/session`). Pri povratku šalje `{roomCode, reconnectToken}`; server uporedi i zakači.

| Prednosti | Mane |
|---|---|
| Robusno — preživi crash i relaunch | Klijent mora da piše/čita fajl |
| Token je pravi tajni ključ — niko ne otima mesto | Ako se fajl izgubi, nema povratka |
| Radi i kad se IP promeni | Traži nove poruke + seat-hold |
| Povratak automatski (korisnik ništa ne kuca) | Ne radi cross-device sam (fajl je na toj mašini) |

### A2. Ime + kod sobe kao „meki identitet"
Pri povratku klijent pošalje isto ime + kod sobe. Server nađe `DISCONNECTED` mesto sa tim imenom i zakači.

| Prednosti | Mane |
|---|---|
| Nula skladištenja, trivijalno | **Nebezbedno** — ko zna kod + ime otme mesto |
| Korisnik samo ponovo ukuca ime | Sudar imena (dva „Miloš") |
| Radi cross-device besplatno | Zavisi od tačno istog imena |

### A3. Kratak „resume kod" koji server izda
Server pri ulasku da kratak kod (npr. 4 znaka). Klijent ga kešira (auto povratak) i pokaže korisniku (može da ga ukuca drugde).

| Prednosti | Mane |
|---|---|
| Bezbednije od imena | Korisnik čuva/kuca kod ako nije keširan |
| Može i auto (keš) i ručno (cross-device) — zlatna sredina | I dalje treba seat-hold |

### A4. Prepoznavanje po IP adresi — *odbačeno*
NAT i mobilne mreže menjaju IP, više igrača iza istog rutera deli IP, telefon je ionako drugi IP. Ruši samu ideju.

---

## Problem B — drugi uređaj, bez accounta (PC → telefon)

Razlika je samo jedna: token mora da **pređe na drugi uređaj**. Bez accounta, korisnik **sam nosi** nešto: kod koji ukuca, QR koji skenira, ili tajnu koju pamti.

### B1. Ručni resume kod (ukucaš na telefonu)
Server izda čitljiv kod (npr. `roomCode` + lični 4-znakovni PIN). Na telefonu uneseš oba.

| Prednosti | Mane |
|---|---|
| Bez accounta, stvarno cross-device, prosto | Korisnik mora da ga vidi i prekuca |
| Korisnik ga kontroliše | Ako ga nije zapisao pre pada PC-ja — nema povratka (ublažiti: kod stalno vidljiv u UI) |

### B2. Korisnik bira PIN/tajnu pri ulasku
Pri ulasku korisnik izabere kratku tajnu. Za povratak sa bilo kog uređaja: `roomCode + ime + tajna`.

| Prednosti | Mane |
|---|---|
| Bez accounta, bez skladištenja, pun cross-device | Korisnik mora da zapamti tajnu |
| Uvek je reprodukuje (sam ju je izabrao) | Malo trenja pri ulasku |
| Tajna sprečava otimanje (za razliku od A2) | Slabo ako izabere trivijalnu tajnu (za školski projekat OK) |

### B3. QR kod / handoff
PC prikaže QR sa `{roomCode, reconnectToken}`; telefon skenira i uđe kao isti igrač.

| Prednosti | Mane |
|---|---|
| Elegantno, bezbedno, bez kucanja | Treba generisanje QR-a i put za skeniranje |
| Odlična UX | Kamera na telefonu; **QR se mora pokazati pre** nego što PC crkne |

### B4. Centralni registar po „prijavljenom identitetu"
Server drži mesta po korisničkom identifikatoru (ime + tajna); svaki uređaj koji ga pošalje se zakači. Uopštenje B1/B2 — vidi iskrenu granicu dole.

---

## Iskrena granica (za odbranu)

**Cross-device bez accounta u principu zahteva da korisnik nosi *nešto* između uređaja** — kod, QR ili zapamćenu tajnu. Ne postoji način da telefon bude prepoznat kao „isti igrač" kao PC osim ako pokaže isti prenosivi dokaz.

Account je samo trajna, na serveru sačuvana verzija tog istog dokaza. „Ime + tajna koju pamtiš" (B2) *jeste* minimalni account — samo bez baze i registracije. To je sasvim u redu za projekat; bezbednost je tačno onolika kolika je tajna.

Rešenja bez tajne (A2 — samo ime, ili keširan token A1 iskorišćen sa druge mašine) znače da ko zna kod sobe + ime može da uskoči na tuđe mesto. Za igru sa drugarima prihvatljivo; dobro je to eksplicitno navesti kao svesnu odluku.

---

## Preporuka: jedan mehanizam, dva ulaza

Najčistije — **jedan reconnect-token** koji pokriva oba problema:

1. Server pri ulasku izda `reconnectToken` (tajni) **i** kratak čitljiv `resumeCode`.
2. Klijent **automatski kešira** token u fajl → problem A radi transparentno (relaunch = tihi povratak).
3. UI **stalno prikazuje `resumeCode`** (i opciono QR) → problem B: na telefonu uneseš `roomCode + resumeCode` i preuzmeš mesto.
4. Nova konekcija sa važećim tokenom preuzme; stara (ako još živi) se prekine → „prelazak sa PC-ja na telefon" znači da telefon preuzme, a PC ispadne.

Isti kod tako servira i „vrati me posle pada" i „nastavi na telefonu"; jedina razlika je da li token dolazi iz keša ili ga korisnik ukuca/skenira.

### Minimalni skup izmena

| Sloj | Izmena |
|---|---|
| `GameEngine` | `DISCONNECTED` stanje mesta umesto `removePlayer` (auto-igra već postoji) |
| `Room` | seat-hold tajmer (2 min) preko postojećeg `scheduler`-a; ne brisati sobu dok mesto visi |
| `ServerPlayer` | sme da postoji bez aktivnog `ClientHandler`-a |
| Protokol | `reconnect {roomCode, token}` (klijent→server), `state_snapshot` (server→klijent), `resumeCode`/`token` u `room_joined` |
| Klijent | keširanje tokena u fajl + polje „Nastavi partiju" |

### Predlog faza

1. **Faza 1 — samo problem A.** Seat-hold + auto reconnect keširanim tokenom. Najveća vrednost, najmanje UI-ja.
2. **Faza 2 — problem B.** Prikaz `resumeCode`-a i ekran „Nastavi partiju" (unos `roomCode + resumeCode`).
3. **Faza 3 (opciono) — QR handoff.** Kozmetika, ako ostane vremena.

---

## Odnos prema postojećoj dokumentaciji

- Trenutno ponašanje na disconnect opisano je u [ARHITEKTURA.md, poglavlje 9](ARHITEKTURA.md#9-šta-se-dešava-kad-neko-prekine-vezu) — ovaj predlog menja upravo taj lanac (`Room.leave` → `removePlayer`).
- MoSCoW: reconnect je u dokumentaciji **Could Have**, pa je ovo proširenje van MVP-a; uvoditi tek kad je osnovna partija stabilna.
