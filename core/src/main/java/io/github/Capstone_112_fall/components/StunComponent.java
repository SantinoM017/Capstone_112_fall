package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class StunComponent implements Component {
    public float stunTime;
    public boolean applyStun;
    public Entity stunnedEntity;
}
