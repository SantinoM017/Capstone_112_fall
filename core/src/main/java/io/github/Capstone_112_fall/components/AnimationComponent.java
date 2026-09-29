package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectMap;

// map enum state to animation name
public class AnimationComponent implements Component {
    public ObjectMap<StateComponent.State, Animation<TextureRegion>> animations = new ObjectMap<>();
}
