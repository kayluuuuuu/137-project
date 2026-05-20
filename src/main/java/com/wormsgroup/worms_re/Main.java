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
        // Sensible network defaults
        int[] localPort = {5000};
        String[] peerIp = {"127.0.0.1"};
        int[] peerPort = {5000};

        // 1. Check for command-line arguments to support automated non-interactive launches
        java.util.List<String> rawArgs = getParameters().getRaw();
        boolean hasArgs = false;
        if (rawArgs.size() >= 3) {
            try {
                localPort[0] = Integer.parseInt(rawArgs.get(0));
                peerIp[0] = rawArgs.get(1);
                peerPort[0] = Integer.parseInt(rawArgs.get(2));
                hasArgs = true;
            } catch (NumberFormatException e) {
                System.out.println("Invalid CLI args. Falling back to launcher UI.");
            }
        }

        if (hasArgs) {
            startGame(mainStage, localPort[0], peerIp[0], peerPort[0]);
        } else {
            // 2. Display a beautiful, styled setup launcher stage
            Stage launcherStage = new Stage();
            launcherStage.setTitle("WORMS 137 - Multiplayer Launcher");

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setAlignment(javafx.geometry.Pos.CENTER);
            grid.setHgap(15);
            grid.setVgap(15);
            grid.setPadding(new javafx.geometry.Insets(25, 25, 25, 25));
            grid.setStyle("-fx-background-color: #1a1a24;");

            // Header Title
            javafx.scene.text.Text title = new javafx.scene.text.Text("WORMS 137");
            title.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 26));
            title.setFill(Color.web("#ff4a5a"));
            grid.add(title, 0, 0, 2, 1);

            javafx.scene.text.Text subtitle = new javafx.scene.text.Text("Multiplayer Lobby Setup");
            subtitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.NORMAL, 14));
            subtitle.setFill(Color.web("#8b8ba8"));
            grid.add(subtitle, 0, 1, 2, 1);

            // Local listening port
            javafx.scene.control.Label localPortLabel = new javafx.scene.control.Label("Your Port (Listen):");
            localPortLabel.setTextFill(Color.web("#b4b4d0"));
            grid.add(localPortLabel, 0, 2);

            javafx.scene.control.TextField localPortField = new javafx.scene.control.TextField("5000");
            localPortField.setStyle("-fx-background-color: #2b2b3d; -fx-text-fill: white; -fx-border-color: #3b3b4f; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13;");
            localPortField.setPrefWidth(200);
            grid.add(localPortField, 1, 2);

            // Peer IP address
            javafx.scene.control.Label peerIpLabel = new javafx.scene.control.Label("Peer IP Address:");
            peerIpLabel.setTextFill(Color.web("#b4b4d0"));
            grid.add(peerIpLabel, 0, 3);

            javafx.scene.control.TextField peerIpField = new javafx.scene.control.TextField("127.0.0.1");
            peerIpField.setStyle("-fx-background-color: #2b2b3d; -fx-text-fill: white; -fx-border-color: #3b3b4f; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13;");
            grid.add(peerIpField, 1, 3);

            // Peer Port
            javafx.scene.control.Label peerPortLabel = new javafx.scene.control.Label("Peer Port:");
            peerPortLabel.setTextFill(Color.web("#b4b4d0"));
            grid.add(peerPortLabel, 0, 4);

            javafx.scene.control.TextField peerPortField = new javafx.scene.control.TextField("5000");
            peerPortField.setStyle("-fx-background-color: #2b2b3d; -fx-text-fill: white; -fx-border-color: #3b3b4f; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13;");
            grid.add(peerPortField, 1, 4);

            // Local machine presets
            javafx.scene.layout.HBox presets = new javafx.scene.layout.HBox(10);
            presets.setAlignment(javafx.geometry.Pos.CENTER);
            
            javafx.scene.control.Button p1Btn = new javafx.scene.control.Button("Preset: Player 1");
            p1Btn.setStyle("-fx-background-color: #3b3b4f; -fx-text-fill: #00ffcc; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;");
            p1Btn.setOnAction(e -> {
                localPortField.setText("5000");
                peerIpField.setText("127.0.0.1");
                peerPortField.setText("5001");
            });

            javafx.scene.control.Button p2Btn = new javafx.scene.control.Button("Preset: Player 2");
            p2Btn.setStyle("-fx-background-color: #3b3b4f; -fx-text-fill: #00ffcc; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;");
            p2Btn.setOnAction(e -> {
                localPortField.setText("5001");
                peerIpField.setText("127.0.0.1");
                peerPortField.setText("5000");
            });

            presets.getChildren().addAll(p1Btn, p2Btn);
            grid.add(presets, 0, 5, 2, 1);

            // Connect & Launch Button
            javafx.scene.control.Button launchBtn = new javafx.scene.control.Button("Connect & Launch Game");
            launchBtn.setPrefWidth(350);
            launchBtn.setStyle("-fx-background-color: #ff4a5a; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 8 16;");
            
            // Hover styling
            launchBtn.setOnMouseEntered(e -> launchBtn.setStyle("-fx-background-color: #ff606e; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 8 16;"));
            launchBtn.setOnMouseExited(e -> launchBtn.setStyle("-fx-background-color: #ff4a5a; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 8 16;"));
            
            launchBtn.setOnAction(e -> {
                try {
                    int myLp = Integer.parseInt(localPortField.getText().trim());
                    String peerIpStr = peerIpField.getText().trim();
                    int peerLp = Integer.parseInt(peerPortField.getText().trim());
                    launcherStage.close();
                    startGame(mainStage, myLp, peerIpStr, peerLp);
                } catch (NumberFormatException ex) {
                    subtitle.setText("Error: Ports must be integers!");
                    subtitle.setFill(Color.YELLOW);
                }
            });
            grid.add(launchBtn, 0, 6, 2, 1);

            Scene scene = new Scene(grid, 420, 360);
            launcherStage.setScene(scene);
            launcherStage.setResizable(false);
            launcherStage.show();
        }
    }

    private void startGame(Stage mainStage, int localPort, String peerIp, int peerPort) {
        initContent();

        // Initialize Network
        try {
            networkManager = new NetworkManager(localPort, message -> {
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
            // Connect to peer dynamically configured
            networkManager.setPeer(peerIp, peerPort);
            System.out.println("Network Initialized Successfully!");
            System.out.println("Listening on Port: " + localPort);
            System.out.println("Connected to Peer: " + peerIp + ":" + peerPort);
        } catch (Exception e) {
            System.err.println("Failed to start networking: " + e.getMessage());
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
        mainStage.setTitle("WORMS 137 [Port " + localPort + " -> Peer " + peerIp + ":" + peerPort + "]");
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
                    System.out.println("FPS: " + fps);
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
