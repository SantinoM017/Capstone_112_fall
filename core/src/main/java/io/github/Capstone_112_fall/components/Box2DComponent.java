package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.physics.box2d.Body;

// tags an entity that has a box2d body
public class Box2DComponent implements Component {
    public Body body;
}
