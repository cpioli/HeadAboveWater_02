package com.cpioli.headabovewater.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.cpioli.headabovewater.Assets;

public class ProgressBar extends Group {
	private MeshActor pathBar; //the full length of the level represented as a line
	final private TextureRegion playerIcon; //the location of the player on the screen
	private Actor player;
	ShapeRenderer renderer;
	float height; //the height of the player icon
	
	public float finalLocation = 200.0f; //the location of the icon when the level is complete
	float currentLocation;
	float startingLocation = 364.0f;
	
	public ProgressBar(float x, float y, float height, ShapeRenderer renderer) {
		super.setX(x);
		super.setY(y);
		
		playerIcon = new TextureRegion(Assets.playerSide);
		player = new Actor() {
			@Override
			public void draw(Batch batch, float parentAlpha) {
				batch.draw(playerIcon, getX(), getY(), getOriginX(), getOriginY(), getWidth(), getHeight(), getScaleX(), getScaleY(),
						getRotation());
			}
		};
		player.setBounds(364.0f, 600.0f, 32.0f, 32.0f);

		currentLocation = 0.0f;
		pathBar = new MeshActor(380.0f, 600.0f, finalLocation, 3.0f, Color.OLIVE, "pathBar", renderer);
		this.renderer = renderer;
		this.addActor(pathBar);
		this.addActor(player);
	}
	
	public Actor getPlayer() {
		return player;
	}

	public void setPlayer(Actor player) {
		this.player = player;
	}

	public void reset() {
		//set player back to his original position
		player.setPosition(startingLocation, player.getY());
	}
}