package com.cpioli.headabovewater.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.cpioli.headabovewater.Assets;

import java.util.ArrayList;

public class OxygenMeter extends Group implements SubmergedObserver, OxygenSubject {
	
	
	private ArrayList<OxygenObserver> observers;
	
	float x, y;
	
	private Border border;
	public MeshActor meterFill;
	private Label meterLabel;
	
	private StringBuffer labelText;
	private float maxFill;

	public OxygenMeter(ShapeRenderer renderer, float x, float y) {
		super.setX(x);
		super.setY(y);
		border = new Border(3.0f, x, y, 100.0f, 25.0f, Color.BLACK, renderer);
		meterFill = new MeshActor(x + 3.0f, y + 3.0f, 94.0f, 19.0f, Color.RED, "02MeterFill", renderer);
		labelText = new StringBuffer();
		labelText.append("Oxygen");
		//makeborderthickness
		meterLabel = Assets.oxygenMeterLabel;
		meterLabel.setAlignment(Align.right);
		meterLabel.setText(labelText);
		meterLabel.setPosition(100.0f, 0.0f);//lower_right hand side of the meter
		
		this.addActor(meterFill);
		this.addActor(border);
		this.addActor(meterLabel);
		
		//oxygenState = O2_FULL;
		maxFill = 94.0f;
	}
	
	@Override
	public void registerObserver(OxygenObserver oo) {
		// TODO Auto-generated method stub
		observers.add(oo);
	}

	@Override
	public void removeObserver(OxygenObserver oo) {
		int i = observers.indexOf(oo);
		if(i >= 0) {
			observers.remove(i);
		}
	}

	@Override
	public void notifyObservers() {
		
		
	}

	@Override
	public void update(int submergedStatus) {
		
	}
	
	
	public float getMaxFill() {
		return maxFill;
	}

	public void setMaxFill(float maxFill) {
		this.maxFill = maxFill;
	}
	//when submerged, the oxygen meter slowly depletes
	//when
	
	//TODO: IMPLEMENT RESET OXYGEN METER
	public void restart() {
		meterFill.setWidth(maxFill);
	}
}