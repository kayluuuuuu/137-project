package com.wormsgroup.worms_re;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.Pane;

import java.util.ArrayList;

public class Player {
    private Node entity;
    private Point2D velocity = new Point2D(0, 0);
    private boolean canJump = true;
    private int levelWidth;
    private ArrayList<Node> platforms;
    private Pane gameRoot;

    private static final int WIDTH = 20;
    private static final int HEIGHT = 20;
    private static final int GRAVITY = 1;
    private static final int MAX_FALL_SPEED = 10;
    private static final int JUMP_POWER = 30;

    public Player(int startX, int startY, Pane gameRoot, ArrayList<Node> platforms, int levelWidth) {
        this.gameRoot = gameRoot;
        this.platforms = platforms;
        this.levelWidth = levelWidth;

        // Create player entity
        Rectangle rect = new Rectangle(WIDTH, HEIGHT);
        rect.setTranslateX(startX);
        rect.setTranslateY(startY);
        rect.setFill(Color.BLUE);

        this.entity = rect;
        gameRoot.getChildren().add(entity);
    }

    /**
     * Update player position based on velocity and collisions
     */
    public void update() {
        // Apply gravity
        if (velocity.getY() < MAX_FALL_SPEED) {
            velocity = velocity.add(0, GRAVITY);
        }

        // Move player vertically
        double oldY = entity.getTranslateY();
        moveY((int) velocity.getY());

        // Check if Y position changed (collision detection)
        if (oldY != entity.getTranslateY()) {
            canJump = false;
        }
    }

    /**
     * Move player left
     */
    public void moveLeft() {
        if (entity.getTranslateX() >= 5) {
            moveX(-5);
        }
    }

    /**
     * Move player right
     */
    public void moveRight() {
        if (entity.getTranslateX() + WIDTH <= levelWidth - 5) {
            moveX(5);
        }
    }

    /**
     * Make player jump
     */
    public void jump() {
        if (canJump) {
            velocity = velocity.add(0, -JUMP_POWER);
            canJump = false;
        }
    }

    /**
     * Move player along X-axis with collision detection
     */
    private void moveX(int val) {
        boolean movingRight = val > 0;

        for (int i = 0; i < Math.abs(val); i++) {
            for (Node platform : platforms) {
                if (entity.getBoundsInParent().intersects(platform.getBoundsInParent())) {
                    if (movingRight) {
                        if (entity.getTranslateX() + WIDTH == platform.getTranslateX()) {
                            return;
                        }
                    } else {
                        if (entity.getTranslateX() == platform.getTranslateX() + 40) {
                            return;
                        }
                    }
                }
            }
            entity.setTranslateX(entity.getTranslateX() + (movingRight ? 1 : -1));
        }
    }

    /**
     * Move player along Y-axis with collision detection
     */
    private void moveY(int val) {
        boolean movingDown = val > 0;

        for (int i = 0; i < Math.abs(val); i++) {
            for (Node platform : platforms) {
                if (entity.getBoundsInParent().intersects(platform.getBoundsInParent())) {
                    if (movingDown) {
                        if (entity.getTranslateY() + HEIGHT == platform.getTranslateY()) {
                            entity.setTranslateY(entity.getTranslateY() - 1);
                            canJump = true;
                            return;
                        }
                    } else {
                        if (entity.getTranslateY() == platform.getTranslateY() + 40) {
                            return;
                        }
                    }
                }
            }
            entity.setTranslateY(entity.getTranslateY() + (movingDown ? 1 : -1));
        }
    }

    // Getters
    public Node getEntity() {
        return entity;
    }

    public Point2D getVelocity() {
        return velocity;
    }

    public void setVelocity(Point2D velocity) {
        this.velocity = velocity;
    }

    public int getX() {
        return (int) entity.getTranslateX();
    }

    public int getY() {
        return (int) entity.getTranslateY();
    }
}