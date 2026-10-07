package io.github.Capstone_112_fall.Screens;

import com.badlogic.gdx.Game;
import io.github.Capstone_112_fall.Main;

public class MainScreen extends Game {
    @Override
    public void create() {
        setScreen(new Main(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        if(screen != null) screen.dispose();
    }
}
