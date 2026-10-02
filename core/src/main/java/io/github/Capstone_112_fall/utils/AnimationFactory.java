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
            createAnimation("character", 0.1f, Animation.PlayMode.LOOP)
        );
        animComp.animations.put(
            StateComponent.State.WALKING,
            createAnimation("character_walk", 0.1f, Animation.PlayMode.LOOP)
        );
        animComp.animations.put(
            StateComponent.State.JUMPING,
            createAnimation("character_jump", 0.1f, Animation.PlayMode.NORMAL)
        );
        animComp.animations.put(
            StateComponent.State.FALLING,
            createAnimation("character_fall", 0.1f, Animation.PlayMode.NORMAL)
        );
        return animComp;
    }

    private Animation<TextureRegion> createAnimation(String animationName, float frameDuration, Animation.PlayMode mode) {
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(animationName);
        return new Animation<>(frameDuration, regions, mode);
    }
}
