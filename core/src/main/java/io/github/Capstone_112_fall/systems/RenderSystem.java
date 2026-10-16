package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
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

    private final ComponentMapper<TextureComponent> textureMapper = ComponentMapper.getFor(TextureComponent.class);
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);

    private final ComponentMapper<AnimationComponent> animationMapper = ComponentMapper.getFor(AnimationComponent.class);
    private final ComponentMapper<StateComponent> stateMapper = ComponentMapper.getFor(StateComponent.class);
    private final ComponentMapper<HealthComponent> healthMapper = ComponentMapper.getFor(HealthComponent.class);
    private final ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private ImmutableArray<Entity> renderQueue;
    private final TextureAtlas atlas;

    public RenderSystem(SpriteBatch batch, OrthographicCamera camera, TextureAtlas atlas) {
        this.batch = batch;
        this.camera = camera;
        this.atlas = atlas;
        hitShader = new ShaderProgram(
            Gdx.files.internal("shaders/default.vert"),
            Gdx.files.internal("shaders/hit_flash.frag")
        );
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
            HealthComponent health = healthMapper.get(entity);

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
