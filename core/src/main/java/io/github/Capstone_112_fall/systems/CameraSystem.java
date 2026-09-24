package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import io.github.Capstone_112_fall.components.PlayerComponent;
import io.github.Capstone_112_fall.components.TransformComponent;

public class CameraSystem extends IteratingSystem {
    private final OrthographicCamera camera;
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);
    private float minX, minY, maxX, maxY;

    // camera follows player
    public CameraSystem(OrthographicCamera camera){
        super(Family.all(PlayerComponent.class, TransformComponent.class).get());
        this.camera = camera;
    }

    // makes sure camera doesn't go out of bounds
    public void setMapBounds(float width, float height){
        float halfViewportWidth = camera.viewportWidth / 2;
        float halfViewportHeight = camera.viewportHeight / 2;
        // viewports are centered on the camera position, so the min and max values are offset by half the viewport size
        minX = halfViewportWidth;
        minY = halfViewportHeight;
        maxX = Math.max(halfViewportWidth, width-halfViewportWidth);
        maxY = Math.max(halfViewportHeight, height-halfViewportHeight);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        if(transform == null) return;

        // where the player is
        float targetX = transform.x;
        float targetY = transform.y;

        // smoothly move the camera to the player
        float lerp = 0.1f;
        float currentX = MathUtils.lerp(camera.position.x, targetX, lerp);
        float currentY = MathUtils.lerp(camera.position.y, targetY, lerp);

        // make sure camera doesn't go out of bounds
        camera.position.x = MathUtils.clamp(currentX, minX, maxX);
        camera.position.y = MathUtils.clamp(currentY, minY, maxY);
        camera.update();
    }
}
