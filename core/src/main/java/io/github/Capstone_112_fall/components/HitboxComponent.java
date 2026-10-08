package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;

public class HitboxComponent implements Component {
    public Body body;
    public Fixture mainFixture;
    public Fixture hurtboxFixture;
}
