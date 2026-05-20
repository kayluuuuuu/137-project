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
    private static final int TILE_SIZE   = 40;
    private static final int VIEW_WIDTH  = 800;
    private static final int VIEW_HEIGHT = 600;

    private Pane appRoot  = new Pane();
    private Pane gameRoot = new Pane();
    private Pane uiRoot   = new Pane();

    private ArrayList<Node>       platforms   = new ArrayList<>();
    private ArrayList<Projectile> projectiles = new ArrayList<>();
    private ArrayList<Player>     allPlayers  = new ArrayList<>(); 
    private Player player;
    private int levelWidth;

    private boolean leftPressed  = false;
    private boolean rightPressed = false;

    private static final float TIMESTEP    = 1.0f / 60.0f;
    private float  accumulatedTime         = 0;
    private long   previousTime            = 0;
    private float  secondsSinceLastFpsLog  = 0;
    private int    framesSinceLastFpsLog   = 0;

    private Label hudLabel = new Label();
    private Label netLabel = new Label();
    private Label victoryLabel = new Label(); // Victory overlay banner

    private Rectangle      remoteGhost;
    private Rectangle      remoteHpBarBg;
    private Rectangle      remoteHpBarFill;
    private NetworkManager networkManager;
    private double remoteHp    = 100;
    private double remoteMaxHp = 100;
    private boolean remoteDead = false;
    private boolean gameFinished = false;

    private boolean isMyTurn = false; 

    private void initContent() {
        var stream = getClass().getResourceAsStream("background.jpg");
        if (stream == null) {
            throw new RuntimeException("background.jpg not found — check resources!");
        }
        ImageView bg = new ImageView(new Image(stream));
        bg.setFitWidth(VIEW_WIDTH);
        bg.setFitHeight(VIEW_HEIGHT);

        levelWidth = LevelData.LEVEL1[0].length() * TILE_SIZE;
        for (int row = 0; row < LevelData.LEVEL1.length; row++) {
            String line = LevelData.LEVEL1[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '1') {
                    Rectangle tile = new Rectangle(TILE_SIZE, TILE_SIZE, Color.DARKGREEN);
                    tile.setTranslateX(col * TILE_SIZE);
                    tile.setTranslateY(row * TILE_SIZE);
                    platforms.add(tile);
                    gameRoot.getChildren().add(tile);
                }
            }
        }

        remoteGhost = new Rectangle(20, 20, Color.RED);
        remoteGhost.setOpacity(0.6);
        remoteGhost.setVisible(false);
        gameRoot.getChildren().add(remoteGhost);

        player = new Player(40, 360, 100, gameRoot, uiRoot, platforms, levelWidth,
                            projectiles, allPlayers, remoteGhost);
        allPlayers.add(player);

        player.setHitCallback((hitPlayer, damage) -> {
            if (hitPlayer != null) {
                hitPlayer.takeDamage(damage);
            }
            if (networkManager != null) {
                networkManager.sendData("HIT:" + damage);
            }
        });

        remoteHpBarBg   = new Rectangle(40, 5, Color.DARKGRAY);
        remoteHpBarFill = new Rectangle(40, 5, Color.LIMEGREEN);
        remoteHpBarBg.setVisible(false);
        remoteHpBarFill.setVisible(false);
        uiRoot.getChildren().addAll(remoteHpBarBg, remoteHpBarFill);

        player.getEntity().translateXProperty().addListener((obs, oldVal, newVal) -> {
            if (player.isDead()) return;
            int offset = newVal.intValue();
            if (offset > VIEW_WIDTH / 2 && offset < levelWidth - VIEW_WIDTH / 2) {
                gameRoot.setLayoutX(-(offset - VIEW_WIDTH / 2));
            }
        });

        hudLabel.setTranslateX(15);
        hudLabel.setTranslateY(15);
        hudLabel.setStyle("-fx-font-size: 14px; -fx-font-family: monospace; -fx-text-fill: white;");

        netLabel.setTranslateX(15);
        netLabel.setTranslateY(185);
        netLabel.setStyle("-fx-font-size: 12px; -fx-font-family: monospace; -fx-text-fill: #00ffcc;");
        netLabel.setText("Network: initialising...");

        // Victory banner placement configuration
        victoryLabel.setPrefWidth(VIEW_WIDTH);
        victoryLabel.setTranslateY(VIEW_HEIGHT / 2.0 - 40);
        victoryLabel.setAlignment(javafx.geometry.Pos.CENTER);
        victoryLabel.setVisible(false);

        uiRoot.getChildren().addAll(hudLabel, netLabel, victoryLabel);
        appRoot.getChildren().addAll(bg, gameRoot, uiRoot);
    }

    private void updateRemoteHpBar() {
        if (!remoteGhost.isVisible() || remoteDead) {
            remoteHpBarBg.setVisible(false);
            remoteHpBarFill.setVisible(false);
            return;
        }
        double screenX = remoteGhost.getTranslateX() + gameRoot.getLayoutX();
        double screenY = remoteGhost.getTranslateY() - 10;
        double ratio   = remoteHp / remoteMaxHp;

        remoteHpBarBg.setTranslateX(screenX);
        remoteHpBarBg.setTranslateY(screenY);
        remoteHpBarBg.setVisible(true);

        remoteHpBarFill.setWidth(40 * ratio);
        remoteHpBarFill.setTranslateX(screenX);
        remoteHpBarFill.setTranslateY(screenY);
        remoteHpBarFill.setVisible(true);

        if (ratio > 0.5)       remoteHpBarFill.setFill(Color.LIMEGREEN);
        else if (ratio > 0.25) remoteHpBarFill.setFill(Color.YELLOW);
        else                   remoteHpBarFill.setFill(Color.RED);
    }

    private void initNetwork(Stage primaryStage, int localPort, String peerIp, int peerPort) {
        try {
            networkManager = new NetworkManager(localPort, message ->
                Platform.runLater(() -> handleNetworkMessage(message))
            );
            networkManager.startListening();
            networkManager.setPeer(peerIp, peerPort);
            netLabel.setText("Network: :" + localPort + " → " + peerIp + ":" + peerPort);
        } catch (SocketException | UnknownHostException e) {
            netLabel.setText("Network: failed — " + e.getMessage());
            e.printStackTrace();
        }

        primaryStage.setOnCloseRequest(e -> {
            if (networkManager != null) networkManager.close();
        });
    }

    private void handleNetworkMessage(String message) {
        if (gameFinished) return;

        if (message.equals("PASS_TURN")) {
            if (!player.isDead()) {
                isMyTurn = true;
                player.resetTurnLimits();
            }
            return;
        }

        if (message.startsWith("HIT:")) {
            try {
                int dmg = Integer.parseInt(message.substring(4));
                player.takeDamage(dmg);
            } catch (NumberFormatException ignored) {}
            return;
        }

        String[] parts = message.split(",");
        if (parts.length >= 3) {
            try {
                double rx = Double.parseDouble(parts[0]);
                double ry = Double.parseDouble(parts[1]);
                remoteHp = Double.parseDouble(parts[2]);

                if (remoteHp <= 0 && !remoteDead) {
                    remoteDead = true;
                    remoteGhost.setVisible(false);
                } else if (!remoteDead) {
                    remoteGhost.setTranslateX(rx);
                    remoteGhost.setTranslateY(ry);
                    remoteGhost.setVisible(true);
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private void broadcastState() {
        if (networkManager == null) return;
        networkManager.sendData(
            player.getEntity().getTranslateX() + "," +
            player.getEntity().getTranslateY() + "," +
            player.getHp()
        );
    }

    private void passTurnToPeer() {
        isMyTurn = false;
        leftPressed = false;
        rightPressed = false;
        player.endTurn();
        if (networkManager != null) {
            networkManager.sendData("PASS_TURN");
        }
    }

    private void checkWinConditions() {
        if (gameFinished) return;

        // Condition A: Opponent died, you are still standing
        if (remoteDead && !player.isDead()) {
            gameFinished = true;
            isMyTurn = false;
            victoryLabel.setText("VICTORY! YOU WIN!");
            victoryLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #00ffcc; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 20;");
            victoryLabel.setVisible(true);
        }
        // Condition B: You died
        else if (player.isDead()) {
            gameFinished = true;
            isMyTurn = false;
            victoryLabel.setText("DEFEAT! YOU DIED!");
            victoryLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #ff4a5a; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 20;");
            victoryLabel.setVisible(true);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        var rawArgs = getParameters().getRaw();
        if (rawArgs.size() >= 3) {
            try {
                startGame(primaryStage,
                    Integer.parseInt(rawArgs.get(0)),
                    rawArgs.get(1),
                    Integer.parseInt(rawArgs.get(2)));
                return;
            } catch (NumberFormatException e) {
                System.out.println("Invalid CLI args, showing launcher.");
            }
        }

        Stage launcher = new Stage();
        launcher.setTitle("WORMS RE — Multiplayer Launcher");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setAlignment(javafx.geometry.Pos.CENTER);
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(25));
        grid.setStyle("-fx-background-color: #1a1a24;");

        javafx.scene.text.Text title = new javafx.scene.text.Text("WORMS RE");
        title.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 26));
        title.setFill(Color.web("#ff4a5a"));
        grid.add(title, 0, 0, 2, 1);

        javafx.scene.text.Text subtitle = new javafx.scene.text.Text("Multiplayer Lobby Setup");
        subtitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.NORMAL, 14));
        subtitle.setFill(Color.web("#8b8ba8"));
        grid.add(subtitle, 0, 1, 2, 1);

        javafx.scene.control.TextField localPortField = styledField("5000");
        javafx.scene.control.TextField peerIpField    = styledField("127.0.0.1");
        javafx.scene.control.TextField peerPortField  = styledField("5000");

        grid.add(styledLabel("Your Port (Listen):"), 0, 2); grid.add(localPortField, 1, 2);
        grid.add(styledLabel("Peer IP Address:"),    0, 3); grid.add(peerIpField,    1, 3);
        grid.add(styledLabel("Peer Port:"),          0, 4); grid.add(peerPortField,  1, 4);

        javafx.scene.layout.HBox presets = new javafx.scene.layout.HBox(10);
        presets.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.control.Button p1Btn = presetButton("Preset: Player 1");
        p1Btn.setOnAction(e -> { localPortField.setText("5000"); peerIpField.setText("127.0.0.1"); peerPortField.setText("5001"); });
        javafx.scene.control.Button p2Btn = presetButton("Preset: Player 2");
        p2Btn.setOnAction(e -> { localPortField.setText("5001"); peerIpField.setText("127.0.0.1"); peerPortField.setText("5000"); });
        presets.getChildren().addAll(p1Btn, p2Btn);
        grid.add(presets, 0, 5, 2, 1);

        javafx.scene.control.Button launchBtn = new javafx.scene.control.Button("Connect & Launch Game");
        launchBtn.setPrefWidth(350);
        String ls = "-fx-background-color: #ff4a5a; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 8 16;";
        launchBtn.setStyle(ls);
        launchBtn.setOnAction(e -> {
            try {
                int lp    = Integer.parseInt(localPortField.getText().trim());
                String ip = peerIpField.getText().trim();
                int pp    = Integer.parseInt(peerPortField.getText().trim());
                launcher.close();
                startGame(primaryStage, lp, ip, pp);
            } catch (NumberFormatException ex) {
                subtitle.setText("Error: Ports must be integers!");
                subtitle.setFill(Color.YELLOW);
            }
        });
        grid.add(launchBtn, 0, 6, 2, 1);

        launcher.setScene(new Scene(grid, 420, 360));
        launcher.setResizable(false);
        launcher.show();
    }

    private void startGame(Stage primaryStage, int localPort, String peerIp, int peerPort) {
        if (localPort == 5000) {
            isMyTurn = true;
        } else {
            isMyTurn = false;
        }

        initContent();
        initNetwork(primaryStage, localPort, peerIp, peerPort);

        Weapon bazooka = new Weapon("Bazooka", true, 50);
        Scene scene = new Scene(appRoot, VIEW_WIDTH, VIEW_HEIGHT);

        scene.setOnKeyPressed(e -> {
            if (!isMyTurn || gameFinished) return; 

            if (e.getCode() == KeyCode.A)      leftPressed = true;
            if (e.getCode() == KeyCode.D)      rightPressed = true;
            if (e.getCode() == KeyCode.W)      player.jump();
            if (e.getCode() == KeyCode.LEFT)   player.updateAim(-5);
            if (e.getCode() == KeyCode.RIGHT)  player.updateAim(5);
            if (e.getCode() == KeyCode.ENTER)  player.startAiming(bazooka);
            if (e.getCode() == KeyCode.SPACE)  player.startCharging();
            if (e.getCode() == KeyCode.ESCAPE) player.cancelAim();
        });

        scene.setOnKeyReleased(e -> {
            if (!isMyTurn || gameFinished) return; 

            if (e.getCode() == KeyCode.A) leftPressed  = false;
            if (e.getCode() == KeyCode.D) rightPressed = false;
            if (!leftPressed && !rightPressed) player.stopMovingState();
            if (e.getCode() == KeyCode.SPACE) player.releaseCharge();
        });

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (previousTime == 0) {
                    previousTime = now;
                    return;
                }

                float secondsElapsed = (now - previousTime) / 1e9f;
                accumulatedTime += secondsElapsed;
                previousTime = now;

                while (accumulatedTime >= TIMESTEP) {
                    tick(TIMESTEP);
                    accumulatedTime -= TIMESTEP;
                }

                player.updateHpBar(gameRoot.getLayoutX());
                updateRemoteHpBar();
                checkWinConditions();
                
                String turnStatus = isMyTurn ? "YOUR TURN" : "WAITING FOR OPPONENT";
                if (gameFinished) turnStatus = "GAME OVER";

                hudLabel.setText(String.format(
                    "[%s]\nHP: %d\nState: %s\nTurn Clock: %.1fs\nMove Fuel: %.1fs\nAim Angle: %.0f°\nCharge: %.0f%%",
                    turnStatus,
                    player.getHp(),
                    player.getCurrentState(),
                    player.getTurnTimeRemaining(),
                    player.getMovementTimeRemaining(),
                    player.getAimAngle(),
                    player.getShootPower()
                ));

                secondsSinceLastFpsLog += secondsElapsed;
                framesSinceLastFpsLog++;
                if (secondsSinceLastFpsLog >= 0.5f) {
                    secondsSinceLastFpsLog = 0;
                    framesSinceLastFpsLog  = 0;
                }
            }
        }.start();

        primaryStage.setTitle("Worms RE  [:" + localPort + " \u2192 " + peerIp + ":" + peerPort + "]");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void tick(float dt) {
        if (gameFinished) return;

        if (isMyTurn) {
            if (leftPressed)  player.moveLeft(dt);
            if (rightPressed) player.moveRight(dt);
        }

        player.update(dt);

        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update();
            if (!p.isActive()) {
                projectiles.remove(i);
                
                if (isMyTurn && p.getShooter() == player) {
                    passTurnToPeer();
                }
            }
        }

        broadcastState();
    }

    private javafx.scene.control.Label styledLabel(String text) {
        var lbl = new javafx.scene.control.Label(text);
        lbl.setTextFill(Color.web("#b4b4d0"));
        return lbl;
    }

    private javafx.scene.control.TextField styledField(String defaultVal) {
        var tf = new javafx.scene.control.TextField(defaultVal);
        tf.setStyle("-fx-background-color: #2b2b3d; -fx-text-fill: white; -fx-border-color: #3b3b4f; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13;");
        tf.setPrefWidth(200);
        return tf;
    }

    private javafx.scene.control.Button presetButton(String text) {
        var btn = new javafx.scene.control.Button(text);
        btn.setStyle("-fx-background-color: #3b3b4f; -fx-text-fill: #00ffcc; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;");
        return btn;
    }

    public static void main(String[] args) {
        launch(args);
    }
}