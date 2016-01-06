package com.cpioli.headabovewater.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.cpioli.headabovewater.Assets;

import java.util.ArrayList;

public class OxygenMeter extends Group implements OxygenSubject {

	private final float O2RestorationTime = 4.5f;

	private ArrayList<OxygenObserver> observers;
	private Border border;
	private Label meterLabel;
	private StringBuffer labelText;
	public MeshActor meterFill;

	public enum OxygenConsumptionState {EMPTY, DEPLETING, REPLENISHING, FULL}
	public OxygenConsumptionState oxygenBarState;

	private float maxFill;
	private float O2LossDuration = 20.0f;

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
		oxygenBarState = OxygenConsumptionState.FULL;
		maxFill = 94.0f;
		observers = new ArrayList<OxygenObserver>();
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
	public void notifyObservers(OxygenConsumptionState ocs) {
		for(int i = 0; i < observers.size(); i++) {
			observers.get(i).oxygenConsumed(ocs);
		}
		
	}

	public float decreaseOxygenMeter(float deltaTime) {
		float updatedOxygenValue;
		if(oxygenBarState == OxygenConsumptionState.EMPTY) {
			return 0f;
		}
		updatedOxygenValue = meterFill.getWidth() - getMaxFill() / O2LossDuration * deltaTime;
		if(updatedOxygenValue <= 0.0f && oxygenBarState != OxygenConsumptionState.EMPTY) {
			oxygenBarState = OxygenConsumptionState.EMPTY;
			meterFill.setWidth(0.0f);
			notifyObservers(OxygenConsumptionState.EMPTY);
			return 0f;
		}

		if(oxygenBarState == OxygenConsumptionState.FULL) {
			oxygenBarState = OxygenConsumptionState.DEPLETING;
		}
		meterFill.setWidth(updatedOxygenValue);
		float percentRemaining = meterFill.getWidth() / getMaxFill();

		return percentRemaining;
	}

	public float increaseOxygenMeter(float deltaTime, Swimmer swimmer) {
		float updatedOxygenValue = meterFill.getWidth() + getMaxFill() / O2RestorationTime * deltaTime;

		if(updatedOxygenValue >= getMaxFill()) {
			oxygenBarState = OxygenConsumptionState.FULL;
			meterFill.setWidth(getMaxFill());
		} else {
			meterFill.setWidth(updatedOxygenValue);
		}

		float percentRemaining = meterFill.getWidth() / getMaxFill();
		return percentRemaining;
	}

	public float getMaxFill() {
		return maxFill;
	}

	public void reset() {
		meterFill.setWidth(maxFill);
		oxygenBarState = OxygenConsumptionState.FULL;
	}


	public float getO2LossDuration() {
		return O2LossDuration;
	}

	public void setO2LossDuration(float o2LossDuration) {
		O2LossDuration = o2LossDuration;
	}
}