package com.wormsgroup.worms_re;

import javafx.scene.Node;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Main extends Application {
    private static final int TILE_SIZE = 40;
    private static final int VIEW_WIDTH = 800;
    private static final int VIEW_HEIGHT = 600;
    private static final int NETWORK_PORT = 5000;

    // Scene layer hierarchy
    private Pane appRoot  = new Pane();  // Fixed scene root
    private Pane gameRoot = new Pane();  // Scrolling world content
    private Pane uiRoot   = new Pane();  // HUD overlay (never scrolls)

    // Game state
    private ArrayList<Node>       platforms   = new ArrayList<>();
    private ArrayList<Projectile> projectiles = new ArrayList<>();
    private Player player;
    private int levelWidth;

    // Input state
    private boolean leftPressed  = false;
    private boolean rightPressed = false;

    // HUD
    private Label hudLabel    = new Label();
    private Label netLabel    = new Label(); // Network status line

    // Multiplayer — remote player ghost (visual only, no physics)
    private Rectangle remoteGhost;
    private NetworkManager networkManager;

    // -------------------------------------------------------------------------
    // Level + player setup
    // -------------------------------------------------------------------------
    private void initContent() {

        // Background Image
        Image bgImage = new Image(getClass().getResourceAsStream("background.jpg"));
        ImageView bg = new ImageView(bgImage);
        bg.setFitWidth(VIEW_WIDTH);
        bg.setFitHeight(VIEW_HEIGHT);

        levelWidth = LevelData.LEVEL1[0].length() * TILE_SIZE;
        for (int row = 0; row < LevelData.LEVEL1.length; row++) {
            String line = LevelData.LEVEL1[row];
            for (int col = 0; col < line.length(); col++) {
                switch (line.charAt(col)) {
                    case '1':
                        Rectangle tile = new Rectangle(TILE_SIZE, TILE_SIZE, Color.DARKGREEN);
                        tile.setTranslateX(col * TILE_SIZE);
                        tile.setTranslateY(row * TILE_SIZE);
                        platforms.add(tile);
                        gameRoot.getChildren().add(tile);
                        break;
                    // case '2': enemies, powerups, etc.
                    default:
                        break;
                }
            }
        }

        // Local player
        player = new Player(40, 360, 100, gameRoot, platforms, levelWidth, projectiles);

        // Remote player ghost — red tinted, no collision
        remoteGhost = new Rectangle(20, 20, Color.RED);
        remoteGhost.setOpacity(0.6);
        remoteGhost.setVisible(false); // Hidden until first packet received
        gameRoot.getChildren().add(remoteGhost);

        // Camera follows local player
        player.getEntity().translateXProperty().addListener((obs, oldVal, newVal) -> {
            int offset = newVal.intValue();
            if (offset > VIEW_WIDTH / 2 && offset < levelWidth - VIEW_WIDTH / 2) {
                gameRoot.setLayoutX(-(offset - VIEW_WIDTH / 2));
            }
        });

        // HUD — lives in uiRoot so camera scroll doesn't affect it
        hudLabel.setTranslateX(15);
        hudLabel.setTranslateY(15);
        hudLabel.setStyle("-fx-font-size: 14px; -fx-font-family: monospace; -fx-text-fill: black;");

        netLabel.setTranslateX(15);
        netLabel.setTranslateY(165);
        netLabel.setStyle("-fx-font-size: 12px; -fx-font-family: monospace; -fx-text-fill: darkblue;");
        netLabel.setText("Network: initialising...");

        uiRoot.getChildren().addAll(hudLabel, netLabel);
        appRoot.getChildren().addAll(bg, gameRoot, uiRoot);
    }

    // -------------------------------------------------------------------------
    // Networking
    // -------------------------------------------------------------------------
    private void initNetwork(Stage primaryStage) {
        try {
            networkManager = new NetworkManager(NETWORK_PORT, message -> {
                // Runs on UDP receive thread — dispatch to JavaFX thread before touching scene
                Platform.runLater(() -> handleNetworkMessage(message));
            });
            networkManager.startListening();

            // TODO: replace with peer's actual IP — could be moved to a lobby/config screen
            networkManager.setPeer("127.0.0.1", NETWORK_PORT);

            netLabel.setText("Network: listening on :" + NETWORK_PORT);
        } catch (SocketException | UnknownHostException e) {
            netLabel.setText("Network: failed to bind (" + e.getMessage() + ")");
            e.printStackTrace();
        }

        // Clean shutdown when window closes
        primaryStage.setOnCloseRequest(e -> {
            if (networkManager != null) networkManager.close();
        });
    }

    /**
     * Parses inbound UDP messages.
     * Current protocol: "x,y"  — remote player world-position
     */
    private void handleNetworkMessage(String message) {
        String[] parts = message.split(",");
        if (parts.length == 2) {
            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                remoteGhost.setTranslateX(x);
                remoteGhost.setTranslateY(y);
                remoteGhost.setVisible(true);
            } catch (NumberFormatException ignored) {}
        }
    }

    /**
     * Broadcasts local player position to peer each frame.
     */
    private void broadcastPosition() {
        if (networkManager == null) return;
        double x = player.getEntity().getTranslateX();
        double y = player.getEntity().getTranslateY();
        networkManager.sendData(x + "," + y);
    }

    // -------------------------------------------------------------------------
    // JavaFX entry point
    // -------------------------------------------------------------------------
    @Override
    public void start(Stage primaryStage) {
        initContent();
        initNetwork(primaryStage);

        Weapon bazooka = new Weapon("Bazooka", true, 50);
        Scene scene = new Scene(appRoot, VIEW_WIDTH, VIEW_HEIGHT);

        // Input — pressed
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.A) leftPressed = true;
            if (e.getCode() == KeyCode.D) rightPressed = true;
            if (e.getCode() == KeyCode.W) player.jump();

            // Aim angle
            if (e.getCode() == KeyCode.LEFT)  player.updateAim(-5);
            if (e.getCode() == KeyCode.RIGHT) player.updateAim(5);

            // ENTER — enter aiming mode
            if (e.getCode() == KeyCode.ENTER) player.startAiming(bazooka);

            // SPACE press — begin charge (only activates from AIMING state)
            if (e.getCode() == KeyCode.SPACE) player.startCharging();

            if (e.getCode() == KeyCode.ESCAPE) player.cancelAim();
        });

        // Input — released
        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.A) leftPressed = false;
            if (e.getCode() == KeyCode.D) rightPressed = false;

            if (!leftPressed && !rightPressed) player.stopMovingState();

            // SPACE release — fire at accumulated charge
            if (e.getCode() == KeyCode.SPACE) player.releaseCharge();
        });

        // Game loop
        new AnimationTimer() {
            private long lastFrameTime = System.nanoTime();

            @Override
            public void handle(long currentNanoTime) {
                double deltaTime = (currentNanoTime - lastFrameTime) / 1_000_000_000.0;
                lastFrameTime = currentNanoTime;

                // Movement
                if (leftPressed)  player.moveLeft(deltaTime);
                if (rightPressed) player.moveRight(deltaTime);

                // Player logic
                player.update(deltaTime);

                // Projectiles (iterate backwards for safe removal)
                for (int i = projectiles.size() - 1; i >= 0; i--) {
                    Projectile p = projectiles.get(i);
                    p.update();
                    if (!p.isActive()) projectiles.remove(i);
                }

                // Broadcast local position every frame
                broadcastPosition();

                // HUD
                hudLabel.setText(String.format(
                    "State: %s\nTurn Clock: %.1fs\nMove Fuel: %.1fs\nAim Angle: %.0f°\nCharge: %.0f%%",
                    player.getCurrentState(),
                    player.getTurnTimeRemaining(),
                    player.getMovementTimeRemaining(),
                    player.getAimAngle(),
                    player.getShootPower()
                ));
            }
        }.start();

        primaryStage.setTitle("Worms RE");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}