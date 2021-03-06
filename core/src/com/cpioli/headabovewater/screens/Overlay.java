package com.cpioli.headabovewater.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.cpioli.headabovewater.Assets;
import com.cpioli.headabovewater.ui.Border;
import com.cpioli.headabovewater.ui.MeshActor;

public class Overlay extends Group {
	private Label title;
	private MeshActor background;
	private Border whiteBorder1;
	private Border whiteBorder2;
	private static float TWEEN_DURATION = 0.5f;
	private static Interpolation TWEEN_INTERPOLATION = Interpolation.exp10Out;
	private float screenX, screenY;
	private ShapeRenderer renderer;
	
	public Overlay(float x, float y, float width, float height, String title, Color backgroundColor, ShapeRenderer renderer) {
		screenX = x;
		screenY = y;
		background = new MeshActor(x, y, width, height, backgroundColor, title, renderer);
		whiteBorder1 = new Border(2.0f, x, y, width, height, Color.WHITE, renderer);
		whiteBorder2 = new Border(2.0f, x+4, y+4.0f, width - 8.0f, height - 8.0f, Color.WHITE, renderer);
		this.title = new Label(title, new LabelStyle(Assets.myriadPro48, Color.WHITE));
		this.title.setPosition(x + 140.0f, y + 160.0f);
		this.addActor(background);
		this.addActor(whiteBorder1);
		this.addActor(whiteBorder2);
		this.addActor(this.title);
		
	}
}