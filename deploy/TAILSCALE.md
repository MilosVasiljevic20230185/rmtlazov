# Tailscale — detaljno uputstvo za tim

Tailscale pravi privatnu mrežu između vaših računara. Ponaša se kao da ste svi na istoj WiFi mreži, bez obzira gde se nalazite — idealno za razvoj i testiranje.

**Prednosti:**
- Besplatan zauvek (Personal plan: do 6 korisnika, neograničeni uređaji)
- Registracija preko Google ili GitHub naloga — **nikakva kartica nije potrebna**
- Radi iza CGNAT-a i bez otvaranja portova na ruteru
- Nema latencije ni saobraćaja kroz tuđe servere (P2P konekcija)

---

## Korak 1: Svaki član tima — kreiraj Tailscale nalog

**1a. Na [tailscale.com](https://tailscale.com):**

- Klikni **Sign up** desno gore
- **Odaberi:** Google ili GitHub nalog za prijavu (najjednostavnije)
- Prosledi autentifikaciju — gotovo

**1b. Instalacija na svoj računar:**

- [tailscale.com/download](https://tailscale.com/download)
- **Windows:** `tailscale-setup-latest.exe` → instalacija
- **Mac:** `tailscale-latest.dmg` → instalacija

Nakon instalacije, Tailscale će biti u system tray-u.

---

## Korak 2: Svetinje — bira jedan član

Jedan član (preporučujem Miloša) će biti "vlasnik" tailneta. Ostali će se pridružiti tom nalogu.

**Važno:** Ne trebate tri odvojena Tailscale naloga. Trebate **jedan tailnet** (privatnu mrežu) i tri korisnika koje će taj vlasnik pozvati.

Hajde da to bude ovako:

- **Miloš** — kreira nalog (vlasnik tailneta)
- **Aleksa i Strahinja** — primaju pozivnicu od Miloša i pristaju na isti tailnet

---

## Korak 3: Miloš — poziva Aleksu i Strahinju

**3a. Miloš se prijavi na [login.tailscale.com](https://login.tailscale.com)**

U levom meniju: **Admin** → **Users & access**

Klikni **+ Invite users** desno gore.

**3b. Unesi mejlove:**
```
aleksa.packovski@fakultet.edu
strahinja.sokolovic@fakultet.edu
```

Tailscale šalje pozivnice.

**3c. Aleksa i Strahinja primaju mejl, kliknu link, završe prijavu.**

Sada su svi u istoj privatnoj mreži.

---

## Korak 4: Svi — uključi Tailscale i vidiš svoju IP adresu

**4a. Na svakom računaru — klikni na Tailscale u system tray-u (donji desni ugao)**

Trebalo bi da kaže: `Connected` i `100.x.y.z` (ili sličan broj počevši sa 100)

**4b. Dvostruko klikni na Tailscale ikonu ili klikni na nju:**

Vidiš formu sa `Tailnet name` (npr. `milos-family`), **IPv4 address** (evo ga — to je onaj `100.x.y.z`), i druge opcije.

**Svako od vas tri napišite na papiru svoju IP adresu:**
```
Miloš:      100.x.y.z
Aleksa:     100.x.y.a
Strahinja:  100.x.y.b
```

---

## Korak 5: Miloš — pokreće server

Na računaru gde je Tailscale uključen, u terminalu:

```bash
cd C:\Users\milos\Documents\GitHub\rmtlazov
mvn clean install
java -jar server/target/lazes-server.jar
```

Server sluša na portu 5555.

---

## Korak 6: Aleksa — pokreće klijent (prvi put)

Na svom računaru, u **glavni meni igre — polje "Server":**

Unesi **Miloševu IP adresu iz koraka 4b:**
```
100.x.y.z
```

Port: `5555` (ostaje isto)

**Miloš** u istom meniju unosi:
```
Server: localhost (ili 127.0.0.1)
Port: 5555
```

Oba klijenta se povezuju na isti server → soba se pravi na isti način kao pre.

---

## Korak 7: Strahinja — treći igrač (ista stvar)

Unesi Miloševu IP adresu i port 5555, igraj.

---

## Provera da li radi

Otvorite PowerShell na računaru gde je klijent i testirajte:

```powershell
Test-NetConnection 100.x.y.z -Port 5555
```

`TcpTestSucceeded : True` znači da je sve ispravno.

---

## Ako nešto ne radi

| Problem | Rešenje |
|---|---|
| Tailscale kaže „Not connected" ili nema IP adrese | Klikni dugme `Login` u Tailscale aplikaciji i prijavi se |
| Vidiš IP adresu ali se klijent ne povezuje | Proveri firewall na Windows-u — vidiš uputstvo u [deploy/DEPLOY.md](DEPLOY.md), sekcija **Windows Firewall** |
| Klijent kaže „Nije moguće povezati se" | Proveri da si upisao **tačnu IP adresu** iz Tailscale aplikacije — često su `100.x.y.z` adrese slične |
| Server se ne vidi | Tailscale ikada može biti pauziran — klikni na nju, provjeri da je `Connected` |

---

## Ažuriranje koda posle inicijalne verzije

Miloš povuče nove izmene:

```bash
cd C:\Users\milos\Documents\GitHub\rmtlazov
git pull
mvn clean install -DskipTests
```

Ponovo pokreće server — ostali klijenti se automatski reconnect-uju.

---

## Dodatne opcije (nisu potrebne za osnovnu igru)

**Exit node** — ako neko pokušava da bude na drugoj mreži nego što je kući (npr. na poslu sa VPN-om), može da koristi Tailscale exit node. To je naprednije, za sad nije potrebno.

**Split tunnel** — ako želiš da samo Lazes game ide kroz Tailscale, a ostali saobraćaj ide normalno. Za dev okruženje, nije važno.

---

## Zaključak

1. Svi kreiramo Tailscale naloge (Google/GitHub prijava)
2. Miloš je vlasnik, poziva Aleksu i Strahinju
3. Svi bilježe svoje `100.x.y.z` IP adrese
4. Miloš pokreće server na svojoj IP adresi
5. Aleksa i Strahinja se povezuju sa tom IP adresom
6. Igra radi kao na lokalnoj mreži

Nema komplikacija sa portima, firewall-ima ili regijama. Čisto P2P između vas tri.
