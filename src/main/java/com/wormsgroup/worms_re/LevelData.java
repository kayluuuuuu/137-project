package com.wormsgroup.worms_re;
import javafx.geometry.Point2D;

public class LevelData {
    // 0 = open space
    // 1 = platform
    // 40x40 squares
    public static final String[] LEVEL1 = new String[] {
            "000000000000000000000000000000", // Row 0
            "000000000000000000000000000000", // Row 1
            "000000000000000000000000000000", // Row 2
            "000000000000000000000000000000", // Row 3
            "000000000000000000000000000000", // Row 4
            "000000000000000000000000000000", // Row 5
            "000000000000000000000000000000", // Row 6
            "000000000000000000000000000000", // Row 7
            "000000000000000000000000000000", // Row 8
            "000000000000000000000000000000", // Row 9
            "000000000000000000000000000000", // Row 10
            "000000001100000000000000000000", // Row 11 -> Floating Platform 1
            "000000000000000000000000000000", // Row 12
            "000011000000011110000000000000", // Row 13 -> Floating Platforms 2 & 3
            "111111111111111111111111111111"  // Row 14 -> Main Ground Floor
    };

    // Adjusted Random Spawnpoints to align with the grid above
    public static final Point2D[] SPAWN_POINTS = {
        new Point2D(80, 540),   // Left side of the main floor
        new Point2D(180, 500),  // On top of the left floating platform (Row 13)
        new Point2D(340, 420),  // On top of the high central floating platform (Row 11)
        new Point2D(560, 500),  // On top of the long middle floating platform (Row 13)
        new Point2D(760, 540),  // Middle-right side of the main floor
        new Point2D(1000, 540)  // Far right side of the main floor
    };
}