package io.github.Capstone_112_fall.Screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class MainScreen extends Game {
    private Skin skin;
    private TextureAtlas uiAtlas;

    @Override
    public void create() {
        uiAtlas = new TextureAtlas(Gdx.files.internal("ui.atlas"));
        skin = new Skin(Gdx.files.internal("ui.json"), uiAtlas);
        setScreen(new MainMenuScreen(this));
    }

    public Skin getSkin() {
        return skin;
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        if(screen != null) screen.dispose();
        if(skin != null) skin.dispose();
        if(uiAtlas != null) uiAtlas.dispose();
    }
}
