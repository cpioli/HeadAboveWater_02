package com.cpioli.headabovewater.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.cpioli.headabovewater.Assets;

public class StaminaMeter extends Group {
	
	private Border border;
	public MeshActor meterFill;
	private Label meterLabel;
	
	private StringBuffer labelText;
	
	private float maxFill;

	public StaminaMeter(ShapeRenderer renderer, float x, float y) {
		super.setX(x);
		super.setY(y);
		border = new Border(3.0f, x, y, 100.0f, 25.0f, Color.BLACK, renderer);
		meterFill = new MeshActor(x + 3.0f, y + 3.0f, 94.0f, 19.0f, Color.BLUE, "StaminaMeterFill", renderer);
		labelText = new StringBuffer();
		labelText.append("Stamina");
		meterLabel = Assets.staminaMeterLabel;
		meterLabel.setAlignment(Align.right);
		meterLabel.setText(labelText);
		meterLabel.setPosition(100.0f, 0.0f);
		
		this.addActor(meterFill);
		this.addActor(border);
		this.addActor(meterLabel);
		
		maxFill = 94.0f;
	}
	
	public float getMaxFill() {
		return maxFill;
	}

	public void setMaxFill(float maxFill) {
		this.maxFill = maxFill;
	}
	
	public void restart() {
		meterFill.setWidth(maxFill);
	}
}