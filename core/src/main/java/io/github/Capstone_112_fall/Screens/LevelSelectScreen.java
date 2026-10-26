package io.github.Capstone_112_fall.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.Capstone_112_fall.Main;
import io.github.Capstone_112_fall.utils.SaveManager;

public class LevelSelectScreen extends ScreenAdapter {
    private static final String[] LEVEL_PATHS = {
        "levels/level1.tmx",
        "levels/level2.tmx"
    };

    private final MainScreen game;
    private Stage stage;

    public LevelSelectScreen(MainScreen game) {
        this.game = game;
    }

    @Override
    public void show() {
        Skin skin = game.getSkin();
        int unlockedLevels = SaveManager.getHighestUnlockedLevel();
        stage = new Stage(new FitViewport(400, 240));
        Gdx.input.setInputProcessor(stage);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        Table panel = new Table();
        panel.pad(16);
        panel.setBackground(skin.newDrawable("round-white", new Color(1f, 0.84f, 0.89f, 1f)));
        rootTable.add(panel);

        Label.LabelStyle titleStyle = new Label.LabelStyle(skin.getFont("monogram"), new Color(0.35f, 0.16f, 0.24f, 1f));
        Label title = new Label("SELECT LEVEL", titleStyle);
        title.setFontScale(1.5f);
        title.invalidate();
        panel.add(title).padBottom(9).row();

        Label.LabelStyle subtitleStyle = new Label.LabelStyle(skin.getFont("monogram"), new Color(0.48f, 0.27f, 0.35f, 1f));
        Label subtitle = new Label("CHOOSE A STAGE", subtitleStyle);
        subtitle.setFontScale(0.75f);
        subtitle.invalidate();
        panel.add(subtitle).padBottom(19).row();

        Table gridTable = new Table();
        for (int i = 1; i <= LEVEL_PATHS.length; i++) {
            boolean isUnlocked = i <= unlockedLevels;
            TextButton levelButton = new TextButton(
                isUnlocked ? String.valueOf(i) : "",
                createButtonStyle(skin)
            );
            levelButton.getLabel().setFontScale(0.75f);
            levelButton.getLabel().invalidate();

            if (isUnlocked) {
                final String levelPath = LEVEL_PATHS[i - 1];
                levelButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        game.setScreen(new Main(game, levelPath));
                    }
                });
            } else {
                levelButton.setDisabled(true);
            }

            gridTable.add(levelButton).size(44, 44).pad(5);
            if (i % 5 == 0) {
                gridTable.row();
            }
        }

        panel.add(gridTable).row();

        TextButton backButton = new TextButton("BACK", createButtonStyle(skin));
        backButton.getLabel().setFontScale(0.75f);
        backButton.getLabel().invalidate();
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
        panel.add(backButton).size(110, 34).padTop(10);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void hide() {
        disposeUi();
    }

    @Override
    public void dispose() {
        disposeUi();
    }

    private void disposeUi() {
        if (stage != null) {
            if (Gdx.input.getInputProcessor() == stage) {
                Gdx.input.setInputProcessor(null);
            }
            stage.dispose();
            stage = null;
        }
    }

    private static TextButton.TextButtonStyle createButtonStyle(Skin skin) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(
            skin.get("default", TextButton.TextButtonStyle.class)
        );
        if (style.fontColor != null) style.fontColor = new Color(style.fontColor);
        if (style.downFontColor != null) style.downFontColor = new Color(style.downFontColor);
        if (style.overFontColor != null) style.overFontColor = new Color(style.overFontColor);
        if (style.checkedFontColor != null) style.checkedFontColor = new Color(style.checkedFontColor);
        if (style.disabledFontColor != null) style.disabledFontColor = new Color(style.disabledFontColor);
        return style;
    }
}
