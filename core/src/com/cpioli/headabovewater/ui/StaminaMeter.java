package com.cpioli.headabovewater.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.cpioli.headabovewater.Assets;

public class StaminaMeter extends Group {

	private final float staminaExhaustionRecovery = 5.0f;
	private final float staminaDefaultRecovery = 2.3f;
	private final float staminaRestorationTime = 10.0f; //in seconds
	private Swimmer swimmer;
	private Border border;
	public MeshActor meterFill;
	private Label meterLabel;

	public enum StaminaConsumptionState {EMPTY, REPLENISHING, MIDSTROKE, FULL}
	public StaminaConsumptionState staminaBarState;

	private float staminaRecoveryDelay; //this is a timer. It is used to determine when stamina begins refilling
	private float maxStrokesInFullBar = 16.0f;
	private float maxFill;
	private StringBuffer labelText;

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

	public void increaseStaminaBar(float deltaTime) {
		float newStaminaValue = 0f;
		if(staminaBarState == StaminaMeter.StaminaConsumptionState.EMPTY) {
			//stamina is empty, and we're clocking in time
			staminaRecoveryDelay += deltaTime;
			//if the penalty period has passed, resume stamina replenishment
			if(staminaRecoveryDelay >= staminaExhaustionRecovery) {
				staminaBarState = StaminaMeter.StaminaConsumptionState.REPLENISHING;
				float remains = staminaRecoveryDelay - staminaExhaustionRecovery;
				meterFill.setWidth(getMaxFill() / staminaRestorationTime * remains);
			}
		} else if (staminaBarState == StaminaMeter.StaminaConsumptionState.MIDSTROKE) { //stamina doesn't start immediately recovering after a swim stroke
			staminaRecoveryDelay += deltaTime;
			if(staminaRecoveryDelay >= this.staminaDefaultRecovery) { //if the delay of stamina recovery has ended
				staminaBarState = StaminaMeter.StaminaConsumptionState.REPLENISHING;
				float remains = staminaRecoveryDelay - staminaDefaultRecovery;
				newStaminaValue = meterFill.getWidth() + getMaxFill() / staminaRestorationTime * remains;
				meterFill.setWidth(newStaminaValue);
			}
		} else if (staminaBarState == StaminaMeter.StaminaConsumptionState.REPLENISHING) {
			//check to make sure Player is NOT walking on the riverbed
			if(swimmer.getSubmergedState() != Swimmer.SubmergedState.SWIMMER_ON_RIVERBED || swimmer.getSwimmerPhysicsBody().getLinearVelocity().x == 0.0f) {
				newStaminaValue = meterFill.getWidth() + getMaxFill() / staminaRestorationTime * deltaTime;
				if(newStaminaValue >= getMaxFill()) {
					staminaBarState = StaminaMeter.StaminaConsumptionState.FULL;
					meterFill.setWidth(getMaxFill());
				} else {
					meterFill.setWidth(newStaminaValue);
				}
			}

		}
	}

	//if the swimmer has enough stamina to execute a swim stroke, returns true
	//else, returns false
	public boolean decrementStaminaBar() {
		staminaRecoveryDelay = 0.0f;
		if(meterFill.getWidth() <= (getMaxFill() / maxStrokesInFullBar)) { //we'll be emptying our stamina
			meterFill.setWidth(0.0f);
			staminaBarState = StaminaConsumptionState.EMPTY;
			return false;
			//TODO: indicate stamina was exhausted to the player by creating a "struggle" animation
		} else { //consume another portion of stamina
			meterFill.setWidth(meterFill.getWidth() - getMaxFill() / maxStrokesInFullBar);
			staminaBarState = StaminaConsumptionState.MIDSTROKE;
			staminaRecoveryDelay = 0.0f;//we just made a stroke, the timer is reset
			return true;
		}
	}

	public float getMaxFill() {
		return maxFill;
	}

	public void setMaxFill(float maxFill) {
		this.maxFill = maxFill;
	}
	
	public void reset() {
		staminaBarState = StaminaConsumptionState.FULL;
		meterFill.setWidth(maxFill);
	}

	public void setSwimmer(Swimmer swimmer) {
		this.swimmer = swimmer;
	}
}