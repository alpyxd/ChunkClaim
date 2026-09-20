package dev.alpay.chunkclaim.economy;

import org.bukkit.entity.Player;

/** Ekonomi soyutlaması: Vault veya elmas tabanlı ödeme. */
public interface EconomyProvider {

    String name();

    /** Elmas tabanlı ise config'deki "diamond" fiyat seti kullanılır. */
    boolean usesDiamonds();

    double getBalance(Player player);

    boolean has(Player player, double amount);

    /** Başarılıysa true. Yetersiz bakiyede hiçbir şey çekilmez. */
    boolean withdraw(Player player, double amount);

    boolean deposit(Player player, double amount);

    String format(double amount);
}
