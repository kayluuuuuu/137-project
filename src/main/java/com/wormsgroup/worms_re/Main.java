package com.wormsgroup.worms_re;
import javafx.scene.Node;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.util.ArrayList;

public class Main extends Application {
    private static final int TILE_SIZE = 40;
    private static final int VIEW_WIDTH = 800;
    private static final int VIEW_HEIGHT = 600;

    private Pane appRoot = new Pane();   // Root of the scene (fixed, full scene size)
    private Pane gameRoot = new Pane();  // Scrolling world content
    private Pane uiRoot = new Pane();    // HUD overlay (never scrolls)

    private ArrayList<Node> platforms = new ArrayList<>();
    private ArrayList<Projectile> projectiles = new ArrayList<>();
    private Player player;
    private int levelWidth;

    // Input mapping states
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    // HUD Elements
    private Label hudLabel = new Label();

    private void initContent() {
        // Background fill
        Rectangle bg = new Rectangle(VIEW_WIDTH, VIEW_HEIGHT);
        bg.setFill(Color.LIGHTCYAN);

        // Parse LevelData tile map
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
                    // case '2': enemies, powerups, etc. go here
                    default:
                        break;
                }
            }
        }

        // Initialize Player — spawn at tile (1, 9) so it lands on first available floor
        player = new Player(40, 360, 100, gameRoot, platforms, levelWidth, projectiles);

        // Camera: scroll gameRoot so the player stays centred horizontally
        player.getEntity().translateXProperty().addListener((obs, oldVal, newVal) -> {
            int offset = newVal.intValue();
            if (offset > VIEW_WIDTH / 2 && offset < levelWidth - VIEW_WIDTH / 2) {
                gameRoot.setLayoutX(-(offset - VIEW_WIDTH / 2));
            }
        });

        // HUD lives in uiRoot so it is unaffected by camera scroll
        hudLabel.setTranslateX(15);
        hudLabel.setTranslateY(15);
        hudLabel.setStyle("-fx-font-size: 14px; -fx-font-family: monospace; -fx-text-fill: black;");
        uiRoot.getChildren().add(hudLabel);

        appRoot.getChildren().addAll(bg, gameRoot, uiRoot);
    }

    @Override
    public void start(Stage primaryStage) {
        initContent();

        Weapon bazooka = new Weapon("Bazooka", true, 50);

        Scene scene = new Scene(appRoot, VIEW_WIDTH, VIEW_HEIGHT);

        // Register Input Event Handlers
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.A) leftPressed = true;
            if (e.getCode() == KeyCode.D) rightPressed = true;
            if (e.getCode() == KeyCode.W) player.jump();

            // Aim angle control
            if (e.getCode() == KeyCode.LEFT)  player.updateAim(-5);
            if (e.getCode() == KeyCode.RIGHT) player.updateAim(5);

            // ENTER — enter aiming mode
            if (e.getCode() == KeyCode.ENTER) player.startAiming(bazooka);

            // SPACE pressed — begin charging power (only valid while AIMING)
            if (e.getCode() == KeyCode.SPACE) player.startCharging();

            if (e.getCode() == KeyCode.ESCAPE) player.cancelAim();
        });

        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.A) leftPressed = false;
            if (e.getCode() == KeyCode.D) rightPressed = false;

            if (!leftPressed && !rightPressed) {
                player.stopMovingState();
            }

            // SPACE released — fire at current charge level
            if (e.getCode() == KeyCode.SPACE) player.releaseCharge();
        });

        // Game Loop Engine Execution Loop
        final long startNanoTime = System.nanoTime();
        new AnimationTimer() {
            private long lastFrameTime = System.nanoTime();

            @Override
            public void handle(long currentNanoTime) {
                double deltaTime = (currentNanoTime - lastFrameTime) / 1_000_000_000.0;
                lastFrameTime = currentNanoTime;

                // Process continuous lateral translation updates
                if (leftPressed) player.moveLeft(deltaTime);
                if (rightPressed) player.moveRight(deltaTime);

                // Run State Upkeep
                player.update(deltaTime);

                // Update active projectile array list loops backward to safely clear dropped entities
                for (int i = projectiles.size() - 1; i >= 0; i--) {
                    Projectile p = projectiles.get(i);
                    p.update();
                    if (!p.isActive()) {
                        projectiles.remove(i);
                    }
                }

                // Render dynamic HUD telemetry output
                hudLabel.setText(String.format(
                    "State: %s\nTurn Clock: %.1fs\nMove Fuel Remaining: %.1fs\nAim Hemisphere Angle: %.0f°\nCharge Output: %.0f",
                    player.getCurrentState(),
                    player.getTurnTimeRemaining(),
                    player.getMovementTimeRemaining(),
                    player.getAimAngle(),
                    player.getShootPower()
                ));
            }
        }.start();

        primaryStage.setTitle("Worms Engine Test Environment");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}