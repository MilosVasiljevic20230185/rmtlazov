# Hostovanje servera na Oracle Cloud (Always Free)

Uputstvo za pokretanje **Lažeš** servera na besplatnoj virtuelnoj mašini, tako da članovi tima mogu da igraju partiju preko interneta.

Klijent se i dalje pokreće lokalno na svakom računaru — hostuje se **samo server**.

---

## Zašto virtuelna mašina, a ne Koyeb / Render

Igra koristi **sirovi TCP soket**, a ne HTTP. Platforme tipa Koyeb i Render javne servise rutiraju kroz HTTP proxy, pa raw TCP tamo radi samo uz posebnu (plaćenu) opciju. Obična virtuelna mašina daje javnu IP adresu sa punim TCP-om, bez ograničenja.

---

## 1. Kreiranje instance

1. Prijavi se na [cloud.oracle.com](https://cloud.oracle.com) → **Compute → Instances → Create instance**
2. **Image:** Canonical Ubuntu 22.04 ili 24.04
3. **Shape:** bilo koji označen sa *Always Free-eligible*
   - `VM.Standard.A1.Flex` (ARM) — 2 OCPU / 12 GB, ili
   - `VM.Standard.E2.1.Micro` (AMD) — sasvim dovoljno za ovu igru
4. U sekciji **Add SSH keys** sačuvaj privatni ključ (`.key` fajl) — bez njega nema pristupa mašini
5. Zapamti **Public IP address** instance

> ARM ili AMD je svejedno — serverski modul je čist Java kod bez nativnih zavisnosti.

---

## 2. Otvaranje porta 5555 — **dva nivoa zaštite**

Ovo je najčešći uzrok „server radi ali niko ne može da se poveže". Oracle ima **dva odvojena firewall-a** i moraju se otvoriti oba.

### 2a. Oracle Security List (u konzoli)

**Networking → Virtual Cloud Networks →** tvoj VCN **→ Security Lists →** Default Security List **→ Add Ingress Rules**

| Polje | Vrednost |
|---|---|
| Source Type | CIDR |
| Source CIDR | `0.0.0.0/0` |
| IP Protocol | TCP |
| Destination Port Range | `5555` |

### 2b. iptables na samoj mašini (preko SSH)

Oracle-ove Ubuntu slike dolaze sa iptables pravilima koja blokiraju sve osim SSH-a. Poveži se:

```bash
ssh -i putanja/do/kljuca.key ubuntu@JAVNA_IP_ADRESA
```

pa otvori port i **trajno sačuvaj pravilo**:

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 5555 -j ACCEPT
sudo netfilter-persistent save
```

> Bez `netfilter-persistent save` pravilo nestaje posle restarta mašine.

---

## 3. Instalacija Jave i build projekta

```bash
sudo apt update && sudo apt install -y openjdk-21-jdk maven git
```

```bash
git clone https://github.com/MilosVasiljevic20230185/rmtlazov.git
cd rmtlazov && mvn clean install -DskipTests
```

---

## 4. Server kao systemd servis

Ovako server ostaje pokrenut i kada zatvoriš SSH sesiju, i sam se podiže posle restarta mašine.

```bash
sudo mkdir -p /opt/lazes
sudo cp ~/rmtlazov/server/target/lazes-server.jar /opt/lazes/
sudo cp ~/rmtlazov/deploy/lazes-server.service /etc/systemd/system/
```

```bash
sudo systemctl daemon-reload && sudo systemctl enable --now lazes-server
```

Provera da radi:

```bash
sudo systemctl status lazes-server
```

Praćenje logova uživo (vidiš svako povezivanje, sobu i potez):

```bash
sudo journalctl -u lazes-server -f
```

---

## 5. Povezivanje igrača

Svaki igrač pokreće klijent kod sebe:

```bash
java -jar client/target/lazes-client.jar
```

U glavnom meniju:

| Polje | Vrednost |
|---|---|
| Server | javna IP adresa instance |
| Port | `5555` |

Jedan igrač pravi sobu i deli šestoznamenkasti kod, ostali se pridružuju tim kodom.

---

## 6. Ažuriranje servera posle izmena u kodu

```bash
cd ~/rmtlazov && git pull && mvn clean install -DskipTests
```

```bash
sudo cp server/target/lazes-server.jar /opt/lazes/ && sudo systemctl restart lazes-server
```

---

## Rešavanje problema

| Problem | Uzrok i rešenje |
|---|---|
| Klijent javlja „Nije moguće povezati se" | Skoro uvek firewall. Proveri **oba** koraka iz sekcije 2 — najčešće nedostaje iptables pravilo. |
| Radilo pa prestalo posle restarta | Nije pokrenuto `netfilter-persistent save`. |
| `systemctl status` pokazuje `failed` | Pogledaj `journalctl -u lazes-server -n 50`. Obično nedostaje jar na `/opt/lazes/` ili nije instalirana Java. |
| Port 5555 zauzet | Promeni port u `.service` fajlu i u Security List pravilu, pa `sudo systemctl daemon-reload && sudo systemctl restart lazes-server`. |

Brza provera da li je port otvoren spolja, sa svog računara:

```bash
Test-NetConnection JAVNA_IP_ADRESA -Port 5555
```
