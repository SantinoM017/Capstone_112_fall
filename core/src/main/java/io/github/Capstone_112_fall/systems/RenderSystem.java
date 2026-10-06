package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.github.Capstone_112_fall.components.AnimationComponent;
import io.github.Capstone_112_fall.components.StateComponent;
import io.github.Capstone_112_fall.components.TextureComponent;
import io.github.Capstone_112_fall.components.TransformComponent;
import io.github.Capstone_112_fall.Main;

// draws the actual sprites
public class RenderSystem extends EntitySystem {
    private final SpriteBatch batch;
    private final OrthographicCamera camera;

    private final ComponentMapper<TextureComponent> textureMapper = ComponentMapper.getFor(TextureComponent.class);
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);

    private final ComponentMapper<AnimationComponent> animationMapper = ComponentMapper.getFor(AnimationComponent.class);
    private final ComponentMapper<StateComponent> stateMapper = ComponentMapper.getFor(StateComponent.class);

    private ImmutableArray<Entity> renderQueue;

    public RenderSystem(SpriteBatch batch, OrthographicCamera camera) {
        this.batch = batch;
        this.camera = camera;
    }

    @Override
    public void addedToEngine(Engine engine) {
        renderQueue = engine.getEntitiesFor(
            Family.all(TextureComponent.class, TransformComponent.class).get()
        );
    }

    @Override
    public void update(float deltaTime) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Array<Entity> sortedEntities = new Array<>(renderQueue.size());
        for(Entity entity : renderQueue) {
            sortedEntities.add(entity);
        }
        sortedEntities.sort((first, second) -> Integer.compare(
            textureMapper.get(first).zIndex,
            textureMapper.get(second).zIndex
        ));

        for(Entity entity : sortedEntities) {
            TextureComponent texture = textureMapper.get(entity);
            TransformComponent transform = transformMapper.get(entity);
            AnimationComponent animation = animationMapper.get(entity);
            StateComponent state = stateMapper.get(entity);

            if(animation != null && state != null && animation.animations.containsKey(state.currentState)) {
                Animation<TextureRegion> currentAnimation = animation.animations.get(state.currentState);
                texture.textureRegion = currentAnimation.getKeyFrame(state.stateTime);
            }

            // Render the entity
            if(texture.textureRegion == null) continue;

            if(texture.sourceRegion != texture.textureRegion) {
                texture.sourceRegion = texture.textureRegion;
                texture.renderRegion = new TextureRegion(texture.textureRegion);
            }
            if(texture.renderRegion.isFlipX() != texture.flipX) {
                texture.renderRegion.flip(true, false);
            }
            if(texture.renderRegion.isFlipY() != texture.flipY) {
                texture.renderRegion.flip(false, true);
            }

            // scale by PPM (pixels per meter) to convert from Box2D units to pixels
            float width = texture.renderRegion.getRegionWidth() / Main.PPM;
            float height = texture.renderRegion.getRegionHeight() / Main.PPM;

            float originX = width / 2f;
            float originY = height / 2f;

            float drawX = transform.x - originX;
            float drawY = transform.y - originY;
            if(texture.bottomAligned) {
                drawY = transform.y - 0.5f - originY + originY * texture.scale;
            }

            // Modified by Claude (Anthropic AI assistant)
            batch.draw(
                texture.renderRegion,
                drawX, drawY,
                originX, originY,
                width, height,
                texture.scale, texture.scale,
                transform.rotation
            );
        }
        batch.end();
    }
}
