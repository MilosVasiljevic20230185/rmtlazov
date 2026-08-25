# Igranje preko interneta

Klijent se uvek pokreće lokalno na svakom računaru — deli se **samo server**. Ovaj dokument opisuje nekoliko načina da server postane dostupan ostalim igračima.

## Koju opciju izabrati

| Opcija | Treba kartica | Ko šta instalira | Najbolje za |
|---|---|---|---|
| [Lokalna mreža](#a-lokalna-mreža-lan) | ne | ništa | **odbrana pred mentorom** |
| [Tailscale](#b-tailscale) | **ne** | svi igrači | redovno testiranje u timu |
| [playit.gg](#c-playitgg) | **ne** | samo host | pokazivanje nekome van tima |
| [Oracle Cloud VM](#d-oracle-cloud-always-free) | da | ništa | server koji radi 24/7 |

> **Zašto ne Koyeb, Render i slični:** igra koristi **sirovi TCP soket**, a ne HTTP. Te platforme javne servise rutiraju kroz HTTP proxy, pa raw TCP tamo radi samo uz posebnu plaćenu opciju. Isto važi i za ngrok — [njegovi TCP endpoint-i traže karticu](https://ngrok.com/blog/tcp-endpoints-require-verification) čak i na besplatnom planu.

---

## Windows Firewall — važi za sve opcije

Na računaru na kome se pokreće **server**, Windows Firewall podrazumevano blokira dolazne konekcije na port 5555. Otvori PowerShell **kao administrator**:

```powershell
netsh advfirewall firewall add rule name="Lazes server" dir=in action=allow protocol=TCP localport=5555
```

Ovo je najčešći uzrok situacije „server radi, ali se niko ne povezuje".

---

## A. Lokalna mreža (LAN)

Najjednostavnije, i sasvim dovoljno za demonstraciju — svi računari na istoj WiFi mreži.

Na računaru koji je server:

```powershell
ipconfig
```

Pročitaj `IPv4 Address` (npr. `192.168.0.14`) i pokreni server:

```bash
java -jar server/target/lazes-server.jar
```

Ostali igrači u glavnom meniju klijenta upisuju tu adresu kao **Server**, port ostaje **5555**.

---

## B. Tailscale

Pravi privatnu mrežu između vaših računara — ponaša se kao da ste svi na istom LAN-u, bez obzira gde se nalazite. Besplatan **Personal** plan važi zauvek, obuhvata do 6 korisnika i neograničeno uređaja, a **registracija ide preko Google ili GitHub naloga — bez kartice**.

1. Svi članovi tima odu na [tailscale.com](https://tailscale.com), prijave se istim nalogom tima i instaliraju klijent
2. Na računaru koji je server:

```bash
tailscale ip -4
```

3. Dobiješ adresu oblika `100.x.y.z` — to je adresa koju ostali upisuju kao **Server**, port **5555**

Radi i iza CGNAT-a i bez otvaranja ijednog porta na ruteru. Nema ograničenja saobraćaja ni trajanja sesije.

---

## C. playit.gg

Tunel namenjen upravo game serverima. Besplatan, **bez kartice**, i — za razliku od Tailscale-a — **ostali igrači ne instaliraju ništa**, samo upišu adresu.

1. Na računaru koji je server, preuzmi agenta sa [playit.gg](https://playit.gg) i pokreni ga
2. Napravi tunel: tip **TCP**, lokalni port **5555**
3. Dobijaš javnu adresu oblika `nesto.playit.gg` i **dodeljeni port** (najčešće nije 5555)
4. Ostali u klijentu upisuju tu adresu kao **Server**, a dodeljeni broj kao **Port**

Saobraćaj ide kroz njihove servere, što dodaje 10–50 ms — potpuno nebitno za igru na poteze.

---

## D. Oracle Cloud (Always Free)

Prava virtuelna mašina sa javnom IP adresom i punim TCP-om, koja radi non-stop. Zahteva odobrenu karticu pri registraciji (ne naplaćuje se).

### 1. Kreiranje instance

1. [cloud.oracle.com](https://cloud.oracle.com) → **Compute → Instances → Create instance**
2. **Image:** Canonical Ubuntu 22.04 ili 24.04
3. **Shape:** bilo koji označen sa *Always Free-eligible* (`VM.Standard.A1.Flex` ili `VM.Standard.E2.1.Micro`)
4. Sačuvaj privatni SSH ključ i zapamti **Public IP address**

> ARM ili AMD je svejedno — serverski modul je čist Java kod bez nativnih zavisnosti.

### 2. Otvaranje porta — **dva nivoa zaštite**

Oracle ima dva odvojena firewall-a i moraju se otvoriti **oba**.

**Security List** (u konzoli): **Networking → Virtual Cloud Networks →** tvoj VCN **→ Security Lists →** Default **→ Add Ingress Rules**

| Polje | Vrednost |
|---|---|
| Source CIDR | `0.0.0.0/0` |
| IP Protocol | TCP |
| Destination Port Range | `5555` |

**iptables** (preko SSH) — Oracle-ove Ubuntu slike blokiraju sve osim SSH-a:

```bash
ssh -i putanja/do/kljuca.key ubuntu@JAVNA_IP_ADRESA
```

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 5555 -j ACCEPT && sudo netfilter-persistent save
```

> Bez `netfilter-persistent save` pravilo nestaje posle restarta mašine.

### 3. Java, build i servis

```bash
sudo apt update && sudo apt install -y openjdk-21-jdk maven git
```

```bash
git clone https://github.com/MilosVasiljevic20230185/rmtlazov.git && cd rmtlazov && mvn clean install -DskipTests
```

```bash
sudo mkdir -p /opt/lazes && sudo cp server/target/lazes-server.jar /opt/lazes/ && sudo cp deploy/lazes-server.service /etc/systemd/system/
```

```bash
sudo systemctl daemon-reload && sudo systemctl enable --now lazes-server
```

Server sada preživljava zatvaranje SSH sesije i restart mašine. Logovi uživo:

```bash
sudo journalctl -u lazes-server -f
```

### 4. Ažuriranje posle izmena u kodu

```bash
cd ~/rmtlazov && git pull && mvn clean install -DskipTests && sudo cp server/target/lazes-server.jar /opt/lazes/ && sudo systemctl restart lazes-server
```

---

## Rešavanje problema

| Problem | Uzrok i rešenje |
|---|---|
| „Nije moguće povezati se" | Skoro uvek firewall. Na Windows hostu pokreni `netsh` pravilo iznad; na Oracle VM-u proveri **oba** firewall-a. |
| Radilo pa prestalo posle restarta VM-a | Nije pokrenuto `netfilter-persistent save`. |
| `systemctl status` pokazuje `failed` | `journalctl -u lazes-server -n 50` — obično nedostaje jar u `/opt/lazes/` ili Java nije instalirana. |
| Port 5555 zauzet | Pokreni server na drugom portu (`java -jar lazes-server.jar 5556`) i taj broj upiši u klijentu. |

Provera da li je port dostupan spolja, sa drugog računara:

```powershell
Test-NetConnection ADRESA_SERVERA -Port 5555
```
