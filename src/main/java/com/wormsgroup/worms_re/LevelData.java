package com.wormsgroup.worms_re;
import javafx.geometry.Point2D;

public class LevelData {
    
    public static final String[] LEVEL1 = {
        "000000000000000000000000000000",
        "000000000000000000000000000000",
        "000000000000000000000000000000",
        "000000000000000000000000000000",
        "000000000000001111100000000000",
        "000000000000000000000000000000",
        "000000011110000000000111100000",
        "000000000000000000000000000000",
        "000011100000000000000000011100",
        "111111111111111111111111111111"
    };

    // Your random spawn points array
    public static final Point2D[] SPAWN_POINTS = {
        new Point2D(80, 360),
        new Point2D(240, 200),
        new Point2D(400, 360),
        new Point2D(600, 160),
        new Point2D(720, 400),
        new Point2D(900, 240)
    };
}