package com.wormsgroup.worms_re;

public class Weapon {
    private String name;
    private boolean requiresPowerBar;
    private int damage;

    public Weapon(String name, boolean requiresPowerBar, int damage) {
        this.name = name;
        this.requiresPowerBar = requiresPowerBar;
        this.damage = damage;
    }

    public String getName() { return name; }
    public boolean requiresPowerBar() { return requiresPowerBar; }
    public int getDamage() { return damage; }
}