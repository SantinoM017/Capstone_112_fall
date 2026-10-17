package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;

public class KnockbackComponent implements Component {
    public float xVel;
    public float yVel;
    public Entity dealingKnockbackTo;
    public boolean applyKnockback;
}
