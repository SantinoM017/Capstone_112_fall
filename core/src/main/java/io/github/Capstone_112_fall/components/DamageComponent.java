package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

// tags an entity that can deal damage
public class DamageComponent implements Component {
    public int damage;
    public Entity dealingDamageTo;
    public boolean applyDamage;

    public void takeDamage(int damage, HealthComponent hp) {
        if (hp.invulnerabilityTimer <= 0) {
            hp.hp -= damage;
            System.out.println(hp.hp);
        }
    }
}
