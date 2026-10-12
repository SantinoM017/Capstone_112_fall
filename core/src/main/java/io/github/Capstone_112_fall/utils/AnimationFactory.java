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
            StateComponent.State.LONG_IDLE, createPlayerLongIdle()
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
            createAnimation("character_fall", 0.1f, Animation.PlayMode.LOOP)
        );
        animComp.animations.put(
            StateComponent.State.ATTACKING,
            createAnimation("cross-body_sword_swing_frame", 0.1f, Animation.PlayMode.NORMAL)
        );
        animComp.animations.put(
            StateComponent.State.HURT,
            createAnimation("character_hurt", 0.075f, Animation.PlayMode.NORMAL)
        );
        animComp.animations.put(
            StateComponent.State.DEAD,
            createAnimation("character_death", 0.5f, Animation.PlayMode.NORMAL)
        );
        return animComp;
    }

    public AnimationComponent createEnemyAnimations(Engine engine) {
        AnimationComponent animComp = engine.createComponent(AnimationComponent.class);
        animComp.animations.put(
            StateComponent.State.WALKING, createBlueSlimeWalk()
        );
        return animComp;
    }

    private Animation<TextureRegion> createAnimation(String animationName, float frameDuration, Animation.PlayMode mode) {
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(animationName);
        return new Animation<>(frameDuration, regions, mode);
    }

    public Animation<TextureRegion> createBlueSlimeWalk() {
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions("blue_slime_walk");

        TextureRegion open1 = regions.get(0);
        TextureRegion open2 = regions.get(2);
        TextureRegion open3 = regions.get(3);
        TextureRegion open4 = regions.get(4);
        TextureRegion closedEye = regions.get(1);

        Array<TextureRegion> frameSequence = new Array<>();

        // Repeat open-eye cycle 5 times
        for (int i = 0; i < 5; i++) {
            frameSequence.add(open1);
            if(i == 4) {
                frameSequence.add(closedEye);
            } else {
                frameSequence.add(open2);
            }
            frameSequence.add(open3);
            frameSequence.add(open4);
        }


        return new Animation<>(0.15f, frameSequence, Animation.PlayMode.LOOP);
    }

    public Animation<TextureRegion> createPlayerLongIdle() {
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions("idle");

        TextureRegion open1 = regions.get(0);
        TextureRegion open2 = regions.get(1);
        TextureRegion open3 = regions.get(2);
        TextureRegion open4 = regions.get(3);
        TextureRegion closedEye = regions.get(4);

        Array<TextureRegion> frameSequence = new Array<>();

        // Repeat open-eye cycle 5 times
        for (int i = 0; i < 5; i++) {
            frameSequence.add(open1);
            if(i == 4) {
                frameSequence.add(closedEye);
            } else {
                frameSequence.add(open2);
            }
            frameSequence.add(open3);
            frameSequence.add(open4);
        }


        return new Animation<>(0.15f, frameSequence, Animation.PlayMode.LOOP);
    }
}
