package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;

// tags an entity that has health
public class HealthComponent implements Component {
    public int hp;
    public int maxHp;
    public boolean isVulnerable;
    public float invulnerabilityTimer;
}
