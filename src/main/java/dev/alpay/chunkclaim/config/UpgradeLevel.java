package dev.alpay.chunkclaim.config;

/** Bir geliştirmenin tek seviyesi: Vault fiyatı, elmas fiyatı ve o seviyedeki değer. */
public record UpgradeLevel(double cost, double diamondCost, int value) {
}
