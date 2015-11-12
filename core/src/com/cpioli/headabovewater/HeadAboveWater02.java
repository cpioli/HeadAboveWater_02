package com.cpioli.headabovewater;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.cpioli.headabovewater.screens.GameScreen;

public class HeadAboveWater02 extends ApplicationAdapter {
	GameScreen gameScreen;
	
	@Override
	public void create () {
		gameScreen = new GameScreen(this);
		Assets.load();
		gameScreen.show();
	}

	@Override
	public void render () {
		gameScreen.render(Gdx.graphics.getDeltaTime());
	}

	@Override
	public void dispose() {
		gameScreen.dispose();
		Assets.dispose();
	}
}
