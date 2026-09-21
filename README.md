<div align="center">

# ChunkClaim

**Chunk-based land claiming for Paper / Purpur 26.2 — one control block, a hologram, a full GUI, animated border preview and Vault / diamond economy.**

[![Build](https://github.com/alpyxd/ChunkClaim/actions/workflows/build.yml/badge.svg)](https://github.com/alpyxd/ChunkClaim/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/alpyxd/ChunkClaim?include_prereleases)](https://github.com/alpyxd/ChunkClaim/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Paper](https://img.shields.io/badge/Paper%2FPurpur-26.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

[English](#english) · [Türkçe](#türkçe)

</div>

---

## English

### ✨ Features

**Claiming**
- Place the **Claim Control Block** (Lodestone by default — craftable or `/claim give`) and the chunk you're standing in is yours. Name it right away in chat.
- Grow by claiming **adjacent chunks** from the live chunk map; release them again for a partial refund. The area can never be split into islands.
- The block **cannot be broken** by anyone. Deleting a claim takes a confirmation menu **and** typing the claim name in chat — and refunds upgrades + chunks.
- Move the control block anywhere inside its origin chunk.
- Right-click the block with a **compass** to get a *Claim Compass*: custom name, owner in the lore, always points to the control block (updates when the block moves; unlinks if the claim is deleted).

**Management GUI** (right-click the block or `/claim`)

| Row | Items |
|---|---|
| Info | Claim name, owner, chunk / member counts, active economy |
| Management | **Settings** (visitor toggles) · **Protections** (toggle purchased protections) · **Upgrades** · **Members** |
| Area | **Chunks** (9×5 live map) · **Show border** · **Teleport** (shift-click: set home) |
| Owner | **Rename** · **Move block** · **Delete** |

- **Settings** — 11 visitor toggles: place, break, interact (doors/buttons), containers, PvP, harm animals, item pickup/drop, use items (buckets, flint & steel…), entity interaction, enter area.
- **Protections** — explosion / fire / mob-spawn protection can be turned on or off once purchased, without losing the upgrade.
- **Upgrades** — chunk limit, member limit and the three protections. Levels, values and prices are fully configurable.
- **Members** — add by typing a name in chat, shift-click to remove, click a head to open the **per-member permission editor** (15 permissions, basic + management).
- **Chunks** — yellow = claimable (click to buy), green = yours (shift-click to release), red = someone else's, with prices and refunds shown.

**Hologram** — a `TextDisplay` above the block with claim name, owner, chunk and member counts. Lines are customizable per language.

**Animated border preview** (`/border`)
- Square claims: a client-side world border grows smoothly from the centre, holds, then shrinks away. It auto-hides when you walk up to an edge, so it never blocks movement.
- Any other shape (L, T, strips…): the **exact outline** is drawn with particles that spread outward from the origin chunk.

**Teleport** (`/chome`) — warmup countdown, cancel on move / damage, cooldown, optional cost. Home point defaults to the control block; set it anywhere inside the claim.

**Economy** — `AUTO` (Vault if present, else diamonds), `VAULT` or `DIAMOND`. Diamond mode counts diamond blocks as 9 and gives change. Every price has a Vault value and a diamond value.

**Protection** — blocks, containers (incl. the double-chest-across-border trick), interactions, PvP, animals, item frames / armor stands / vehicles, explosions, fire (incl. flaming arrows), pistons and liquids crossing the border, dispensers firing into claims, enderman / wither / ravager griefing, no-enter zones. Owner and members are exempt according to their permissions.

**Localization** — `language: en` / `tr`. *Every* string (messages, GUI, hologram, item names, even command aliases) lives in `lang/<code>.yml`; missing keys fall back to the bundled English file. A new language is copy → translate.

**Safe by design** — chat-prompt timeouts, MiniMessage-injection-safe input, refunds go to the owner only, two-phase block placement (plays nicely with WorldGuard & co.), no privilege escalation between members.

### 📦 Installation

1. Requires **Paper or Purpur 26.2** and **Java 25**.
2. Download the latest jar from [Releases](https://github.com/alpyxd/ChunkClaim/releases) and drop it into `plugins/`.
3. *(Optional)* Install [Vault](https://www.spigotmc.org/resources/vault.34315/) + an economy plugin for money-based pricing. Without Vault, diamonds are used automatically.
4. Start the server, then edit `plugins/ChunkClaim/config.yml` and `lang/*.yml`. `/claim reload` applies everything except command aliases (those need a restart).

### ⌨️ Commands

| Command | Description | Permission |
|---|---|---|
| `/claim` | Open the management menu (your claim, or the claim you're in as a member) | `chunkclaim.use` |
| `/claim info` | Who owns the chunk you're standing in | `chunkclaim.use` |
| `/border` (`/claim border`) | Animated border preview | `chunkclaim.use` |
| `/chome` (`/claim home`) | Teleport to your claim | `chunkclaim.use` |
| `/claim sethome` | Set the teleport point to your position | `chunkclaim.use` |
| `/claim claim` / `unclaim` | Claim / release the chunk you're standing in | `chunkclaim.use` |
| `/claim moveblock` | Move the control block within the origin chunk (owner) | `chunkclaim.use` |
| `/claim delete` | Delete the claim (confirmation menu + type the name) | `chunkclaim.use` |
| `/claim give <player> [amount]` | Give claim control blocks | `chunkclaim.admin` |
| `/claim reload` | Reload config & language files | `chunkclaim.admin` |

Extra aliases per language (e.g. `/sinir`, `/alan`, `/cev` in Turkish) are defined under `commands:` in the language file.

**Permissions:** `chunkclaim.use` (default: everyone), `chunkclaim.admin` (op — full access to every claim), `chunkclaim.bypass` (op — ignores all protection).

### 👥 Member permissions

| Basic (default **on**) | Management (default **off**, owner-only to grant) |
|---|---|
| Place blocks · Break blocks · Interact · Containers · Use items · Entities / animals · Teleport | Claim chunks · Release chunks · Manage settings · Manage protections · Manage members · Buy upgrades · Rename · Set home |

A member with *Manage members* can add members and edit others' basic permissions, but cannot touch other managers or grant management permissions.

### ⚙️ Configuration highlights

```yaml
language: en                      # en | tr

economy:
  type: AUTO                      # AUTO | VAULT | DIAMOND
  chunk-price:
    vault:   { base: 100, per-chunk: 50 }
    diamond: { base: 2,   per-chunk: 1 }
  unclaim-refund: 0.5             # releasing a chunk (owner only)
  delete-refund: 0.5              # deleting the claim: upgrades + chunks (owner only)

upgrades:
  max-chunks:
    base: 4
    levels:
      - { cost: 500, diamond: 8, value: 9 }
      # ...
  explosion-protection:           # toggle-type upgrades: base 1 = always on
    base: 0
    levels: [ { cost: 2000, diamond: 20, value: 1 } ]

border:
  mode: AUTO                      # AUTO | WORLD_BORDER | PARTICLES
  particle-color: "#55FF55"
  expand-ms: 1500
  hold-ms: 4000
  shrink-ms: 1000
  auto-hide-distance: 2.0

teleport: { warmup-seconds: 3, cooldown-seconds: 30, cancel-on-move: true, cost: 0 }

creation: { ask-name: true, max-name-length: 24, name-pattern: "^[\\p{L}\\p{N} _'\\-.!?]+$" }

hologram: { enabled: true, y-offset: 1.6, update-interval-ticks: 100 }
compass:  { enabled: true }        # compass + control block = Claim Compass
```

Claims are stored as `plugins/ChunkClaim/claims/<uuid>.yml`. All text (messages, GUI, hologram lines, item names, command aliases) is in `plugins/ChunkClaim/lang/<code>.yml`.

### 🔨 Building from source

```bash
git clone https://github.com/alpyxd/ChunkClaim.git
cd ChunkClaim
./gradlew build          # Windows: .\gradlew.bat build
```

The jar lands in `build/libs/`. The Gradle wrapper is included; only a JDK 25 is needed.

### 🤝 Contributing

Issues and pull requests are welcome. For a new language, copy `src/main/resources/lang/en.yml`, translate it and open a PR.

---

## Türkçe

### ✨ Özellikler

**Claim alma**
- **Claim Yönetim Bloğu**'nu (varsayılan Lodestone — tarif veya `/claim give`) yerleştir, bulunduğun chunk senin olsun. İsmini hemen sohbetten ver.
- Canlı chunk haritasından **komşu chunk'lar** alarak büyü; bırakınca kısmi iade. Alan hiçbir zaman adalara bölünemez.
- Blok **kimse tarafından kırılamaz**. Silmek için onay menüsü **ve** claim ismini sohbete yazmak gerekir — geliştirmeler ve chunk'lar iade edilir.
- Bloğu ana chunk içinde istediğin yere taşı.
- Bloğa **pusulayla** sağ tıkla → *Claim Pusulası*: özel isim, lore'da sahip, her zaman yönetim bloğunu gösterir (blok taşınınca güncellenir, claim silinince bağlantısı kopar).

**Yönetim menüsü** (bloğa sağ tık veya `/claim`)

| Satır | Öğeler |
|---|---|
| Bilgi | İsim, sahip, chunk / üye sayısı, aktif ekonomi |
| Yönetim | **Ayarlar** (ziyaretçi izinleri) · **Korumalar** (satın alınanları aç/kapa) · **Geliştirmeler** · **Üyeler** |
| Alan | **Chunklar** (9×5 canlı harita) · **Sınırı göster** · **Işınlan** (shift+tık: ev ayarla) |
| Sahip | **İsim değiştir** · **Bloğu taşı** · **Sil** |

- **Ayarlar** — 11 ziyaretçi aç/kapa: blok koyma/kırma, etkileşim, sandık, PvP, hayvan, eşya alma/atma, eşya kullanma, canlı etkileşimi, alana giriş.
- **Korumalar** — patlama / yangın / canavar koruması satın alındıktan sonra geliştirmeyi kaybetmeden açılıp kapatılabilir.
- **Geliştirmeler** — chunk limiti, üye limiti ve üç koruma. Seviyeler, değerler ve fiyatlar config'den.
- **Üyeler** — sohbetten isimle ekle, shift+tık ile çıkar, kafaya tıklayınca **üye başına izin editörü** (15 izin: temel + yönetim).
- **Chunklar** — sarı = alınabilir (tıkla), yeşil = senin (shift+tık bırak), kırmızı = başkasının; fiyat ve iadeler görünür.

**Hologram** — bloğun üstünde isim, sahip, chunk ve üye sayısı; satırlar dil dosyasından.

**Animasyonlu sınır önizlemesi** (`/border`)
- Kare claim: istemci tarafı dünya sınırı merkezden yumuşakça büyür, bekler, küçülür. Kenara yaklaşınca otomatik kapanır, hareketi engellemez.
- Diğer şekiller (L, T, şerit…): claim'in **gerçek çevresi** ana chunk'tan dışa yayılan partiküllerle çizilir.

**Işınlanma** (`/chome`) — geri sayım, hareket/hasar ile iptal, cooldown, isteğe bağlı ücret. Ev noktası varsayılan blok üstü; claim içinde istediğin yere ayarlanabilir.

**Ekonomi** — `AUTO` (Vault varsa Vault, yoksa elmas), `VAULT` veya `DIAMOND`. Elmas modu elmas bloğunu 9 sayar, para üstü verir. Her fiyatın Vault ve elmas değeri ayrı.

**Koruma** — blok, sandık (sınır ötesi çift sandık dahil), etkileşim, PvP, hayvan, tablo/zırh askısı/araç, patlama, yangın (alev oku dahil), sınırı geçen piston ve sıvı, içeri püskürten dispenser, enderman/wither/ravager, giriş yasağı. Sahip ve üyeler izinlerine göre muaf.

**Dil desteği** — `language: en` / `tr`. Mesajlar, GUI, hologram, eşya isimleri ve komut alias'ları dahil *her şey* `lang/<kod>.yml`'de; eksik anahtarlar jar'daki İngilizce dosyadan tamamlanır.

### 📦 Kurulum

1. **Paper veya Purpur 26.2** ve **Java 25** gerekir.
2. [Releases](https://github.com/alpyxd/ChunkClaim/releases) sayfasından jar'ı indirip `plugins/` klasörüne at.
3. *(İsteğe bağlı)* Para ekonomisi için [Vault](https://www.spigotmc.org/resources/vault.34315/) + bir ekonomi eklentisi kur. Vault yoksa otomatik elmas kullanılır.
4. Sunucuyu başlat, `config.yml` içinde `language: tr` yap, `lang/tr.yml`'i dilediğin gibi düzenle. `/claim reload` komut alias'ları dışında her şeyi uygular (alias'lar restart ister).

### ⌨️ Komutlar

| Komut | Açıklama | Yetki |
|---|---|---|
| `/claim` | Yönetim menüsü (kendi claim'in veya üyesi olduğun claim) | `chunkclaim.use` |
| `/claim info` | Bulunduğun chunk kimin | `chunkclaim.use` |
| `/border` (`/sinir`, `/alan`) | Animasyonlu sınır önizlemesi | `chunkclaim.use` |
| `/chome` (`/cev`) | Claim'ine ışınlan | `chunkclaim.use` |
| `/claim sethome` | Işınlanma noktasını buraya ayarla | `chunkclaim.use` |
| `/claim claim` / `unclaim` | Bulunduğun chunk'ı al / bırak | `chunkclaim.use` |
| `/claim moveblock` | Yönetim bloğunu ana chunk içinde taşı (sahip) | `chunkclaim.use` |
| `/claim delete` | Claim'i sil (onay menüsü + isim yazma) | `chunkclaim.use` |
| `/claim give <oyuncu> [adet]` | Claim bloğu ver | `chunkclaim.admin` |
| `/claim reload` | Config ve dil dosyalarını yenile | `chunkclaim.admin` |

**Yetkiler:** `chunkclaim.use` (herkes), `chunkclaim.admin` (op — tüm claim'lerde tam erişim), `chunkclaim.bypass` (op — korumaları atlar).

### 👥 Üye izinleri

| Temel (varsayılan **açık**) | Yönetim (varsayılan **kapalı**, sadece sahip verir) |
|---|---|
| Blok koyma · Blok kırma · Etkileşim · Sandık · Eşya kullanma · Canlı/hayvan · Işınlanma | Chunk alma · Chunk bırakma · Ayarlar · Koruma yönetimi · Üye yönetimi · Geliştirme satın alma · İsim · Ev noktası |

*Üye yönetimi* izni olan üye başkalarını ekleyip temel izinlerini düzenleyebilir; diğer yöneticilere dokunamaz, yönetim izni veremez.

### ⚙️ Öne çıkan ayarlar

```yaml
language: tr                      # en | tr

economy:
  type: AUTO                      # AUTO | VAULT | DIAMOND
  chunk-price:
    vault:   { base: 100, per-chunk: 50 }
    diamond: { base: 2,   per-chunk: 1 }
  unclaim-refund: 0.5             # chunk bırakma iadesi (sadece sahibe)
  delete-refund: 0.5              # claim silme iadesi: geliştirmeler + chunklar (sadece sahibe)

upgrades:
  max-chunks:
    base: 4
    levels:
      - { cost: 500, diamond: 8, value: 9 }
      # ...
  explosion-protection:           # aç/kapa tipi geliştirmeler: base 1 = her zaman açık
    base: 0
    levels: [ { cost: 2000, diamond: 20, value: 1 } ]

border:
  mode: AUTO                      # AUTO | WORLD_BORDER | PARTICLES
  particle-color: "#55FF55"
  expand-ms: 1500
  hold-ms: 4000
  shrink-ms: 1000
  auto-hide-distance: 2.0

teleport: { warmup-seconds: 3, cooldown-seconds: 30, cancel-on-move: true, cost: 0 }

creation: { ask-name: true, max-name-length: 24 }
hologram: { enabled: true, y-offset: 1.6 }
compass:  { enabled: true }        # pusula + yönetim bloğu = Claim Pusulası
```

Claim'ler `plugins/ChunkClaim/claims/<uuid>.yml` dosyalarında; tüm metinler `plugins/ChunkClaim/lang/<kod>.yml` içinde.

### 🔨 Kaynaktan derleme

```bash
git clone https://github.com/alpyxd/ChunkClaim.git
cd ChunkClaim
.\gradlew.bat build      # Linux/macOS: ./gradlew build
```

Jar `build/libs/` altına çıkar. Gradle wrapper dahil; sadece JDK 25 gerekir.

---

<div align="center">
MIT © 2026 Alpay
</div>
