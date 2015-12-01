package com.cpioli.headabovewater.actors;

import java.util.ArrayList;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.cpioli.headabovewater.Assets;
import com.cpioli.headabovewater.screens.GameScreen;
import com.cpioli.headabovewater.ui.OxygenMeter;
import com.cpioli.headabovewater.ui.ProgressBar;
import com.cpioli.headabovewater.ui.StaminaMeter;
import com.cpioli.headabovewater.utils.GameOverObserver;
import com.cpioli.headabovewater.utils.GameOverSubject;

/*
 * NOTES
 * 
 * 1. remember that the head is nine pixels tall in the 32x32 texture
 * 2. that would mean the head is 15.75 pixels in height. Round it out to 16.
 */
public class Swimmer implements Disposable, GameOverSubject {

	public enum SubmergedState {SWIMMER_ABOVE_WATER, SWIMMER_UNDER_WATER, SWIMMER_ON_RIVERBED};
	private SubmergedState submergedState;

	public enum OxygenConsumptionState {EMPTY, DEPLETING, REPLENISHING, FULL}
	private OxygenConsumptionState oxygenBarState;
	private float O2LossDuration = 20.0f; //in seconds (it's shorter for testing purposes)
	private float O2RestorationTime = 4.5f;

	public enum StaminaConsumptionState {EMPTY, REPLENISHING, MIDSTROKE, FULL}
	private StaminaConsumptionState staminaBarState;

	//number of strokes one can accomplish with a full stamina bar
	private float strokesPerBar = 16.0f;


	private float staminaLoss_MoveRiverbed = 3.0f;
	private float staminaRestorationTime = 10.0f; //in seconds
	float levelLength = 250.0f;
	
	private float timeBetweenStroke; //this is a timer. It is used to determine when stamina begins refilling
	private float staminaExhaustionRecovery = 5.0f;
	private float staminaDefaultRecovery = 2.0f;
	/*
	 * RULES ABOUT STAMINA
	 * 1) Stamina can't be replenished and consumed at the same time
	 * 2) Stamina replenishing is delayed for 5 seconds if the player exhausts it COMPLETELY
	 * 3) X amount of strokes will consume the entire stamina bar
	 * 4) moving forward on the riverbed consumes more stamina than moving forward on the water's surface by a factor of 3
	 * 5) Stamina will restore to full in 20 seconds from 0 if you rest at the bottom of the riverbed (AFTER your 5 seconds delay is up)
	 */
	//int submergedState;
	//int oxygenState;
	//int staminaState;
	public boolean onRiverbed;
	
	boolean gamePaused;
	float pausedYVelocity;
	
	private ArrayList<GameOverObserver> observers;
	private Vector2 location; //in pixels? to replace the Actor class's responsibilities
	public Vector2 viewportLoc;
	private float boundingBoxX = 3.0f;
	private float boundingBoxY = 4.25f;
	public Vector2 waterGravity; //it'll be hard to find the right gravity

	boolean triggeredVO_02, triggeredVO_03, vo_speaking;

	private Sound strokeSound; //either underwater or surface
	
	private Body body;
	private Fixture fixture;
	
	OrthographicCamera camera;
	OxygenMeter oxygenMeter;
	StaminaMeter staminaMeter;
	ProgressBar progressBar;
	public Texture playerTexture;
	
	public Swimmer(Texture texture, OrthographicCamera camera,
			OxygenMeter oxygenMeter, StaminaMeter staminaMeter, ProgressBar
			progressBar, Body body, float startingX, float startingY) {
		//super();
		waterGravity = new Vector2(0.0f, 2.6f); //this is a very rough guess
		location = new Vector2(body.getPosition().x, body.getPosition().y); //new Vector2(body.getPosition());
		this.camera = camera;
		viewportLoc = new Vector2(0.0f, 0.0f);
		
		
		//observers = new ArrayList<SubmergedObserver>();
		observers = new ArrayList<GameOverObserver>();
		this.playerTexture = texture;
		this.body = body;
		this.oxygenMeter = oxygenMeter;
		this.staminaMeter = staminaMeter;
		this.progressBar = progressBar;
		//submergedState = SWIMMER_UNDER_WATER;
		submergedState = SubmergedState.SWIMMER_UNDER_WATER;
		oxygenBarState = OxygenConsumptionState.FULL;
		staminaBarState = StaminaConsumptionState.FULL;
		Assets.aboveSurfaceAmbience.setLooping(true);
		Assets.belowSurfaceAmbience.setLooping(true);
		//Assets.belowSurfaceAmbience.setVolume(0.25f);
		Assets.aboveSurfaceAmbience.play();
		
		gamePaused = false;
		pausedYVelocity = 0.0f;
		onRiverbed = false;
		triggeredVO_02 = false;
		triggeredVO_03 = false;
		vo_speaking = false;
		
	}
	
	/*
	 * takes in a float deltaTime and calculates the current velocity and position
	 * based on the rules of acceleration and gravity.
	 * This takes the controls as input (goingLeft, goingRight) and adds velocity to
	 * the Swimmer's Box2D physics Body.
	 */
	public void update(float deltaTime, boolean goingLeft, boolean goingRight) {
		//this.setX(this.getX() + velocity.x);
		//if the player is making a swim stroke, this is where the player's Action will take place
		//myMoveByAction.updateRelative(float percentDelta)
		//position.y += myMoveByAction.getAmountY();
		
		//calculating swimmer's movement along the x axis
		if(!onRiverbed) {
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
		
		//HERE WE WILL UPDATE THE OXYGEN METER
		float deltaO2;
		switch(submergedState) {
		case SWIMMER_UNDER_WATER:
			if(oxygenBarState != OxygenConsumptionState.EMPTY){
				deltaO2 = oxygenMeter.meterFill.getWidth() - oxygenMeter.getMaxFill() / O2LossDuration * deltaTime;
				float percentRemaining = oxygenMeter.meterFill.getWidth() / oxygenMeter.getMaxFill();
				//System.out.println(deltaO2);
				if(deltaO2 <= 0.0f) {
					oxygenBarState = OxygenConsumptionState.EMPTY;
					oxygenMeter.meterFill.setWidth(0.0f);
					//notify of death
					notifyObservers(GameScreen.GAME_DYING);
				} else if(percentRemaining <= 0.5f && !triggeredVO_02) {
					Assets.belowSurfaceAmbience.setVolume(0.2f);
					Assets.aboveSurfaceAmbience.setVolume(0.2f);
					Assets.swimmerVO_02.play();
					vo_speaking = true;
					new Timer().scheduleTask(new Task() {
						@Override
						public void run() {
							Assets.belowSurfaceAmbience.setVolume(1.0f);
							Assets.aboveSurfaceAmbience.setVolume(1.0f);
							vo_speaking = false;
						}
					}, 2.608f);
					/*new action that returns ambience music back to volume of 1.0*/
					//lines 191, 192, and 194 must be done for the swimmer 03 asset.
					triggeredVO_02 = true;
				} else if(percentRemaining <= 0.1f && !triggeredVO_03) {
					Assets.belowSurfaceAmbience.setVolume(0.2f);
					Assets.aboveSurfaceAmbience.setVolume(0.2f);
					Assets.swimmerVO_03.play();
					vo_speaking = true;
					triggeredVO_03 = true;
					new Timer().scheduleTask(new Task() {
						@Override
						public void run() {
							Assets.belowSurfaceAmbience.setVolume(1.0f);
							Assets.aboveSurfaceAmbience.setVolume(1.0f);
							vo_speaking = false;
						}
					}, 1.506f);
				} else {
					if(oxygenBarState == OxygenConsumptionState.FULL) {
						oxygenBarState = OxygenConsumptionState.DEPLETING;
					}
					oxygenMeter.meterFill.setWidth(deltaO2);
				}
			}
		break;
			
		case SWIMMER_ABOVE_WATER:
			//System.out.println("We should be restoring oxygen right about now!");
			if(oxygenBarState != OxygenConsumptionState.FULL) {
				deltaO2 = oxygenMeter.meterFill.getWidth() + oxygenMeter.getMaxFill() / O2RestorationTime * deltaTime;
				//System.out.println("deltaO2: " + deltaO2);
				if(deltaO2 >= oxygenMeter.getMaxFill()) {
					oxygenBarState = OxygenConsumptionState.FULL;
					oxygenMeter.meterFill.setWidth(oxygenMeter.getMaxFill());
					triggeredVO_02 = false;
					triggeredVO_03 = false;
				} else {
					oxygenMeter.meterFill.setWidth(deltaO2);
				}
			}
		break;
		}
		
		
		float deltaStamina;
		//HERE WE WILL UPDATE THE STAMINA METER
		if(staminaBarState == StaminaConsumptionState.EMPTY) {
			//stamina is empty, and we're clocking in time
			timeBetweenStroke += deltaTime;
			//if the penalty period has passed, resume stamina replenishment
			if(this.timeBetweenStroke >= this.staminaExhaustionRecovery) {
				staminaBarState = StaminaConsumptionState.REPLENISHING;
				float remains = timeBetweenStroke - staminaExhaustionRecovery;
				staminaMeter.meterFill.setWidth(staminaMeter.getMaxFill() / staminaRestorationTime * remains);
			}
		} else if (staminaBarState == StaminaConsumptionState.MIDSTROKE) { //stamina doesn't start immediately recovering after a swim stroke
			timeBetweenStroke += deltaTime;
			if(timeBetweenStroke >= this.staminaDefaultRecovery) {
				staminaBarState = StaminaConsumptionState.REPLENISHING;
				float remains = timeBetweenStroke - staminaDefaultRecovery;
				deltaStamina = staminaMeter.meterFill.getWidth() + staminaMeter.getMaxFill() / staminaRestorationTime * remains;
				staminaMeter.meterFill.setWidth(deltaStamina);
			}
		} else if (staminaBarState == StaminaConsumptionState.REPLENISHING){
			if(!onRiverbed || body.getLinearVelocity().x == 0.0f) {
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
	
	public void setPosition(float x, float y) {
		//1) perform bounding box hit detection
		//2) perform movement
		
		float dx = x - location.x;
		float dy = y - location.y;
		
		float viewportX = x - camera.position.x;
		float viewportY = y - camera.position.y;
		
		//System.out.println(x + " - " + location.x + " = " + dx);
		//System.out.println(viewportX);
		
		if(gamePaused) {
			return;
		}
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
		if(location.y >= -0.75f && submergedState == SubmergedState.SWIMMER_UNDER_WATER) { //we're above water
			submergedState = SubmergedState.SWIMMER_ABOVE_WATER;
			System.out.println("Above");
			strokeSound = Assets.surfaceStroke;
			Assets.belowSurfaceAmbience.pause();
			Assets.aboveSurfaceAmbience.play();
			if(oxygenMeter.meterFill.getWidth() <= oxygenMeter.getMaxFill() / 2.0f) {
				Assets.gasp.play();
			}
			//notifyObservers(submergedState); in lieu of this I have to make manual changes
			//play a sound here
		} else if (location.y < -0.75f && submergedState == SubmergedState.SWIMMER_ABOVE_WATER) {
			submergedState = SubmergedState.SWIMMER_UNDER_WATER;
			Assets.aboveSurfaceAmbience.pause();
			Assets.belowSurfaceAmbience.play();
			System.out.println("Below");
			strokeSound = Assets.underwaterStroke;
			//notifyObservers(submergedState);
			//play a sound here
		}
		//System.out.println("Location: " + location.x + ", " + location.y);
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
			timeBetweenStroke = 0.0f;
			if(staminaMeter.meterFill.getWidth() <= (staminaMeter.getMaxFill() / strokesPerBar)) { //we'll be emptying our stamina
				staminaMeter.meterFill.setWidth(0.0f);
				staminaBarState = StaminaConsumptionState.EMPTY;
			} else { //consume another portion of stamina
				body.setLinearVelocity(body.getLinearVelocity().x, 2.5f);
				//consume stamina
				staminaMeter.meterFill.setWidth(staminaMeter.meterFill.getWidth() - staminaMeter.getMaxFill() / strokesPerBar);
				staminaBarState = StaminaConsumptionState.MIDSTROKE;
				timeBetweenStroke = 0.0f;//we just made a stroke, the timer is reset
			}
			//no longer on riverbed
			if(submergedState == SubmergedState.SWIMMER_ON_RIVERBED) {
				submergedState = SubmergedState.SWIMMER_UNDER_WATER;
			}
			onRiverbed = false;
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

	@Override
	public void notifyObservers(int gameState) {
		for(int i = 0; i < observers.size();i++) {
			GameOverObserver goo = observers.get(i);
			goo.update(gameState);
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
		camera.update();
		body.setLinearVelocity(0.0f, 0.1f);
		gamePaused = false;
		submergedState = SubmergedState.SWIMMER_ABOVE_WATER;
		oxygenBarState = OxygenConsumptionState.FULL;
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
	public float getStrokesPerBar() {
		return strokesPerBar;
	}

	public void setStrokesPerBar(float strokesPerBar) {
		this.strokesPerBar = strokesPerBar;
	}
	
	public float getO2LossDuration() {
		return O2LossDuration;
	}

	public void setO2LossDuration(float o2LossDuration) {
		O2LossDuration = o2LossDuration;
	}
}