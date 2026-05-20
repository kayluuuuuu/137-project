package com.wormsgroup.worms_re;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.ArrayList;
import java.util.function.BiConsumer;

public class Projectile {
    private Circle  sprite;
    private Point2D velocity;
    private Pane    gameRoot;
    private ArrayList<Node>   platforms;
    private ArrayList<Player> players;
    private Node    remoteGhost; 
    private Player  shooter;
    private int     damage;
    private boolean active = true;

    private BiConsumer<Player, Integer> onHitPlayer;
    private static final double GRAVITY = 0.3;

    public Projectile(double startX, double startY, double angleDegrees, double power,
                      Pane gameRoot, ArrayList<Node> platforms,
                      ArrayList<Player> players, Node remoteGhost, Player shooter, int damage,
                      BiConsumer<Player, Integer> onHitPlayer) {
        this.gameRoot     = gameRoot;
        this.platforms    = platforms;
        this.players      = players;
        this.remoteGhost  = remoteGhost;
        this.shooter      = shooter;
        this.damage       = damage;
        this.onHitPlayer  = onHitPlayer;

        sprite = new Circle(5, Color.RED);
        sprite.setTranslateX(startX);
        sprite.setTranslateY(startY);
        gameRoot.getChildren().add(sprite);

        double radians = Math.toRadians(angleDegrees);
        double speed   = power * 0.15;
        this.velocity  = new Point2D(Math.cos(radians) * speed, Math.sin(radians) * speed);
    }

    public void update() {
        if (!active) return;

        velocity = velocity.add(0, GRAVITY);
        sprite.setTranslateX(sprite.getTranslateX() + velocity.getX());
        sprite.setTranslateY(sprite.getTranslateY() + velocity.getY());

        for (Node platform : platforms) {
            if (sprite.getBoundsInParent().intersects(platform.getBoundsInParent())) {
                destroy();
                return;
            }
        }

        if (remoteGhost != null && remoteGhost.isVisible()) {
            if (sprite.getBoundsInParent().intersects(remoteGhost.getBoundsInParent())) {
                if (onHitPlayer != null) {
                    onHitPlayer.accept(null, damage); 
                }
                destroy();
                return;
            }
        }

        for (Player p : players) {
            if (p == shooter) continue;
            if (sprite.getBoundsInParent().intersects(p.getEntity().getBoundsInParent())) {
                if (onHitPlayer != null) onHitPlayer.accept(p, damage);
                destroy();
                return;
            }
        }

        if (sprite.getTranslateY() > 800 ||
            sprite.getTranslateX() < 0   ||
            sprite.getTranslateX() > 1200) {
            destroy();
        }
    }

    public void destroy() {
        if (active) {
            active = false;
            gameRoot.getChildren().remove(sprite);
        }
    }

    public boolean isActive() { return active; }
    
    public Player getShooter() { return shooter; }
}