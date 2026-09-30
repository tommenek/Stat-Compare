# Stat Compare (Fabric, Minecraft 26.2)

A tooltip-only mod: no new blocks or items. While an inventory-style screen is open (your
inventory, a chest, this or any container), hovering an item adds a coloured comparison to
its tooltip:

- **Armour** (helmet, chestplate, leggings, boots) compares against whatever you have
  equipped in that slot.
- **Tools** — including weapons like swords, axes and tridents, detected by having
  attack/mining stats — compare against whatever is in your main hand.

Press **H** (rebindable under Options > Controls > Key Binds > Stat Compare) to show or
hide the comparison at any time.

Each line is:
- **green** if the hovered item is better
- **red** if it's worse
- **grey** if it's the same

Stats compared: every flat attribute the item carries (attack damage, attack speed, armor,
armor toughness, knockback resistance, movement speed, etc. — whatever the item has),
durability, mining speed for tools, and the level of every enchantment actually applied to
the item (not a book's stored enchantments). All are treated as "higher is better", which
covers every vanilla stat and enchantment level in this list.

Java 25, Fabric Loader 0.19.3+, Fabric API for 26.2.

## Build
Easiest: generate a template at https://fabricmc.net/develop/template (Minecraft 26.2, mod id
`statcompare`) to get the Gradle wrapper, then copy in `src/`, `build.gradle`,
`gradle.properties`, `settings.gradle`.
Or with Gradle 9.5.1 installed: `gradle wrapper --gradle-version 9.5.1` then `./gradlew build`.
The jar lands in `build/libs/`.

## Build the jar without installing anything (GitHub Actions)
1. Create a **new** GitHub repository (a separate one from Enchant Library is easiest).
2. On the repository page, click **uploading an existing file**, then drag in the *contents*
   of this folder — `settings.gradle`, `build.gradle`, `gradle.properties`, `src`, `README.md` —
   not the `stat-compare` folder itself. Commit.
3. Click **Add file → Create new file**, name it exactly `.github/workflows/build.yml`
   (the slashes create the folders), paste in the contents of `.github/workflows/build.yml`
   from this project, and commit.
4. Open the **Actions** tab and wait for the green check.
5. Download the `stat-compare-jar` artifact, unzip it, and put `stat-compare-1.0.0.jar`
   (not the `-sources` jar) in your `mods` folder next to Fabric API.

## Notes / limitations
- This only works while a container-style screen is open (inventory, chest, etc.), so it
  won't clutter normal hotbar tooltips during gameplay.
- Bows and crossbows have no melee attributes, so they won't show a comparison.
- Percentage-based attribute modifiers (rare, mostly from other mods) are ignored; only
  flat modifiers are summed, since they're the ones that add up into one meaningful number.
