package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import io.github.Capstone_112_fall.components.*;
import io.github.Capstone_112_fall.Main;

// draws the actual sprites
public class RenderSystem extends EntitySystem {
    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private final ShaderProgram hitShader;

    private ImmutableArray<Entity> renderQueue;
    private final Array<Entity> sortedEntities = new Array<>();
    private final TextureAtlas atlas;
    private boolean renderQueueDirty = true;

    public RenderSystem(SpriteBatch batch, OrthographicCamera camera, TextureAtlas atlas, ShaderProgram hitShader) {
        this.batch = batch;
        this.camera = camera;
        this.atlas = atlas;
        this.hitShader = hitShader;
    }

    @Override
    public void addedToEngine(Engine engine) {
        Family renderFamily = Family.all(TextureComponent.class, TransformComponent.class).get();
        renderQueue = engine.getEntitiesFor(renderFamily);
        engine.addEntityListener(renderFamily, new EntityListener() {
            @Override
            public void entityAdded(Entity entity) {
                renderQueueDirty = true;
            }

            @Override
            public void entityRemoved(Entity entity) {
                renderQueueDirty = true;
            }
        });
    }

    @Override
    public void update(float deltaTime) {
        if (renderQueueDirty) {
            sortedEntities.clear();
            for (Entity entity : renderQueue) {
                sortedEntities.add(entity);
            }
            sortedEntities.sort((first, second) -> Integer.compare(
                ComponentMappers.TEXTURE.get(first).zIndex,
                ComponentMappers.TEXTURE.get(second).zIndex
            ));
            renderQueueDirty = false;
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for(Entity entity : sortedEntities) {
            TextureComponent texture = ComponentMappers.TEXTURE.get(entity);
            TransformComponent transform = ComponentMappers.TRANSFORM.get(entity);
            AnimationComponent animation = ComponentMappers.ANIMATION.get(entity);
            StateComponent state = ComponentMappers.STATE.get(entity);
            HealthComponent health = ComponentMappers.HEALTH.get(entity);

            if(animation != null && state != null && animation.animations.containsKey(state.currentState)) {
                Animation<TextureRegion> currentAnimation = animation.animations.get(state.currentState);
                texture.textureRegion = currentAnimation.getKeyFrame(state.stateTime);
            }

            // Render the entity
            if(texture.textureRegion == null) continue;

            if (texture.renderRegion == null) {
                texture.renderRegion = new TextureRegion(texture.textureRegion);
                texture.sourceRegion = texture.textureRegion;
            } else if (texture.sourceRegion != texture.textureRegion) {
                texture.sourceRegion = texture.textureRegion;
                texture.renderRegion.setRegion(texture.textureRegion);
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

            // render hurt animation. Written by gemini
            if (state != null && state.currentState == StateComponent.State.HURT && animation != null) {
                Animation<TextureRegion> hurtAnim = animation.animations.get(StateComponent.State.HURT);
                int currentFrameIndex = hurtAnim.getKeyFrameIndex(state.stateTime);

                switch (currentFrameIndex) {
                    case 0: // Frame 1: Light White Tint (Contact)
                        hitShader.setUniformf("u_flashAmount", 0.5f);
                        batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                        break;

                    case 1: // Frame 2: Solid White Flash (Peak Impact)
                        hitShader.setUniformf("u_flashAmount", 1.0f);
                        batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                        break;

                    case 2: // Frame 3: Solid Red Tint (Knockback Pain)
                        hitShader.setUniformf("u_flashAmount", 0.5f);
                        batch.setColor(1.0f, 0.2f, 0.2f, 1.0f);
                        break;

                    case 3: // Frame 4: Recovery
                    default:
                        hitShader.setUniformf("u_flashAmount", 0.0f);
                        batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                        break;
                }
            } else if (health != null && health.invulnerabilityTimer > 0 && state != null) {
                // Post hit-stun i-frame flickering
                hitShader.setUniformf("u_flashAmount", 0);
                float alpha = (Math.sin(state.stateTime * 35f) > 0) ? 0.3f : 1.0f;
                batch.setColor(1.0f, 1f, 1f, alpha);
            } else {
                // Default rendering
                hitShader.setUniformf("u_flashAmount", 0.0f);
                batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
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
