package com.wormsgroup.worms_re;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
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

    private Player player;

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

        // Create player instance
        player = new Player(0, 200, gameRoot, platforms, levelWidth);
        
        // Add camera follow for player
        player.getEntity().translateXProperty().addListener((obs, old, newVal) -> {
            int offset = newVal.intValue();

            if (offset > 400 && offset < levelWidth - 400) {
                gameRoot.setLayoutX(-(offset - 400));
            }
        });

        appRoot.getChildren().addAll(bg, gameRoot, uiRoot);
    }

    // Game Loop
    private void update() {
        // Handle input
        if (isPressed(KeyCode.W)) {
            player.jump();
        }
        if (isPressed(KeyCode.A)) {
            player.moveLeft();
        }
        if (isPressed(KeyCode.D)) {
            player.moveRight();
        }

        // Update player physics
        player.update();
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

    @Override
    public void start(Stage mainStage) throws Exception {
        initContent();

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