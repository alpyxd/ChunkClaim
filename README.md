<div align="center">

# ChunkClaim

**Chunk-based land claiming for Paper / Purpur 26.2 — one control block, a hologram, a full GUI, animated border preview and Vault / diamond economy.**

[![Build](https://github.com/AlpayTaner/ChunkClaim/actions/workflows/build.yml/badge.svg)](https://github.com/AlpayTaner/ChunkClaim/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/AlpayTaner/ChunkClaim?include_prereleases)](https://github.com/AlpayTaner/ChunkClaim/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Paper](https://img.shields.io/badge/Paper%2FPurpur-26.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

[English](#english) · [Türkçe](#türkçe)

</div>

---

## English

### ✨ Features

- **Control block** — place the *Claim Control Block* (Lodestone by default, craftable or `/claim give`) and the chunk you're standing in becomes yours. Right-click it to manage everything. The block **cannot be broken**; deleting a claim requires a two-step confirmation.
- **Hologram** — a `TextDisplay` above the block shows the claim name, owner, chunk and member counts (fully customizable lines).
- **GUI-driven management** — main menu with sub-menus:
  - **Settings** — 11 visitor toggles: place, break, interact (doors/buttons), containers, PvP, harm animals, item pickup/drop, use items (buckets, flint & steel…), entity interaction, enter area.
  - **Upgrades** — chunk limit, member limit, explosion protection, fire protection, mob-spawn block. Levels and prices are configurable.
  - **Members** — add by typing a name in chat, remove with shift-click, and click a head to open the **per-member permission editor** (14 permissions, basic + management).
  - **Chunks** — a live 9×5 chunk map: yellow = claimable (click to buy), green = yours (shift-click to release), red = someone else's.
  - **Teleport / Set home**, **Rename**, **Show border**, **Delete** (owner only).
- **Animated border preview** — a client-side world border grows smoothly from the origin chunk's centre to cover the whole claim, holds, then shrinks away. It auto-hides when you approach an edge so it never blocks movement. `/border`.
- **Teleport** — `/chome` with warmup countdown, cancel-on-move/damage, cooldown and optional cost. Home point defaults to the control block, or set anywhere inside the claim.
- **Economy** — `AUTO` (Vault if present, else diamonds), `VAULT` or `DIAMOND`. Diamond mode counts diamond blocks as 9 and gives change. Separate price sets for both.
- **Protection** — blocks, containers (incl. the double-chest-across-border exploit), interactions, PvP, animals, item frames / armor stands / vehicles, explosions, fire (incl. flaming arrows), pistons and liquids crossing the border, dispensers firing into claims, enderman/wither/ravager griefing, no-enter zones. Owner and members are exempt according to their permissions.
- **Localization** — `language: tr` / `en`. *Every* string, including GUI text, hologram lines and item names, lives in `lang/<code>.yml`. Missing keys fall back to the bundled defaults, so adding a language is copy → translate.
- **Safe by design** — chat-prompt timeouts, MiniMessage-injection-safe player input, refunds only to the owner, two-phase block placement (plays nicely with WorldGuard & co.).

### 📦 Installation

1. Requires **Paper or Purpur 26.2** and **Java 25**.
2. Download the latest jar from [Releases](https://github.com/AlpayTaner/ChunkClaim/releases) and drop it into `plugins/`.
3. *(Optional)* Install [Vault](https://www.spigotmc.org/resources/vault.34315/) + an economy plugin for money-based pricing. Without Vault, diamonds are used automatically.
4. Start the server, then edit `plugins/ChunkClaim/config.yml` and `lang/*.yml` to taste. `/claim reload` applies changes.

### ⌨️ Commands

| Command | Description | Permission |
|---|---|---|
| `/claim` | Open the management menu (own claim, or the claim you're in as a member) | `chunkclaim.use` |
| `/claim info` | Who owns the chunk you're standing in | `chunkclaim.use` |
| `/border` (`/claim border`) | Animated border preview of the claim you're in / your claim | `chunkclaim.use` |
| `/chome` (`/claim home`) | Teleport to your claim | `chunkclaim.use` |
| `/claim sethome` | Set the teleport point to your position | `chunkclaim.use` |
| `/claim claim` / `unclaim` | Claim / release the chunk you're standing in | `chunkclaim.use` |
| `/claim delete` | Delete the claim (confirmation menu + type the name) | `chunkclaim.use` |
| `/claim give <player> [amount]` | Give claim control blocks | `chunkclaim.admin` |
| `/claim reload` | Reload config & language files | `chunkclaim.admin` |

**Permissions:** `chunkclaim.use` (default: everyone), `chunkclaim.admin` (op — full access to every claim), `chunkclaim.bypass` (op — ignores all protection).

### 👥 Member permissions

| Basic (default **on**) | Management (default **off**, owner-only to grant) |
|---|---|
| Place blocks · Break blocks · Interact · Containers · Use items · Entities / animals · Teleport | Claim chunks · Release chunks · Manage settings · Manage members · Buy upgrades · Rename · Set home |

A member with *Manage members* can add members and edit others' basic permissions, but cannot touch other managers or grant management permissions — no privilege escalation.

### ⚙️ Configuration highlights

```yaml
language: tr                      # tr | en
economy:
  type: AUTO                      # AUTO | VAULT | DIAMOND
  chunk-price:
    vault:   { base: 100, per-chunk: 50 }
    diamond: { base: 2,   per-chunk: 1 }
  unclaim-refund: 0.5             # owner only
upgrades:
  max-chunks:
    base: 4
    levels:
      - { cost: 500, diamond: 8, value: 9 }
      # ...
border:  { expand-ms: 1500, hold-ms: 4000, shrink-ms: 1000, auto-hide-distance: 2.0 }
teleport: { warmup-seconds: 3, cooldown-seconds: 30, cancel-on-move: true }
```

Claims are stored as `plugins/ChunkClaim/claims/<uuid>.yml`.

> **Note:** explosion and fire protection are *upgrades* (off by default). To make them always-on, set `base: 1` and clear the `levels` list for that upgrade.

### 🔨 Building from source

```bash
git clone https://github.com/AlpayTaner/ChunkClaim.git
cd ChunkClaim
./gradlew build          # Windows: .\gradlew.bat build
```

The jar lands in `build/libs/`. Gradle wrapper is included; only a JDK 25 is needed.

### 🤝 Contributing

Issues and pull requests are welcome. For a new language, copy `src/main/resources/lang/en.yml`, translate it and open a PR.

---

## Türkçe

### ✨ Özellikler

- **Yönetim bloğu** — *Claim Yönetim Bloğu*'nu (varsayılan Lodestone; tarif veya `/claim give`) yerleştirdiğin an bulunduğun chunk senin olur. Sağ tık ile her şeyi yönetirsin. Blok **kırılamaz**; claim silmek iki aşamalı onay ister.
- **Hologram** — bloğun üstünde isim, sahip, chunk ve üye sayısı (satırlar özelleştirilebilir).
- **GUI ile yönetim** — ana menü ve alt menüler:
  - **Ayarlar** — 11 ziyaretçi aç/kapa: blok koyma/kırma, etkileşim, sandık, PvP, hayvan, eşya alma/atma, eşya kullanma, canlı etkileşimi, alana giriş.
  - **Geliştirmeler** — chunk limiti, üye limiti, patlama/yangın koruması, canavar engeli. Seviyeler ve fiyatlar config'de.
  - **Üyeler** — sohbetten isimle ekle, shift+tık ile çıkar, kafaya tıklayınca **üye başına izin editörü** (14 izin: temel + yönetim).
  - **Chunklar** — canlı 9×5 chunk haritası: sarı = alınabilir (tıkla), yeşil = senin (shift+tık bırak), kırmızı = başkasının.
  - **Işınlan / Ev ayarla**, **İsim değiştir**, **Sınırı göster**, **Sil** (sadece sahip).
- **Animasyonlu sınır önizlemesi** — istemci tarafı dünya sınırı ana chunk'ın merkezinden tüm alanı kaplayana kadar yumuşakça büyür, bekler, küçülerek kaybolur. Kenara yaklaşınca otomatik kapanır, hareketi asla engellemez. `/border`.
- **Işınlanma** — `/chome`: geri sayım, hareket/hasar ile iptal, cooldown, isteğe bağlı ücret. Ev noktası varsayılan blok üstü, claim içinde istediğin yere ayarlanabilir.
- **Ekonomi** — `AUTO` (Vault varsa Vault, yoksa elmas), `VAULT` veya `DIAMOND`. Elmas modu elmas bloğunu 9 sayar, para üstü verir. İki ayrı fiyat seti.
- **Koruma** — blok, sandık (sınır ötesi çift sandık açığı dahil), etkileşim, PvP, hayvan, tablo/zırh askısı/araç, patlama, yangın (alev oku dahil), sınırı geçen piston ve sıvı, içeri püskürten dispenser, enderman/wither/ravager, giriş yasağı.
- **Dil desteği** — `language: tr` / `en`. GUI dahil *tüm* metinler `lang/<kod>.yml`'de; eksik anahtarlar jar'daki varsayılandan tamamlanır.

### 📦 Kurulum

1. **Paper veya Purpur 26.2** ve **Java 25** gerekir.
2. [Releases](https://github.com/AlpayTaner/ChunkClaim/releases) sayfasından jar'ı indirip `plugins/` klasörüne at.
3. *(İsteğe bağlı)* Para ekonomisi için [Vault](https://www.spigotmc.org/resources/vault.34315/) + bir ekonomi eklentisi kur. Vault yoksa otomatik elmas kullanılır.
4. Sunucuyu başlat, `plugins/ChunkClaim/config.yml` ve `lang/*.yml` dosyalarını düzenle. `/claim reload` ile uygula.

### ⌨️ Komutlar

| Komut | Açıklama | Yetki |
|---|---|---|
| `/claim` | Yönetim menüsü (kendi claim'in veya üyesi olduğun claim) | `chunkclaim.use` |
| `/claim info` | Bulunduğun chunk kimin | `chunkclaim.use` |
| `/border` (`/sinir`, `/alan`) | Bulunduğun / kendi claim'inin sınır önizlemesi | `chunkclaim.use` |
| `/chome` (`/cev`) | Claim'ine ışınlan | `chunkclaim.use` |
| `/claim sethome` | Işınlanma noktasını buraya ayarla | `chunkclaim.use` |
| `/claim claim` / `unclaim` | Bulunduğun chunk'ı al / bırak | `chunkclaim.use` |
| `/claim delete` | Claim'i sil (onay menüsü + isim yazma) | `chunkclaim.use` |
| `/claim give <oyuncu> [adet]` | Claim bloğu ver | `chunkclaim.admin` |
| `/claim reload` | Config ve dil dosyalarını yenile | `chunkclaim.admin` |

**Yetkiler:** `chunkclaim.use` (herkes), `chunkclaim.admin` (op — tüm claim'lerde tam erişim), `chunkclaim.bypass` (op — korumaları atlar).

### 👥 Üye izinleri

| Temel (varsayılan **açık**) | Yönetim (varsayılan **kapalı**, sadece sahip verir) |
|---|---|
| Blok koyma · Blok kırma · Etkileşim · Sandık · Eşya kullanma · Canlı/hayvan · Işınlanma | Chunk alma · Chunk bırakma · Ayarlar · Üye yönetimi · Geliştirme satın alma · İsim · Ev noktası |

*Üye yönetimi* izni olan üye başkalarını ekleyip temel izinlerini düzenleyebilir; diğer yöneticilere dokunamaz, yönetim izni veremez.

### 🔨 Kaynaktan derleme

```bash
git clone https://github.com/AlpayTaner/ChunkClaim.git
cd ChunkClaim
.\gradlew.bat build      # Linux/macOS: ./gradlew build
```

Jar `build/libs/` altına çıkar. Gradle wrapper dahil; sadece JDK 25 gerekir.

> **Not:** Patlama ve yangın koruması *geliştirme* olarak satılır (varsayılan kapalı). Her zaman açık olsun istersen ilgili geliştirmede `base: 1` yapıp `levels` listesini boşalt.

---

<div align="center">
MIT © 2026 Alpay
</div>
