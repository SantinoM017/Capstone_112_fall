package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;

// tags an entity that can deal damage
public class DamageComponent implements Component {
    public int damage;
    public boolean hasHit;
}
