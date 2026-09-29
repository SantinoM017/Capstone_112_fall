package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;

// tags anything with a position and rotation. Differs from box2d component in that it is not tied to a physics body and can be used for non-physics entities
public class TransformComponent implements Component {
    public float x, y, rotation;
}
