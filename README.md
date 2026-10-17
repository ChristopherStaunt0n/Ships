# Ships
A top-down Java Swing ship game

Requirements:
Java 17+
Maven

```text
mvn package
java -jar target/ships-game-1.0.0.jar
```

The application starts at a menu for a new game, saved games, or the ship-preset editor. Before a voyage, configure the ship's turn speed in degrees per frame; it controls how quickly both player and enemy ships rotate toward their target direction. You can also configure collision slowdown strength and duration. Ship-to-ship impacts damage both ships and temporarily reduce their speed; this reduction fades to zero over the configured duration. Ships bounce off each other and the screen edges, while walls cause no damage; enemy steering normally tries to avoid nearby ships. Move the player ship with the mouse; hold the left mouse button to turn the bow and redirect the ship's velocity toward the cursor. When not steering, the bow turns to follow the ship's current velocity; release the button to coast without mouse movement changing velocity. Toggle the frames-per-second counter in the voyage configuration or pause menu; it displays the number of game frames from the most recent one-second interval. Cannonballs leave fading smoke trails and ships leave fading wakes; trails are visual effects and do not cause damage. The animated ocean uses flowing blue-green color gradients, with lighter shallow-looking water at the top and bottom boundaries and darker deep-looking water through the middle. A translucent sky tint cycles through day and night every five minutes. These background effects are rendering-only and do not affect mouse or keyboard controls. Press **W** to fire two forward cannons, **S** for three rear cannons, **A** for five port-side cannons, and **D** for five starboard-side cannons. Side batteries cool down independently for 5 seconds; front and rear batteries cool down independently for 10 seconds. Press **Escape** to pause.

Ship presets and saves are JSON text files in the `data/presets.json` and `data/saves/` folders under the application's working directory. Presets carry stable unique IDs and define shape, size, and color for full, half, and sunken durability appearances. Enemy behavior is selected through the `EnemyBehavior` interface and can be switched at runtime as an enemy's situation changes.
