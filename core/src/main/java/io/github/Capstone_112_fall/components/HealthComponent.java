package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;

// tags an entity that has health
public class HealthComponent implements Component {
    public int previousHp;
    public int hp;
    public int maxHp;
    public float invulnerabilityTimer;
    public final float invulnerabilityDuration = 0.5f;
    public float hitStunTimer;
    public final float hitStunDuration = 0.2f;
    public float knockbackResistance;
}

