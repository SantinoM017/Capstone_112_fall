package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Game;
import io.github.Capstone_112_fall.Main;
import io.github.Capstone_112_fall.Screens.MainScreen;
import io.github.Capstone_112_fall.components.Box2DComponent;
import io.github.Capstone_112_fall.components.HealthComponent;
import io.github.Capstone_112_fall.components.PlayerComponent;

public class DeathSystem extends IteratingSystem {
    private final MainScreen game;
    private final ComponentMapper<PlayerComponent> playerComponent = ComponentMapper.getFor(PlayerComponent.class);
    private final ComponentMapper<Box2DComponent> box2DComponent = ComponentMapper.getFor(Box2DComponent.class);
    private final ComponentMapper<HealthComponent> healthComponent = ComponentMapper.getFor(HealthComponent.class);
    public DeathSystem(MainScreen game){
        super(Family.all(PlayerComponent.class, Box2DComponent.class).get());
        this.game = game;
    }

    protected void processEntity(Entity entity, float deltaTime) {
        PlayerComponent player = playerComponent.get(entity);
        Box2DComponent box2D = box2DComponent.get(entity);
        HealthComponent health = healthComponent.get(entity);
        if(box2D.body.getPosition().y < -10f) {
            health.hp -= 1;
        }
        if (player != null && health.hp <= 0) {
            game.setScreen(new Main(game));
        }
    }
}
