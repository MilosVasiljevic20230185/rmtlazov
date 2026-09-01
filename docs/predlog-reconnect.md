# Ponovno povezivanje igrača (reconnect)

Opis rešenja za povratak u partiju posle prekida veze — **bez accounta i bez profila**, svi igrači su gosti.

Status: **implementirano.**

---

## Šta radi

1. **Grace period + auto-igra.** Kad se klijent odspoji (zatvori prozor, pukne net), mesto mu se čuva 2 minuta i server automatski igra umesto njega.
2. **Povratak istog uređaja** u istu partiju, jednim dugmetom, bez unosa ičega.
3. **Napuštena soba se gasi** ako u njoj minut nema nijednog povezanog igrača.

Cross-device povratak (izađeš sa računara, uđeš sa telefona) **nije podržan, i to namerno** — objašnjeno u poglavlju „Zašto nema cross-device".

---

## Dva nezavisna dela

Rešenje se sastoji iz dve odvojene stvari:

1. **Držanje mesta (seat-hold)** — server ne izbacuje igrača na disconnect, nego mu čuva mesto i auto-igra.
2. **Identitet pri povratku** — kako server prepozna „ovo je taj isti igrač". Ovde je izabran otisak uređaja.

---

## Držanje mesta

**Ranije:** disconnect → `Room.leave` → `engine.removePlayer(id)` → mesto nestaje, karte izlaze iz igre.

**Sada:** disconnect → `Room.onConnectionLost` → ako je partija u toku, `holdSeat` označi mesto kao odspojeno, pokrene tajmer od 2 minuta i pusti da server igra umesto igrača. Tek kad tajmer istekne, `expireSeat` pozove istu onu staru `leave` logiku.

Pravila igre nisu dirana. `GameEngine` je ostao nepromenjen jer je auto-igra već postojala — `onTurnTimeout` vuče kartu, a ako je špil prazan baca najnižu. Jedina izmena je da tajmer poteza za odspojeno mesto traje `DISCONNECTED_TURN_SECONDS` (3s) umesto punih 60s, da partija ne bi stajala.

Kako je to rešeno u kodu:

| Izazov | Rešenje |
|---|---|
| `ServerPlayer.send()` je zvao `ClientHandler`, a mesto neko vreme nema konekciju | `handler` više nije `final`; kad je `null`, `send()` tiho preskače poruku |
| Povratak mora da vrati celo stanje | Nova poruka `game_snapshot` sa rukom, stolom, fazom i preostalim vremenom |
| `engine.removePlayer` pomera indekse | Ne poziva se na disconnect nego tek na istek roka, pa problem ni ne nastaje |
| Soba se brisala kad `players` ostane prazna | Odspojena mesta i dalje stoje u `players`, pa se soba ne briše dok makar jedno visi |
| Stara nit bi zatvaranjem soketa srušila mesto koje je već preuzeto | `ClientHandler.disconnect` radi nešto samo ako je `player.getHandler() == this` |
| Zakasneli tajmeri posle gašenja sobe | `Room.submit` i `Room.schedule` hvataju `RejectedExecutionException` |

---

## Identitet: otisak uređaja

Klijent pri pokretanju izračuna `deviceId` i pošalje ga uz `create_room` / `join_room`:

```
deviceId = sha256(MAC | hostname | user.name)   // prvih 32 heks znaka
```

MAC je leksikografski najmanja adresa među nevirtualnim karticama — sortira se zato da uključivanje VPN-a ili dokovanje laptopa ne promeni otisak. Sirov MAC nikada ne napušta mašinu, šalje se samo heš. Ništa se ne upisuje na disk: otisak se računa iznova pri svakom pokretanju, pa nema ni profila ni sesije koja bi preživela izvan same mašine.

Krhkost otiska praktično ne smeta jer je prozor kratak — u dva minuta se ni hostname ni mrežne kartice ne menjaju.

`RoomManager` drži registar `deviceId → roomCode` od ulaska u sobu do trenutka kada mesto konačno nestane. Zbog toga klijent pri povratku ne šalje ni kod sobe — server sam zna gde je taj uređaj bio, pa je povratak jedno dugme „Nastavi partiju".

Aktivno mesto se **nikada ne preuzima**: ako otisak odgovara mestu koje ima živu konekciju, povratak se odbija sa `SESSION_ACTIVE`.

### Razmatrane alternative

| Varijanta | Zašto nije izabrana |
|---|---|
| Tajni token keširan u fajl na klijentu | Traži upis na disk, a to je oblik profila koji smo hteli da izbegnemo |
| Ime + kod sobe | Nebezbedno — ko zna kod i ime, uskoči na tuđe mesto; uz to se imena sudaraju |
| Kratak resume kod za prekucavanje | Ima smisla samo ako se podržava cross-device, a on je odbačen |
| PIN koji korisnik bira pri ulasku | Trenje pri svakom ulasku, i nema tihog automatskog povratka |
| Prepoznavanje po IP adresi | NAT i mobilne mreže menjaju IP, više igrača iza istog rutera deli IP |

---

## Napuštena soba

Ako u sobi ostane nula povezanih igrača, partija se **zaledi**: tajmer poteza se otkaže, auto-igra staje, i kreće tajmer od `EMPTY_ROOM_SECONDS` (60s). Vrati li se neko u tom roku, tekuća faza dobija pun tajmer iznova i partija se nastavlja. Ako se niko ne vrati, partija se prekida i soba briše.

Taj tajmer je kraći od grace perioda mesta (60s naspram 120s), pa kad svi ispadnu, soba umire pre nego što pojedinačna mesta isteknu. To je svesna odluka: bez zaleđivanja bi automatski potezi doigrali celu partiju dok je niko ne gleda.

---

## Protokol

Dodato je troje poruka, a nekoliko postojećih je dobilo nova polja. Sva nova polja su aditivna i stari konstruktori su zadržani, pa se ništa u postojećem toku igre nije promenilo.

**Nove poruke**

- `reconnect` (klijent → server): `deviceId`, `playerName`
- `game_snapshot` (server → klijent): celo stanje partije — faza, ruka, sto, špil, preostalo vreme, igrači, i podaci o potezu koji čeka prozivanje
- `player_reconnected` (server → klijent): `playerId`, `playerName`

**Dopunjene poruke**

- `create_room`, `join_room` + `deviceId`
- `player_disconnected` + `temporary`, `graceSeconds`
- `turn_update` → `PlayerInfo` + `connected`

**Novi kodovi grešaka**

- `RECONNECT_FAILED` — nema sačuvanog mesta za taj uređaj, ili je rok istekao
- `SESSION_ACTIVE` — mesto tog uređaja ima živu konekciju

---

## Zašto nema cross-device

Bez accounta, korisnik bi morao **sam da prenese neki dokaz** sa jednog uređaja na drugi — kod koji prekuca, QR koji skenira ili tajnu koju pamti. Ne postoji način da server prepozna telefon kao „istog igrača" ako telefon ne pokaže isti prenosivi dokaz; account je samo trajna, na serveru sačuvana verzija tog istog dokaza.

Odlučeno je da se ta mogućnost ne uvodi. Posledica je da je identitet vezan za mašinu i da ništa prenosivo ne postoji, pa nema ni čega da se otme.

## Poznata ograničenja

- Dva klijenta sa iste mašine dele otisak, pa ne mogu biti u istoj sobi — drugi ulazak se odbija (`SESSION_ACTIVE`). Mogu biti povezani na server i igrati u različitim sobama. Povratak posle prekida podržan je za jedno mesto po mašini.
- Ko bi lažirao MAC, hostname i korisničko ime, mogao bi da uzme mesto. To zahteva pristup podacima mašine i prozor od dva minuta, pa je za ovaj projekat prihvatljivo.
- Igrač ne vidi šta se dešavalo dok ga nije bilo — poruke se ne baferuju, nego mu se snapshot-om vrati zatečeno stanje.
- Ako je host izgubio vezu, uloga hosta prelazi na povezanog igrača i **ne vraća mu se** kad se vrati.
