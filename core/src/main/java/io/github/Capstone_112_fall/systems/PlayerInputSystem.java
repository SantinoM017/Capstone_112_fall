package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import io.github.Capstone_112_fall.components.Box2DComponent;
import io.github.Capstone_112_fall.components.PlayerComponent;

public class PlayerInputSystem extends IteratingSystem {
    private final ComponentMapper<Box2DComponent> box2DComponent = ComponentMapper.getFor(Box2DComponent.class);
    private final ComponentMapper<PlayerComponent> playerComponent = ComponentMapper.getFor(PlayerComponent.class);

    public PlayerInputSystem() {
        super(Family.all(Box2DComponent.class, PlayerComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Box2DComponent box2D = box2DComponent.get(entity);
        PlayerComponent player = playerComponent.get(entity);

        if(box2D.body == null) return;
        Vector2 vel = box2D.body.getLinearVelocity();
        float speed = 2f; // meters per second

        // horizontal movement
        if(Gdx.input.isKeyPressed(Input.Keys.A)){
            box2D.body.setLinearVelocity(-speed, vel.y);
        } else if(Gdx.input.isKeyPressed(Input.Keys.D)){
            box2D.body.setLinearVelocity(speed, vel.y);
        } else {
            box2D.body.setLinearVelocity(0, vel.y);
        }

        // vertical movement. Guardrails to prevent jumping while in the air
        if(Gdx.input.isKeyJustPressed(Input.Keys.W) && player.isGrounded){
            box2D.body.applyLinearImpulse(
                new Vector2(0, 6f),
                box2D.body.getWorldCenter(),
                true
            );
        }

        if(!Gdx.input.isKeyPressed(Input.Keys.W) && vel.y > 0){
            box2D.body.setLinearVelocity(vel.x, vel.y * 0.5f);
        }
    }
}
