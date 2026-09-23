# 🥚 FancyEggs

Ein leichtgewichtiges Minecraft-Plugin für **Paper 26.1.2+**, das passives Einkommen durch **Eggs** ins Spiel bringt. Ideal für Idle-, Economy- oder Season-Server.

![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2+-brightgreen)
![Paper](https://img.shields.io/badge/Paper-API%2026.1.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## ✨ Features

- 🥚 **Dynamische Eggs** – beliebig viele über die `config.yml` hinzufügen
- 💰 **Passives Einkommen** – jede Sekunde wird Geld generiert
- ⬆️ **Upgrade-System** – Eggs leveln und den Ertrag steigern
- 💸 **Verkaufssystem** – Eggs gegen das 4-fache der Upgrade-Kosten verkaufen
- 🔄 **Auto-Collect** – Geld automatisch sammeln oder in einer Kiste horten
- 🎨 **Hex-Farben** – modernes Design mit `#FFD700` und Fett-Formatierung
- 🌍 **Mehrsprachig** – Deutsch (Standard) & Englisch per Config
- 📊 **bStats-Integration** – anonyme Nutzungsstatistiken
- 🔗 **PlaceholderAPI** – Top-Ranglisten, Einkommen, Egg-Anzahl, Rang
- 🏆 **Persistente Rangliste** – auch offline Spieler werden angezeigt (`tops.yml`)
- 📈 **K/M/B/T-Formatierung** – große Zahlen kompakt und lesbar
- 🎃 **Saisonale Eggs** – einfach per Config für Halloween, Weihnachten etc.

---

## 📋 Voraussetzungen

| Software | Version | Erforderlich |
|---|---|---|
| **Paper** | 26.1.2 oder höher | ✅ |
| **Java** | 25 | ✅ |
| **Vault** | beliebig | ✅ |
| **Economy-Plugin** | EssentialsX, CMI, etc. | ✅ |
| **PlaceholderAPI** | 2.11.6+ | ⚠️ Optional (für Placeholder) |

---

## 🚀 Installation

1. **Server vorbereiten**
   - Paper 26.1.2 installieren (läuft auf **Java 25**)
   - Vault + ein Economy-Plugin (EssentialsX empfohlen) installieren

2. **Plugin herunterladen**
   - Neueste `FancyEggs-x.x.jar` von [Releases](../../releases) herunterladen

3. **Plugin installieren**
   - `.jar` in den `plugins/`-Ordner legen
   - Server starten

4. **Fertig!**
   - `/fancyeggs` öffnet das Menü
   - `/fancyeggs give <Spieler> <EggKey>` gibt Eggs aus

---

## 🎮 Befehle

| Befehl | Beschreibung | Permission |
|---|---|---|
| `/fancyeggs` | Öffnet das Eggs-Menü | `fancyeggs.use` |
| `/fancyeggs list` | Zeigt alle Eggs mit Werten | `fancyeggs.use` |
| `/fancyeggs give <Spieler> <EggKey> [charged]` | Gibt einem Spieler ein Egg | `fancyeggs.admin` |
| `/fancyeggs reload` | Lädt die `config.yml` neu | `fancyeggs.admin` |
| `/eggs` | Alias für `/fancyeggs` | `fancyeggs.use` |
| `/fe` | Alias für `/fancyeggs` | `fancyeggs.use` |

### 🎁 Egg an einen Spieler geben

```
/fancyeggs give Steve Chicken_Egg
/fancyeggs give Steve Warden_Egg charged
```

> 💡 Der `charged`-Parameter gibt 25% mehr Einkommen (einstellbar in der Config).

---

## 🕹️ GUI-Steuerung

| Aktion | Effekt |
|---|---|
| **Linksklick** auf ein Egg | ⬆️ Upgrade (Kosten werden vom Konto abgezogen) |
| **Shift + Linksklick** auf ein Egg | 💸 Verkaufen (4x Upgrade-Preis) |
| **Klick auf grüne Scheibe** | Auto-Collect anschalten |
| **Klick auf rote Scheibe** | Auto-Collect ausschalten |
| **Klick auf Truhe** (wenn Auto-Collect aus) | Aufgesammeltes Geld einsammeln |

---

## 🔗 PlaceholderAPI

Alle Placeholder funktionieren mit **PlaceholderAPI**. Für Zahlen wird die **K/M/B/T/Q-Formatierung** verwendet.

### Eigene Werte

| Placeholder | Beispiel | Beschreibung |
|---|---|---|
| `%fancyeggs_total_income%` | `1.50M` | Eigene Produktion pro Sekunde |
| `%fancyeggs_total_income_raw%` | `1500000.0` | Rohe Zahl zum Weiterrechnen |
| `%fancyeggs_egg_count%` | `7` | Anzahl eigener Eggs |
| `%fancyeggs_rank%` | `5` | Eigener Rang in der Liste |
| `%fancyeggs_upgrade_cost_<key>%` | `5.00` | Upgrade-Kosten eines Eggs |

### Top-Rangliste

| Placeholder | Beispiel | Beschreibung |
|---|---|---|
| `%fancyeggs_top_1_name%` | `Steve` | Name Platz 1 |
| `%fancyeggs_top_1_value%` | `2.50M` | Wert Platz 1 formatiert |
| `%fancyeggs_top_1_value_raw%` | `2500000.0` | Wert Platz 1 roh |
| `%fancyeggs_top_2_name%` | `Alex` | Name Platz 2 |
| `%fancyeggs_top_total%` | `42` | Anzahl aller Einträge |

**Beispiel-Scoreboard:**
```yaml
lines:
  - "&6&lFANCYEGGS TOP"
  - "&e#1 &f%fancyeggs_top_1_name% &7- &a$%fancyeggs_top_1_value%/s"
  - "&e#2 &f%ancyeggs_top_2_name% &7- &a$%fancyeggs_top_2_value%/s"
  - "&e#3 &f%fancyeggs_top_3_name% &7- &a$%fancyeggs_top_3_value%/s"
  - " "
  - "&7Dein Rang: &e#%fancyeggs_rank%"
  - "&7Produktion: &a$%fancyeggs_total_income%/s"
```

> ⚠️ Die Rangliste wird in der `tops.yml` gespeichert und enthält **auch offline Spieler**.

---

## ⚙️ Konfiguration (`config.yml`)

```yaml
# Sprache: "de" (Standard) oder "en"
language: de

# Globaler Multiplikator für Charged Eggs (1.25 = +25%)
charged-multiplier: 1.25

eggs:
  Chicken_Egg:
    display-name: "Chicken Egg"
    icon: CHICKEN_SPAWN_EGG
    base-income: 15.0
    base-upgrade-cost: 5.0
    upgrade-multiplier: 1.5
    sell-multiplier: 4.0
```

### Eigene Eggs hinzufügen

Einfach unter `eggs:` einen neuen Block einfügen:

```yaml
  Diamond_Egg:
    display-name: "Diamond Egg"
    icon: DIAMOND
    base-income: 500.0
    base-upgrade-cost: 500.0
    upgrade-multiplier: 2.0
    sell-multiplier: 4.0
```

Dann `/fancyeggs reload` ausführen – fertig!

### Saisonale Eggs

Ideal für Halloween, Weihnachten, Ostern etc. Einfach in der `config.yml` ergänzen:

```yaml
  Halloween_Egg:
    display-name: "Halloween Egg"
    icon: ZOMBIE_SPAWN_EGG
    base-income: 200.0
    base-upgrade-cost: 75.0
    upgrade-multiplier: 1.7
    sell-multiplier: 4.0
```

### Verfügbare Icon-Materialien

Alle Bukkit-Materialien funktionieren, z.B.:
`CHICKEN_SPAWN_EGG`, `WARDEN_SPAWN_EGG`, `DIAMOND`, `NETHER_STAR`, `DRAGON_EGG`, `HEART_OF_THE_SEA`, `TOTEM_OF_UNDYING`

[Vollständige Material-Liste →](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html)

---

## 📊 Dateien

| Datei | Beschreibung |
|---|---|
| `config.yml` | Alle Egg-Einstellungen, Sprache, Multiplikatoren |
| `tops.yml` | Persistente Rangliste (wird automatisch erstellt) |

> 💡 Die `tops.yml` wird **alle 5 Minuten** und **beim Server-Stop** automatisch gespeichert.

---

## 🛠️ Selbst bauen

### Voraussetzungen

- JDK 25
- Maven 3.8+

### Build-Schritte

```bash
git clone https://github.com/LMCStudio/FancyEggs.git
cd FancyEggs
mvn clean package
```

Die fertige `.jar` liegt anschließend in `target/FancyEggs-<version>.jar`.

### GitHub Actions

Der Build läuft automatisch über GitHub Actions bei jedem Push auf `main` oder `master`. Die fertige JAR findest du im **Actions**-Tab unter **Artifacts**.

---

## 🐛 Bekannte Probleme

| Problem | Lösung |
|---|---|
| `Unsupported class file major version 69` | Maven Shade Plugin auf `3.6.1` oder neuer aktualisieren |
| `invalid target release: 25` | GitHub Workflow auf `java-version: '25'` stellen |
| Placeholder funktionieren nicht | PlaceholderAPI installiert? `/papi reload` ausführen |
| Eggs geben kein Geld | Economy-Plugin mit Dezimalstellen-Unterstützung nutzen |
| Vault nicht gefunden | Reihenfolge beim Start: Vault + Economy vor FancyEggs |

---

## 🤝 Contributing

Pull Requests sind willkommen! Für größere Änderungen bitte zuerst ein Issue eröffnen.

1. Fork erstellen
2. Feature-Branch anlegen (`git checkout -b feature/AmazingFeature`)
3. Änderungen committen (`git commit -m 'Add some AmazingFeature'`)
4. Pushen (`git push origin feature/AmazingFeature`)
5. Pull Request öffnen

---

## 📜 Lizenz

Dieses Projekt steht unter der **MIT-Lizenz**. Siehe [`LICENSE`](LICENSE) für Details.

---

## 👨‍💻 Autor

**LMCStudio**
- GitHub: [@LMCStudio](https://github.com/LMCStudio)

---

## ⭐ Support

Wenn dir das Plugin gefällt, lass gerne einen ⭐ auf GitHub da!

Bei Fragen oder Problemen:
- 🐛 [Issue erstellen](../../issues)
- 💬 [Discussions](../../discussions)

---

**Viel Spaß mit FancyEggs! 🥚💰**
