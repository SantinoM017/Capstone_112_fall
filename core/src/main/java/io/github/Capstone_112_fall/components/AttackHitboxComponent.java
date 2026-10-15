package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.physics.box2d.Fixture;

// tags an entity that has a hitbox for an attack
public class AttackHitboxComponent implements Component {
    public Fixture sensorFixture;
}
