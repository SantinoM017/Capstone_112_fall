package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;

// tags a player entity
public class PlayerComponent implements Component {
    public boolean isAttacking;
    public boolean attackRequested;
    public float attackCooldown;
    public float jumpCooldown;
    public boolean isFlipped;
    public boolean isMoving;
}
