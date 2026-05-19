package com.wormsgroup.worms_re;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;

public class Main extends Application {
    // Enum for keys
    private HashMap<KeyCode, Boolean> keys = new HashMap<KeyCode, Boolean>();

    private ArrayList<Node> platforms = new ArrayList<Node>();

    private Pane appRoot = new Pane();
    private Pane gameRoot = new Pane();
    private Pane uiRoot = new Pane();

    private Node player;
    private Node enemyPlayer;
    private NetworkManager networkManager;
    private double lastSentX = -1;
    private double lastSentY = -1;
    private Point2D playerVelocity = new Point2D(0, 0);
    private boolean canJump = true;

    private static final float timeStep = 0.01666666f;
    private float accumulatedTime = 0;
    private long previousTime = 0;
    private float secondsElapsedSinceLastFpsUpdate = 0f;
    private int framesSinceLastFpsUpdate = 0;

    private int levelWidth;

    // Initialize UI
    private void initContent() {
        Rectangle bg = new Rectangle(800, 600);

        levelWidth = LevelData.LEVEL1[0].length() * 40;

        for (int i = 0; i < LevelData.LEVEL1.length; i++) {
            String line = LevelData.LEVEL1[i];
            for (int j = 0; j < line.length(); j++) {
                switch (line.charAt(j)) {
                    case '0':
                        break;
                    case '1':
                        Node platform = createEntity(j*40, i*40, 40, 40, Color.RED);
                        platforms.add(platform);
                    // TODO: add case for enemy char
                }
            }
        }

        player = createEntity(0,200, 20, 20, Color.BLUE);
        enemyPlayer = createEntity(0,200, 20, 20, Color.GREEN);
        player.translateXProperty().addListener((obs, old, newVal) -> {
            int offset = newVal.intValue();

            if (offset > 400 && offset < levelWidth - 400) {
                gameRoot.setLayoutX(-(offset - 400));
            }
        });

        appRoot.getChildren().addAll(bg, gameRoot, uiRoot);
    }

    // Game Loop
    private void update() {
        // Movement
        if (isPressed(KeyCode.W) && player.getTranslateY() >= 5) {
            jumpPlayer();
        }
        if (isPressed(KeyCode.A) && player.getTranslateX() >= 5) {
            movePlayerX(-5);
        }
        if (isPressed(KeyCode.D) && player.getTranslateX() + 20 <= levelWidth - 5) {
            movePlayerX(5);
        }

        if (playerVelocity.getY() < 10) {
            playerVelocity = playerVelocity.add(0, 1);
        }
        // Get current Y
        double oldY = player.getTranslateY();
        movePlayerY((int) playerVelocity.getY());

        // Check if there was a change in Y without using the jump button to prevent midair jump while falling
        if (oldY != player.getTranslateY()) {
            canJump = false;
        }

        // Send network data if position changed
        if (networkManager != null) {
            double currentX = player.getTranslateX();
            double currentY = player.getTranslateY();
            if (currentX != lastSentX || currentY != lastSentY) {
                networkManager.sendData(currentX + "," + currentY);
                lastSentX = currentX;
                lastSentY = currentY;
            }
        }
    }

    // Initialize Entities
    // TODO: Encapsulate better as player and platforms are of the same class, only differentiated because the platform entities are in an array
    //  - Also would allow for use of sprites e.g. with ImageView (Subclass of Node).
    private Node createEntity(int x, int y, int width, int height, Color color) {
        Rectangle entity = new Rectangle(width, height);
        entity.setTranslateX(x);
        entity.setTranslateY(y);
        entity.setFill(color);

        gameRoot.getChildren().add(entity);
        return entity;
    }

    private boolean isPressed(KeyCode key) {
        return keys.getOrDefault(key, false);
    }

    // Prevents multiple jumps
    private void jumpPlayer() {
        if (canJump) {
            playerVelocity = playerVelocity.add(0, -30);
            canJump = false;
        }
    }

    // Function to move player entity along X-Axis
    private void movePlayerX(int val) {
        boolean movingRight = val > 0;

        for (int i = 0; i < Math.abs(val); i++) {
            for (Node platform : platforms) {
                if (player.getBoundsInParent().intersects(platform.getBoundsInParent())) {
                    if (movingRight) {
                        if (player.getTranslateX() + 20 == platform.getTranslateX()) {
                            return;
                        }
                    } else {
                        if (player.getTranslateX() == platform.getTranslateX() + 40) {
                            return;
                        }
                    }
                }
            }
            player.setTranslateX(player.getTranslateX() + (movingRight ? 1 : -1));
        }
    }

    // Function to move player entity along Y-Axis
    private void movePlayerY(int val) {
        boolean movingDown = val > 0;

        for (int i = 0; i < Math.abs(val); i++) {
            for (Node platform : platforms) {
                if (player.getBoundsInParent().intersects(platform.getBoundsInParent())) {
                    if (movingDown) {
                        if (player.getTranslateY() + 20 == platform.getTranslateY()) {
                            player.setTranslateY(player.getTranslateY() - 1);
                            canJump = true;
                            return;
                        }
                    } else {
                        if (player.getTranslateY() == platform.getTranslateY() + 40) {
                            return;
                        }
                    }
                }
            }
            player.setTranslateY(player.getTranslateY() + (movingDown ? 1 : -1));
        }
    }

    @Override
    public void start(Stage mainStage) throws Exception {
        initContent();

        // Initialize Network
        try {
            networkManager = new NetworkManager(5000, message -> {
                javafx.application.Platform.runLater(() -> {
                    String[] parts = message.split(",");
                    if (parts.length == 2) {
                        try {
                            double x = Double.parseDouble(parts[0]);
                            double y = Double.parseDouble(parts[1]);
                            enemyPlayer.setTranslateX(x);
                            enemyPlayer.setTranslateY(y);
                        } catch (NumberFormatException ignored) {}
                    }
                });
            });
            networkManager.startListening();
            // Connect to your friend's IP
            networkManager.setPeer("160.20.41.35", 5000); 
        } catch (Exception e) {
            e.printStackTrace();
        }

        mainStage.setOnCloseRequest(e -> {
            if (networkManager != null) {
                networkManager.close();
            }
        });

        // Set GameScene
        Scene scene = new Scene(appRoot);
        scene.setOnKeyPressed(event -> keys.put(event.getCode(), true));
        scene.setOnKeyReleased(event -> keys.put(event.getCode(), false));
        mainStage.setTitle("WORMS 137");
        mainStage.setScene(scene);
        mainStage.show();

        // Timer and main game loop
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Additional code is for 60 ticks of game loop. Modify update() for game stuff.
                if (previousTime == 0) {
                    previousTime = now;
                    return;
                }

                float secondsElapsed = (now - previousTime) / 1e9f;
                float secondsElapsedCapped = Math.min(secondsElapsed, Float.MAX_VALUE);
                accumulatedTime += secondsElapsedCapped;
                previousTime = now;

                while (accumulatedTime >= timeStep) {
                    update();
                    accumulatedTime -= timeStep;
                }
                // Add render call here if using GraphicsContext

                secondsElapsedSinceLastFpsUpdate += secondsElapsed;
                framesSinceLastFpsUpdate++;
                if (secondsElapsedSinceLastFpsUpdate >= 0.5f) {
                    int fps = Math.round(framesSinceLastFpsUpdate / secondsElapsedSinceLastFpsUpdate);
                    System.out.println(fps);
                    secondsElapsedSinceLastFpsUpdate = 0;
                    framesSinceLastFpsUpdate = 0;
                }
            }
        };

        timer.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
