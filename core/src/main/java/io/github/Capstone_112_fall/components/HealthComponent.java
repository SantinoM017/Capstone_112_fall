package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

// tags an entity that has health
public class HealthComponent implements Component {
    public int hp;
    public int maxHp;
    public float invulnerabilityTimer;
    public final float invulnerabilityDuration = 0.4f;
    public float hitStunTimer;
    public final float hitStunDuration = 0.3f;
    public boolean isTakingDamage;
    public Entity takingDamageFrom;

    public void takeDamage(int damage) {
        if (invulnerabilityTimer <= 0) {
            hp -= damage;
            invulnerabilityTimer = invulnerabilityDuration;
        }
    }

}
