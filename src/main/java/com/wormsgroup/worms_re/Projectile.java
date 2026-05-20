package com.wormsgroup.worms_re;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.ArrayList;

public class Projectile {
    private Circle sprite;
    private Point2D velocity;
    private Pane gameRoot;
    private ArrayList<Node> platforms;
    private boolean active = true;

    private static final double GRAVITY = 0.3;

    public Projectile(double startX, double startY, double angleDegrees, double power, Pane gameRoot, ArrayList<Node> platforms) {
        this.gameRoot = gameRoot;
        this.platforms = platforms;

        // Create visual sprite
        sprite = new Circle(5, Color.RED);
        sprite.setTranslateX(startX);
        sprite.setTranslateY(startY);
        gameRoot.getChildren().add(sprite);

        // Convert angle from degrees to radians and calculate velocity components
        // In screen coordinates, negative Y goes up
        double radians = Math.toRadians(angleDegrees);
        double speed = power * 0.15; // Scaler to make physics feel natural
        this.velocity = new Point2D(Math.cos(radians) * speed, Math.sin(radians) * speed);
    }

    public void update() {
        if (!active) return;

        // Apply gravity to projectile velocity vector
        velocity = velocity.add(0, GRAVITY);

        // Update position
        sprite.setTranslateX(sprite.getTranslateX() + velocity.getX());
        sprite.setTranslateY(sprite.getTranslateY() + velocity.getY());

        // Simple bounding collision check with platforms
        for (Node platform : platforms) {
            if (sprite.getBoundsInParent().intersects(platform.getBoundsInParent())) {
                destroy();
                break;
            }
        }

        // Out of bounds check
        if (sprite.getTranslateY() > 800 || sprite.getTranslateX() < 0 || sprite.getTranslateX() > 1000) {
            destroy();
        }
    }

    public void destroy() {
        if (active) {
            active = false;
            gameRoot.getChildren().remove(sprite);
        }
    }

    public boolean isActive() {
        return active;
    }
}