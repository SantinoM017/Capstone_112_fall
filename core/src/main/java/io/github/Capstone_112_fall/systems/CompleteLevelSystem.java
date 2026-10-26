package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.Main;
import io.github.Capstone_112_fall.components.ComponentMappers;
import io.github.Capstone_112_fall.components.PlayerComponent;
import io.github.Capstone_112_fall.utils.SaveManager;

public class CompleteLevelSystem extends IteratingSystem {
    private final Main gameplayScreen;
    private final int completedLevel;

    public CompleteLevelSystem(Main gameplayScreen, int completedLevel) {
        super(Family.all(PlayerComponent.class).get());
        this.gameplayScreen = gameplayScreen;
        this.completedLevel = completedLevel;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PlayerComponent player = ComponentMappers.PLAYER.get(entity);

        if (player != null && player.completedLevel) {
            player.completedLevel = false;
            SaveManager.unlockNextLevel(completedLevel);
            gameplayScreen.showVictoryDialog(completedLevel);
        }
    }
}
