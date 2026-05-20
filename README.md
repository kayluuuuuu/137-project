# Worms RE

Worms RE is a multiplayer, 2D artillery tactical game inspired by the classic Worms series. Built with Java 21 and JavaFX, it allows two players to battle each other over a network connection in a turn-based format.

## Features
- **Turn-based Multiplayer Strategy:** Take turns to move, aim, and shoot your opponent.
- **Networked P2P Play:** Direct peer-to-peer multiplayer using sockets, complete with a built-in lobby for setup.
- **Physics-based Combat:** Adjust your aim angle and weapon charge to hit the enemy.
- **Dynamic Turn Limits:** Strategic management of turn time and movement fuel.

## Prerequisites
To build and run this project, you need:
- **Java Development Kit (JDK) 21** or higher.
- **Apache Maven** (or you can use the included Maven Wrapper `mvnw`).

## Building the Game
Navigate to the project root directory (where `pom.xml` is located) and compile the project using Maven:

```bash
mvn clean install
```
*(Or use `./mvnw clean install` if you prefer the Maven Wrapper)*

## Running the Game

You can run the game directly using the JavaFX Maven Plugin:

```bash
mvn javafx:run
```

Upon launching, the **Multiplayer Lobby Setup** will appear. 

### How to Connect (2 Players)
Since this is a peer-to-peer multiplayer game, both players need to be running the game and configure their ports.

**If playing locally on the same machine:**
1. **Player 1:** Click the **"Preset: Player 1"** button (Local Port: 5000, Peer IP: 127.0.0.1, Peer Port: 5001). Click **Connect & Launch Game**.
2. **Player 2:** Open a second instance of the game, click the **"Preset: Player 2"** button (Local Port: 5001, Peer IP: 127.0.0.1, Peer Port: 5000). Click **Connect & Launch Game**.

**If playing over a network (different machines):**
1. **Player 1:** 
   - Set Local Port to `5000` (or any available port).
   - Set Peer IP to Player 2's local/public IP Address.
   - Set Peer Port to `5001` (or whatever Player 2 uses for their Local Port).
2. **Player 2:** 
   - Set Local Port to `5001`.
   - Set Peer IP to Player 1's local/public IP Address.
   - Set Peer Port to `5000`.

## Controls
When it is your turn, use the following controls:
- **A / D**: Move Left / Right
- **W**: Jump
- **ENTER**: Select weapon and start aiming
- **Left / Right Arrows**: Adjust aim angle
- **SPACE (Hold & Release)**: Charge weapon power and fire
- **ESCAPE**: Cancel aiming

## Technologies Used
- **Language**: Java 21
- **UI Framework**: JavaFX 21
- **Build Tool**: Maven

## Architecture
- `Main.java`: The game loop, rendering, network synchronization, and entry point.
- `Player.java`: Manages player states, health, turns, and aiming/shooting physics.
- `NetworkManager.java`: Handles UDP/TCP peer-to-peer networking messages.
- `Projectile.java` & `Weapon.java`: Weapon mechanics and projectile physics.
