package com.muvrinovci.lazes.client;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Otisak uredjaja na kome klijent radi.
 *
 * Sluzi iskljucivo da server prepozna igraca koji se vraca posle prekida veze,
 * i to samo sa istog uredjaja - otisak se ne moze preneti na drugi racunar.
 * Salje se kao heks heš, sirova MAC adresa nikada ne napusta masinu.
 *
 * Racuna se jednom pri prvom pozivu i ostaje u memoriji; nista se ne upisuje
 * na disk, pa nema profila ni sesije koja bi preživela gasenje aplikacije.
 */
public final class DeviceId {

    private static final int LENGTH = 32;

    private static String cached;

    private DeviceId() {
    }

    public static synchronized String get() {
        if (cached == null) {
            cached = compute();
        }
        return cached;
    }

    private static String compute() {
        String raw = firstStableMac() + "|" + hostname() + "|" + System.getProperty("user.name", "");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, LENGTH);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 postoji na svakoj Java platformi; ako ipak ne postoji,
            // povratak u partiju nece raditi, ali klijent mora da nastavi.
            return UUID.randomUUID().toString().replace("-", "").substring(0, LENGTH);
        }
    }

    /**
     * MAC adresa prve stabilne mrezne kartice.
     *
     * Adrese se sortiraju pa se uzima najmanja, tako da ukljucivanje VPN-a ili
     * dokovanje laptopa ne promeni otisak. Virtuelne i loopback kartice se
     * preskacu iz istog razloga.
     */
    private static String firstStableMac() {
        List<String> addresses = new ArrayList<>();
        try {
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (network.isLoopback() || network.isVirtual() || network.isPointToPoint()) {
                    continue;
                }
                byte[] mac = network.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    addresses.add(HexFormat.of().formatHex(mac));
                }
            }
        } catch (Exception e) {
            // Bez mrezne kartice otisak se oslanja samo na ime masine i korisnika.
        }

        Collections.sort(addresses);
        return addresses.isEmpty() ? "" : addresses.get(0);
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            String fallback = System.getenv("COMPUTERNAME");
            return fallback == null ? "" : fallback;
        }
    }
}
