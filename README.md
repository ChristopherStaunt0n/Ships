# Ships
A top-down Java Swing ship game

Requirements:
Java 17+
Maven

```text
mvn package
java -jar target/ships-game-1.0.0.jar
```

The application starts at a menu for a new game, saved games, or the ship-preset editor. Before a voyage, configure the ship's turn speed in degrees per frame; it controls how quickly both player and enemy ships rotate toward their target direction. Ships bounce off each other and the screen edges. Ship-to-ship impacts cause minor damage to both ships, while walls cause no damage; enemy steering normally tries to avoid nearby ships. Move the player ship by moving the mouse. Press **W** to fire two forward cannons, **S** for three rear cannons, **A** for five port-side cannons, and **D** for five starboard-side cannons. Side batteries cool down independently for 5 seconds; front and rear batteries cool down independently for 10 seconds. Press **Escape** to pause.

Ship presets and saves are JSON text files in the `data/presets.json` and `data/saves/` folders under the application's working directory. Presets carry stable unique IDs and define shape, size, and color for full, half, and sunken durability appearances. Enemy behavior is selected through the `EnemyBehavior` interface and can be switched at runtime as an enemy's situation changes.
