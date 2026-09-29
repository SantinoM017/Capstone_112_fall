package io.github.Capstone_112_fall.utils;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.github.Capstone_112_fall.components.AnimationComponent;
import io.github.Capstone_112_fall.components.StateComponent;

// creates animations for entities based on their state
public class AnimationFactory {
    private final TextureAtlas atlas;

    public AnimationFactory(TextureAtlas atlas) {
        this.atlas = atlas;
    }

    public AnimationComponent createPlayerAnimations(Engine engine) {
        AnimationComponent animComp = engine.createComponent(AnimationComponent.class);

        animComp.animations.put(
            StateComponent.State.IDLE,
            createAnimation("box_man", 0.1f, Animation.PlayMode.LOOP)
        );
        animComp.animations.put(
            StateComponent.State.WALKING,
            createAnimation("box_man_walk", 0.2f, Animation.PlayMode.LOOP)
        );
        animComp.animations.put(
            StateComponent.State.JUMPING,
            createAnimation("box_man_jump", 0.1f, Animation.PlayMode.NORMAL)
        );
        return animComp;
    }

    private Animation<TextureRegion> createAnimation(String animationName, float frameDuration, Animation.PlayMode mode) {
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(animationName);
        return new Animation<>(frameDuration, regions, mode);
    }
}
