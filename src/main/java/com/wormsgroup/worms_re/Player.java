package com.wormsgroup.worms_re;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;
import javafx.scene.layout.Pane;
import java.util.ArrayList;

public class Player {
    private Node entity;
    private int hp;
    private int maxHp;

    private Rectangle hpBarBg;
    private Rectangle hpBarFill;
    private static final int HP_BAR_WIDTH  = 40;
    private static final int HP_BAR_HEIGHT = 5;

    private Point2D velocity = new Point2D(0, 0);
    private boolean canJump = true;

    private int levelWidth;
    private ArrayList<Node> platforms;
    private Pane gameRoot;

    private double turnTimer = 0;
    private static final double TURN_TIME_LIMIT    = 60.0;
    private double movementTimeRemaining = 0;
    private static final double MAX_MOVEMENT_TIME  = 10.0;
    private double postShootMoveTime = 0;
    private static final double POST_SHOOT_MOVE_TIME = 10.0;

    public enum ActionState {
        IDLE, MOVING, AIMING, POWER_SELECTION, SHOOTING, POST_SHOOT_MOVING
    }
    private ActionState currentState = ActionState.IDLE;

    private Weapon currentWeapon;
    private double aimAngle   = -45;
    private double shootPower = 0;
    private double chargeTimer = 0;
    private static final double MAX_CHARGE_TIME = 2.0;

    private Line aimLine;
    private ArrayList<Projectile> activeProjectiles;
    private ArrayList<Player>     allPlayers;
    private Node remoteGhost; // Added reference holder

    private java.util.function.BiConsumer<Player, Integer> hitCallback = null;
    public void setHitCallback(java.util.function.BiConsumer<Player, Integer> cb) { this.hitCallback = cb; }

    private static final int WIDTH              = 20;
    private static final int HEIGHT             = 20;
    private static final int GRAVITY            = 1;
    private static final int MAX_FALL_SPEED     = 10;
    private static final int JUMP_POWER         = 15;
    private static final int LATERAL_MOVE_SPEED = 3;

    public Player(int startX, int startY, int hp,
                  Pane gameRoot, Pane uiRoot,
                  ArrayList<Node> platforms, int levelWidth,
                  ArrayList<Projectile> activeProjectiles,
                  ArrayList<Player> allPlayers, Node remoteGhost) { // Constructor updated
        this.hp                 = hp;
        this.maxHp              = hp;
        this.gameRoot           = gameRoot;
        this.platforms          = platforms;
        this.levelWidth         = levelWidth;
        this.movementTimeRemaining = MAX_MOVEMENT_TIME;
        this.activeProjectiles  = activeProjectiles;
        this.allPlayers         = allPlayers;
        this.remoteGhost        = remoteGhost;

        Rectangle rect = new Rectangle(WIDTH, HEIGHT);
        rect.setTranslateX(startX);
        rect.setTranslateY(startY);
        rect.setFill(Color.BLUE);
        this.entity = rect;
        gameRoot.getChildren().add(entity);

        aimLine = new Line();
        aimLine.setStroke(Color.GOLD);
        aimLine.setStrokeWidth(2);
        aimLine.setVisible(false);
        gameRoot.getChildren().add(aimLine);

        hpBarBg   = new Rectangle(HP_BAR_WIDTH, HP_BAR_HEIGHT, Color.DARKGRAY);
        hpBarFill = new Rectangle(HP_BAR_WIDTH, HP_BAR_HEIGHT, Color.LIMEGREEN);
        uiRoot.getChildren().addAll(hpBarBg, hpBarFill);
    }

    public void updateHpBar(double cameraOffsetX) {
        double screenX = entity.getTranslateX() + cameraOffsetX;
        double screenY = entity.getTranslateY() - 10;

        hpBarBg.setTranslateX(screenX);
        hpBarBg.setTranslateY(screenY);

        double ratio = (double) hp / maxHp;
        hpBarFill.setWidth(HP_BAR_WIDTH * ratio);
        hpBarFill.setTranslateX(screenX);
        hpBarFill.setTranslateY(screenY);

        if (ratio > 0.5)       hpBarFill.setFill(Color.LIMEGREEN);
        else if (ratio > 0.25) hpBarFill.setFill(Color.YELLOW);
        else                   hpBarFill.setFill(Color.RED);
    }

    public void update(double deltaTime) {
        turnTimer += deltaTime;
        if (turnTimer >= TURN_TIME_LIMIT) endTurn();

        switch (currentState) {
            case IDLE:
            case MOVING:
                applyGravity();
                moveY((int) velocity.getY());
                break;
            case AIMING:
                applyGravity();
                moveY((int) velocity.getY());
                updateAimArrow();
                break;
            case POWER_SELECTION:
                applyGravity();
                moveY((int) velocity.getY());
                updateAimArrow();
                updatePowerCharge(deltaTime);
                break;
            case SHOOTING:
                applyGravity();
                moveY((int) velocity.getY());
                break;
            case POST_SHOOT_MOVING:
                postShootMoveTime -= deltaTime;
                if (postShootMoveTime <= 0) endTurn();
                applyGravity();
                moveY((int) velocity.getY());
                break;
        }
    }

    public void moveLeft(double deltaTime) {
        if (movementTimeRemaining > 0 && isStateAllowedToMove()) {
            if (entity.getTranslateX() >= LATERAL_MOVE_SPEED) {
                moveX(-LATERAL_MOVE_SPEED);
                movementTimeRemaining -= deltaTime;
                if (currentState == ActionState.IDLE) setActionState(ActionState.MOVING);
            }
        }
    }

    public void moveRight(double deltaTime) {
        if (movementTimeRemaining > 0 && isStateAllowedToMove()) {
            if (entity.getTranslateX() + WIDTH <= levelWidth - LATERAL_MOVE_SPEED) {
                moveX(LATERAL_MOVE_SPEED);
                movementTimeRemaining -= deltaTime;
                if (currentState == ActionState.IDLE) setActionState(ActionState.MOVING);
            }
        }
    }

    private boolean isStateAllowedToMove() {
        return currentState == ActionState.IDLE    || currentState == ActionState.MOVING   ||
               currentState == ActionState.AIMING  || currentState == ActionState.POWER_SELECTION ||
               currentState == ActionState.POST_SHOOT_MOVING;
    }

    public void jump() {
        if (canJump && (currentState == ActionState.IDLE   ||
                        currentState == ActionState.MOVING ||
                        currentState == ActionState.AIMING)) {
            velocity = new Point2D(velocity.getX(), -JUMP_POWER);
            canJump = false;
        }
    }

    public void startAiming(Weapon weapon) {
        if (currentState == ActionState.IDLE || currentState == ActionState.MOVING) {
            this.currentWeapon = weapon;
            setActionState(ActionState.AIMING);
            aimLine.setVisible(true);
        }
    }

    public void updateAim(double angleChange) {
        if (currentState == ActionState.AIMING || currentState == ActionState.POWER_SELECTION) {
            aimAngle += angleChange;
            aimAngle = Math.max(-180, Math.min(0, aimAngle));
        }
    }

    public void cancelAim() {
        if (currentState == ActionState.AIMING || currentState == ActionState.POWER_SELECTION) {
            setActionState(ActionState.IDLE);
            aimLine.setVisible(false);
            shootPower  = 0;
            chargeTimer = 0;
        }
    }

    private void updatePowerCharge(double deltaTime) {
        chargeTimer = Math.min(chargeTimer + deltaTime, MAX_CHARGE_TIME);
        shootPower  = (chargeTimer / MAX_CHARGE_TIME) * 100.0;
    }

    public void startCharging() {
        if (currentState == ActionState.AIMING) {
            chargeTimer = 0;
            shootPower  = 0;
            setActionState(ActionState.POWER_SELECTION);
        }
    }

    public void releaseCharge() {
        if (currentState == ActionState.POWER_SELECTION) shoot();
    }

    private void shoot() {
        setActionState(ActionState.SHOOTING);
        aimLine.setVisible(false);

        double spawnX  = entity.getTranslateX() + (WIDTH  / 2.0);
        double spawnY  = entity.getTranslateY() + (HEIGHT / 2.0);
        int    damage  = (currentWeapon != null) ? currentWeapon.getDamage() : 10;

        // Projectile setup now passes down the remoteGhost node
        Projectile proj = new Projectile(
            spawnX, spawnY, aimAngle, shootPower,
            gameRoot, platforms, allPlayers, remoteGhost, this, damage,
            hitCallback   
        );
        activeProjectiles.add(proj);

        postShootMoveTime = POST_SHOOT_MOVE_TIME;
        setActionState(ActionState.POST_SHOOT_MOVING);
    }

    public void endTurn() {
        turnTimer             = 0;
        movementTimeRemaining = MAX_MOVEMENT_TIME;
        postShootMoveTime     = 0;
        chargeTimer           = 0;
        aimLine.setVisible(false);
        setActionState(ActionState.IDLE);
    }

    public void takeDamage(int amount) { hp = Math.max(0, hp - amount); }

    private void applyGravity() {
        if (velocity.getY() < MAX_FALL_SPEED) velocity = velocity.add(0, GRAVITY);
    }

    private void moveX(int val) {
        boolean movingRight = val > 0;
        for (int i = 0; i < Math.abs(val); i++) {
            entity.setTranslateX(entity.getTranslateX() + (movingRight ? 1 : -1));
            for (Node platform : platforms) {
                if (entity.getBoundsInParent().intersects(platform.getBoundsInParent())) {
                    entity.setTranslateX(entity.getTranslateX() - (movingRight ? 1 : -1));
                    return;
                }
            }
        }
    }

    private void moveY(int val) {
        boolean movingDown = val > 0;
        for (int i = 0; i < Math.abs(val); i++) {
            entity.setTranslateY(entity.getTranslateY() + (movingDown ? 1 : -1));
            for (Node platform : platforms) {
                if (entity.getBoundsInParent().intersects(platform.getBoundsInParent())) {
                    entity.setTranslateY(entity.getTranslateY() - (movingDown ? 1 : -1));
                    if (movingDown) {
                        canJump  = true;
                        velocity = new Point2D(velocity.getX(), 0);
                    } else {
                        velocity = new Point2D(velocity.getX(), 0);
                    }
                    return;
                }
            }
        }
    }

    private void updateAimArrow() {
        double centerX = entity.getTranslateX() + (WIDTH  / 2.0);
        double centerY = entity.getTranslateY() + (HEIGHT / 2.0);
        double rad     = Math.toRadians(aimAngle);

        aimLine.setStartX(centerX);
        aimLine.setStartY(centerY);
        aimLine.setEndX(centerX + Math.cos(rad) * 40);
        aimLine.setEndY(centerY + Math.sin(rad) * 40);
    }

    public void stopMovingState() {
        if (currentState == ActionState.MOVING) setActionState(ActionState.IDLE);
    }

    private void setActionState(ActionState s)  { this.currentState = s; }
    public Node        getEntity()               { return entity; }
    public int         getHp()                   { return hp; }
    public void        setHp(int value)          { hp = Math.max(0, Math.min(maxHp, value)); }
    public int         getWeaponDamage()         { return (currentWeapon != null) ? currentWeapon.getDamage() : 10; }
    public double      getTurnTimeRemaining()     { return Math.max(0, TURN_TIME_LIMIT - turnTimer); }
    public double      getMovementTimeRemaining() { return Math.max(0, movementTimeRemaining); }
    public double      getShootPower()            { return shootPower; }
    public double      getAimAngle()              { return aimAngle; }
    public ActionState getCurrentState()          { return currentState; }
}