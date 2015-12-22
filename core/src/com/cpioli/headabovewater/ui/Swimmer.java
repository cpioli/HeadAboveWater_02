package com.cpioli.headabovewater.ui;

import java.util.ArrayList;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.cpioli.headabovewater.Assets;
import com.cpioli.headabovewater.screens.GameScreen;
import com.cpioli.headabovewater.utils.GameOverObserver;
import com.cpioli.headabovewater.utils.GameOverSubject;

/*
 * NOTES
 * 
 * 1. remember that the head is nine pixels tall in the 32x32 texture
 * 2. that would mean the head is 15.75 pixels in height. Round it out to 16.
 *
 * RULES ABOUT STAMINA
 * 1) Stamina can't be replenished and consumed at the same time
 * 2) Stamina replenishing is delayed for 5 seconds if the player exhausts it COMPLETELY
 * 3) 16 strokes will consume the entire stamina bar
 * 4) moving forward on the riverbed consumes more stamina than moving forward on the water's surface by a factor of 3
 * 5) Stamina will restore to full in 20 seconds from 0 if you rest at the bottom of the riverbed (AFTER your 5 seconds delay is up)
*/
public class Swimmer implements Disposable, GameOverSubject, OxygenObserver {

	public enum SubmergedState {SWIMMER_ABOVE_WATER, SWIMMER_UNDER_WATER, SWIMMER_ON_RIVERBED};
	public enum StaminaConsumptionState {EMPTY, REPLENISHING, MIDSTROKE, FULL}
	private SubmergedState submergedState;
	private StaminaConsumptionState staminaBarState;

	OrthographicCamera camera;
	OxygenMeter oxygenMeter;
	StaminaMeter staminaMeter;
	ProgressBar progressBar;
	public Texture playerTexture;

	private Sound strokeSound; //either underwater or surface
	private Body body;
	private ArrayList<GameOverObserver> observers;
	private Vector2 location; //in pixels? to replace the Actor class's responsibilities
	private Vector2 viewportLoc;
	private final float staminaExhaustionRecovery = 5.0f;
	private final float staminaDefaultRecovery = 2.3f;
	private final float staminaRestorationTime = 10.0f; //in seconds
	private float staminaRecoveryDelay; //this is a timer. It is used to determine when stamina begins refilling
	private float boundingBoxX = 3.0f;
	private float boundingBoxY = 4.25f;
	private float O2LossDuration = 20.0f; //in seconds (it's shorter for testing purposes)
	private float maxStrokesInFullBar = 16.0f;
	private float levelLength = 250.0f;
	private final float riverSurfaceYVal = -.75f;
	private float pausedYVelocity;

	boolean gamePaused;
	boolean triggeredVO_02, triggeredVO_03, vo_speaking;

	private Task returnVolumeToNormal = new Task() {
		@Override
		public void run() {
			Assets.belowSurfaceAmbience.setVolume(1.0f);
			Assets.aboveSurfaceAmbience.setVolume(1.0f);
			vo_speaking = false;
		}
	};

	public Swimmer(OrthographicCamera camera, OxygenMeter oxygenMeter, StaminaMeter staminaMeter,
				   ProgressBar progressBar, Body body) {
		location = new Vector2(body.getPosition().x, body.getPosition().y); //new Vector2(body.getPosition());
		this.camera = camera;
		viewportLoc = new Vector2(0.0f, 0.0f);
		observers = new ArrayList<GameOverObserver>();
		playerTexture = Assets.playerTexture;
		this.body = body;
		this.oxygenMeter = oxygenMeter;
		this.staminaMeter = staminaMeter;
		this.progressBar = progressBar;
		submergedState = SubmergedState.SWIMMER_UNDER_WATER;
		//oxygenBarState = OxygenConsumptionState.FULL;
		staminaBarState = StaminaConsumptionState.FULL;
		Assets.aboveSurfaceAmbience.setLooping(true);
		Assets.belowSurfaceAmbience.setLooping(true);
		Assets.aboveSurfaceAmbience.play();
		
		gamePaused = false;
		pausedYVelocity = 0.0f;
		triggeredVO_02 = false;
		triggeredVO_03 = false;
		vo_speaking = false;

		oxygenMeter.registerObserver((OxygenObserver)this);

	}
	
	/*
	 * takes in a float deltaTime and calculates the current velocity and position
	 * based on the rules of acceleration and gravity.
	 * Also takes the controls as input (goingLeft, goingRight) and adds velocity to
	 * the Swimmer's Box2D physics Body.
	 */
	public void update(float deltaTime, boolean goingLeft, boolean goingRight) {
		moveSwimmer(goingLeft, goingRight);
		
		//HERE WE WILL UPDATE THE OXYGEN METER
		float updatedOxygenValue;
		float percentRemaining = 0f;
		//TODO: move a lot of this code to the OxygenMeter class! It should be invisible to the Swimmer class!
		//The Swimmer class should determine if the Swimmer loses or gains oxygen based on the submerged State
		//Then the Swimmer class calls the appropriate method in the Oxygen class on every update
		switch(submergedState) {
			case SWIMMER_UNDER_WATER:
				percentRemaining = oxygenMeter.decreaseOxygenMeter(deltaTime);
			break;

			case SWIMMER_ABOVE_WATER:
				if(oxygenMeter.oxygenBarState != OxygenMeter.OxygenConsumptionState.FULL) {
					percentRemaining = oxygenMeter.increaseOxygenMeter(deltaTime, this);
				} else {
					percentRemaining = 1.0f;
				}
				break;

			case SWIMMER_ON_RIVERBED:
				percentRemaining = oxygenMeter.decreaseOxygenMeter(deltaTime);
			break;
		}

		triggerVOResponse(percentRemaining);
		
		float deltaStamina;
		//HERE WE WILL UPDATE THE STAMINA METER
		//TODO: all this should take place in the StaminaMeter class!
		if(staminaBarState == StaminaConsumptionState.EMPTY) {
			//stamina is empty, and we're clocking in time
			staminaRecoveryDelay += deltaTime;
			//if the penalty period has passed, resume stamina replenishment
			if(staminaRecoveryDelay >= staminaExhaustionRecovery) {
				staminaBarState = StaminaConsumptionState.REPLENISHING;
				float remains = staminaRecoveryDelay - staminaExhaustionRecovery;
				staminaMeter.meterFill.setWidth(staminaMeter.getMaxFill() / staminaRestorationTime * remains);
			}
		} else if (staminaBarState == StaminaConsumptionState.MIDSTROKE) { //stamina doesn't start immediately recovering after a swim stroke
			staminaRecoveryDelay += deltaTime;
			if(staminaRecoveryDelay >= this.staminaDefaultRecovery) { //if the delay of stamina recovery has ended
				staminaBarState = StaminaConsumptionState.REPLENISHING;
				float remains = staminaRecoveryDelay - staminaDefaultRecovery;
				deltaStamina = staminaMeter.meterFill.getWidth() + staminaMeter.getMaxFill() / staminaRestorationTime * remains;
				staminaMeter.meterFill.setWidth(deltaStamina);
			}
		} else if (staminaBarState == StaminaConsumptionState.REPLENISHING){
			if(this.submergedState != SubmergedState.SWIMMER_ON_RIVERBED || body.getLinearVelocity().x == 0.0f) {
				deltaStamina = staminaMeter.meterFill.getWidth() + staminaMeter.getMaxFill() / staminaRestorationTime * deltaTime;
				if(deltaStamina >= staminaMeter.getMaxFill()) {
					staminaBarState = StaminaConsumptionState.FULL;
					staminaMeter.meterFill.setWidth(staminaMeter.getMaxFill());
				} else {
					staminaMeter.meterFill.setWidth(deltaStamina);
				}
			}
			
		}
		
	}

	private void moveSwimmer(boolean goingLeft, boolean goingRight) {
		//calculating swimmer's movement along the x axis
		if(submergedState != SubmergedState.SWIMMER_ON_RIVERBED) {
			if(goingLeft && goingRight) {
				body.setLinearVelocity(0.0f, body.getLinearVelocity().y);
			} else if(goingRight) {
				body.setLinearVelocity(2.0f, body.getLinearVelocity().y);
			} else if(goingLeft) {//it reaches the first condition, but not the second. Hmmm...
				body.setLinearVelocity(-2.0f, body.getLinearVelocity().y);
			}
		} else {
			if(goingLeft && goingRight) {
				body.setLinearVelocity(0.0f, body.getLinearVelocity().y);
			} else if(goingRight) {
				body.setLinearVelocity(1.0f, body.getLinearVelocity().y);
			} else if(goingLeft) {//it reaches the first condition, but not the second. Hmmm...
				body.setLinearVelocity(-1.0f, body.getLinearVelocity().y);
			}
		}
		if(staminaBarState == StaminaConsumptionState.EMPTY){
			body.setLinearVelocity(0.0f, body.getLinearVelocity().y);
		}
	}

	/*private void updateOxygenMeter(float deltaTime) {
		float updatedOxygenValue;
		if(oxygenMeter.oxygenBarState == oxygenMeter.OxygenConsumptionState.EMPTY) {
			return;
		}
		updatedOxygenValue = oxygenMeter.meterFill.getWidth() - oxygenMeter.getMaxFill() / O2LossDuration * deltaTime;
		float percentRemaining = oxygenMeter.meterFill.getWidth() / oxygenMeter.getMaxFill();
		if(updatedOxygenValue <= 0.0f && oxygenMeter.oxygenBarState != oxygenMeter.OxygenConsumptionState.EMPTY) {
			oxygenMeter.oxygenBarState = oxygenMeter.OxygenConsumptionState.EMPTY;
			oxygenMeter.meterFill.setWidth(0.0f);
			//notify of death
			notifyObservers(GameScreen.GAME_DYING);
			return;
		}

		if(oxygenMeter.oxygenBarState == oxygenMeter.OxygenConsumptionState.FULL) {
			oxygenMeter.oxygenBarState = oxygenMeter.OxygenConsumptionState.DEPLETING;
		}
		oxygenMeter.meterFill.setWidth(updatedOxygenValue);
		triggerVOResponse(percentRemaining);
		return;
	}*/

	private void triggerVOResponse(float percentRemaining) {
		if(percentRemaining <= 0.5f && !triggeredVO_02) {
			Assets.belowSurfaceAmbience.setVolume(0.2f);
			Assets.aboveSurfaceAmbience.setVolume(0.2f);
			Assets.swimmerVO_02.play();
			vo_speaking = true;
			new Timer().scheduleTask(returnVolumeToNormal, 2.608f);
			triggeredVO_02 = true;
		} else if(percentRemaining <= 0.1f && !triggeredVO_03) {
			Assets.belowSurfaceAmbience.setVolume(0.2f);
			Assets.aboveSurfaceAmbience.setVolume(0.2f);
			Assets.swimmerVO_03.play();
			vo_speaking = true;
			triggeredVO_03 = true;
			new Timer().scheduleTask(returnVolumeToNormal, 1.506f);

		}

		//if we've gotten to that point
		if(percentRemaining > 0.8f) {
			if (triggeredVO_02) {
				triggeredVO_02 = false;
			}
			if (triggeredVO_03) {
				triggeredVO_03 = false;
			}
		}
		return;
	}

	public void setPosition(float x, float y) {
		if(gamePaused) {
			return;
		}

		float dx = x - location.x;
		float dy = y - location.y;
		
		float viewportX = x - camera.position.x;
		float viewportY = y - camera.position.y;
		

		if(dx > 0.0f) {
			if(viewportX >= boundingBoxX) { //if we've hit the viewport
				camera.translate(dx, 0.0f);
			}
		} else if(dx < 0.0f) {
			if(viewportX <= -boundingBoxX) { //if we've hit the viewport's left side
				camera.translate(dx, 0.0f);
			}
		}
		
		if(dy > 0.0f) {
			if(viewportY >= boundingBoxY) {
				camera.translate(0.0f, dy);
			}
		} else if (dy < 0.0f) {
			if(viewportY <= -boundingBoxY) {
				camera.translate(0.0f, dy);
			}
		}
		location.x = x;
		location.y = y;
		
		// in this space, determine if the swimmer's head is above water or underwater
		if(location.y >= riverSurfaceYVal && submergedState == SubmergedState.SWIMMER_UNDER_WATER) { //we're above water
			submergedState = SubmergedState.SWIMMER_ABOVE_WATER;
			strokeSound = Assets.surfaceStroke;
			Assets.belowSurfaceAmbience.pause();
			Assets.aboveSurfaceAmbience.play();
			if(oxygenMeter.meterFill.getWidth() <= oxygenMeter.getMaxFill() / 2.0f) {
				Assets.gasp.play();
			}
		} else if (location.y < riverSurfaceYVal && submergedState == SubmergedState.SWIMMER_ABOVE_WATER) {
			submergedState = SubmergedState.SWIMMER_UNDER_WATER;
			Assets.aboveSurfaceAmbience.pause();
			Assets.belowSurfaceAmbience.play();
			strokeSound = Assets.underwaterStroke;
		}
		float newProgressPoint = progressBar.getPlayer().getX() + (dx / levelLength) * progressBar.finalLocation;
		progressBar.getPlayer().setX(newProgressPoint);
		
		if(location.x >= 250.0f && location.y >= 0.0f) {
			notifyObservers(GameScreen.GAME_FINISHED_SUCCESS);
		}
	}

	public void performStroke() {
		if(staminaBarState != StaminaConsumptionState.EMPTY && location.x < 250.0f) {
			strokeSound.stop(); //if you switched from under to above or vice-versa, the other one might not stop
			long id = strokeSound.play();
			if(!vo_speaking) {
				strokeSound.setVolume(id, 1.2f);
			} else {
				strokeSound.setVolume(id, 0.4f);
			}
			staminaRecoveryDelay = 0.0f;
			if(staminaMeter.meterFill.getWidth() <= (staminaMeter.getMaxFill() / maxStrokesInFullBar)) { //we'll be emptying our stamina
				staminaMeter.meterFill.setWidth(0.0f);
				staminaBarState = StaminaConsumptionState.EMPTY;
			} else { //consume another portion of stamina
				body.setLinearVelocity(body.getLinearVelocity().x, 2.5f);
				staminaMeter.meterFill.setWidth(staminaMeter.meterFill.getWidth() - staminaMeter.getMaxFill() / maxStrokesInFullBar);
				staminaBarState = StaminaConsumptionState.MIDSTROKE;
				staminaRecoveryDelay = 0.0f;//we just made a stroke, the timer is reset
			}
			//no longer on riverbed
			if(submergedState == SubmergedState.SWIMMER_ON_RIVERBED) {
				submergedState = SubmergedState.SWIMMER_UNDER_WATER;
			}
		}
	}
	
	@Override
	public void dispose() {
	}

	public Vector2 getLocation() {
		return location;
	}

	public void setLocation(Vector2 location) {
		this.location = location;
	}

	@Override
	public void registerObserver(GameOverObserver goo) {
		observers.add(goo);
	}

	@Override
	public void removeObserver(GameOverObserver goo) {
		int i = observers.indexOf(goo);
		if(i>= 0) {
			observers.remove(i);
		}
	}


	
	public void pause() {
		gamePaused = true;
		pausedYVelocity = body.linVelLoc.y;
		body.setLinearVelocity(0.0f, 0.0f);
	}
	
	public void resume(boolean goingLeft, boolean goingRight) {
		gamePaused = false;
		body.linVelLoc.y = pausedYVelocity;
		update(0.0f, goingLeft, goingRight);
	}
	
	public void restart() {
		float xMovement = camera.position.x;
		float yMovement = camera.position.y;
		camera.translate(-xMovement + viewportLoc.x, -yMovement - viewportLoc.y);
		this.location.x = 0.0f;
		this.location.y = 0.0f;
		this.body.setTransform(new Vector2(0.0f, 0.0f), 0.0f);
		System.out.println(body.getPosition());
		System.out.println(body.getLinearVelocity());

		camera.update();
		body.setLinearVelocity(0.0f, 0.1f);
		gamePaused = false;
		submergedState = SubmergedState.SWIMMER_ABOVE_WATER;
		//oxygenBarState = OxygenConsumptionState.FULL;
		oxygenMeter.reset();
		staminaBarState = StaminaConsumptionState.FULL;
		Assets.aboveSurfaceAmbience.setVolume(1.0f);
		Assets.belowSurfaceAmbience.setVolume(1.0f);
		Assets.aboveSurfaceAmbience.play();
		Assets.aboveSurfaceAmbience.setLooping(true);
		Assets.belowSurfaceAmbience.setLooping(true);
		triggeredVO_02 = false;
		triggeredVO_03 = false;
		this.progressBar.reset();
	}

	//cheat code functions
	public float getMaxStrokesInFullBar() {
		return maxStrokesInFullBar;
	}

	public void setMaxStrokesInFullBar(float maxStrokesInFullBar) {
		this.maxStrokesInFullBar = maxStrokesInFullBar;
	}
	
	public float getO2LossDuration() {
		return O2LossDuration;
	}

	public void setO2LossDuration(float o2LossDuration) {
		O2LossDuration = o2LossDuration;
	}

	public void setSubmergedState(SubmergedState state) {
		this.submergedState = state;
	}

	public void oxygenConsumed(OxygenMeter.OxygenConsumptionState ocs) {
		if(ocs == OxygenMeter.OxygenConsumptionState.EMPTY) {
			notifyObservers(GameScreen.GAME_DYING);
		}
	}

	@Override
	public void notifyObservers(int gameState) {
		for(int i = 0; i < observers.size();i++) {
			GameOverObserver goo = observers.get(i);
			goo.updateGameOverObserver(gameState);
		}

	}
}