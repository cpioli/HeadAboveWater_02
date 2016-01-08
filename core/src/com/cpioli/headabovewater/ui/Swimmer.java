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
public class Swimmer implements Disposable, GameOverSubject, OrientationSubject, SubmergedSubject, OxygenObserver {

	public enum SubmergedState {SWIMMER_ABOVE_WATER, SWIMMER_UNDER_WATER, SWIMMER_ON_RIVERBED};
	private SubmergedState submergedState;
	public enum OrientationState {LEFT, RIGHT};
	private OrientationState orientationState;

	OrthographicCamera camera;
	OxygenMeter oxygenMeter;
	StaminaMeter staminaMeter;
	ProgressBar progressBar;
	public Texture playerTexture;

	private Sound strokeSound; //either underwater or surface
	private Body body;
	private ArrayList<GameOverObserver> gameOverObservers;
	private ArrayList<SubmergedObserver> submergedObservers;
	private ArrayList<OrientationObserver> orientationObservers;
	private Vector2 location; //in pixels? to replace the Actor class's responsibilities
	private Vector2 viewportLoc;
	private float boundingBoxX = 3.0f;
	private float boundingBoxY = 4.25f;
	private float maxStrokesInFullBar = 16.0f; //used for cheat-code functionality. should move this over to Stamina Meter in the future
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
		location = new Vector2(body.getPosition().x, body.getPosition().y);
		this.camera = camera;
		viewportLoc = new Vector2(0.0f, 0.0f);
		gameOverObservers = new ArrayList<GameOverObserver>();
		submergedObservers = new ArrayList<SubmergedObserver>();
		orientationObservers = new ArrayList<OrientationObserver>();
		//TODO: replace Swimmer.playerTexture with the Animation object we're making
		playerTexture = Assets.playerTexture;
		this.body = body;
		this.oxygenMeter = oxygenMeter;
		this.staminaMeter = staminaMeter;
		this.progressBar = progressBar;
		submergedState = SubmergedState.SWIMMER_UNDER_WATER;
		//staminaBarState = StaminaConsumptionState.FULL;
		Assets.aboveSurfaceAmbience.setLooping(true);
		Assets.belowSurfaceAmbience.setLooping(true);
		Assets.aboveSurfaceAmbience.play();
		
		gamePaused = false;
		pausedYVelocity = 0.0f;
		triggeredVO_02 = false;
		triggeredVO_03 = false;
		vo_speaking = false;

		oxygenMeter.registerObserver(this);

	}
	
	/*
	 * takes in a float deltaTime and calculates the current velocity and position
	 * based on the rules of acceleration and gravity.
	 * Also takes the controls as input (goingLeft, goingRight) and adds velocity to
	 * the Swimmer's Box2D physics Body.
	 */
	public void update(float deltaTime, boolean goingLeft, boolean goingRight) {
		moveSwimmer(goingLeft, goingRight);
		
		float percentRemaining = 0f;
		switch(submergedState) {
			case SWIMMER_UNDER_WATER:
				percentRemaining = oxygenMeter.decreaseOxygenMeter(deltaTime);
			break;

			case SWIMMER_ABOVE_WATER:
				if(oxygenMeter.oxygenBarState != OxygenMeter.OxygenConsumptionState.FULL) {
					percentRemaining = oxygenMeter.increaseOxygenMeter(deltaTime, this);
				} else { //if the oxygenMeter is full
					percentRemaining = 1.0f;
				}
				break;

			case SWIMMER_ON_RIVERBED:
				percentRemaining = oxygenMeter.decreaseOxygenMeter(deltaTime);
			break;
		}

		triggerVOResponse(percentRemaining);
		staminaMeter.increaseStaminaBar(deltaTime);
		
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
		if(staminaMeter.staminaBarState == StaminaMeter.StaminaConsumptionState.EMPTY){
			body.setLinearVelocity(0.0f, body.getLinearVelocity().y);
		}
	}

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

		if(staminaMeter.staminaBarState == StaminaMeter.StaminaConsumptionState.EMPTY || location.x > 250.0f) {
			return;
		}
		strokeSound.stop(); //if you switched from under to above or vice-versa, the other one might not stop
		long id = strokeSound.play();
		if(!vo_speaking) {
			strokeSound.setVolume(id, 1.2f);
		} else {
			strokeSound.setVolume(id, 0.4f);
		}
		if(staminaMeter.decrementStaminaBar()) {
			body.setLinearVelocity(body.getLinearVelocity().x, 2.5f);
		}
		//no longer on riverbed
		if(submergedState == SubmergedState.SWIMMER_ON_RIVERBED) {
			submergedState = SubmergedState.SWIMMER_UNDER_WATER;
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

	/*
	 * REGISTRATION OF GAME OVER STATE INTERFACES
	 */
	@Override
	public void registerObserver(GameOverObserver goo) {
		gameOverObservers.add(goo);
	}

	@Override
	public void removeObserver(GameOverObserver goo) {
		int i = gameOverObservers.indexOf(goo);
		if(i>= 0) {
			gameOverObservers.remove(i);
		}
	}

	public void oxygenConsumed(OxygenMeter.OxygenConsumptionState ocs) {
		if(ocs == OxygenMeter.OxygenConsumptionState.EMPTY) {
			notifyObservers(GameScreen.GAME_DYING);
		}
	}
	@Override
	public void notifyObservers(int gameState) {
		for(int i = 0; i < gameOverObservers.size();i++) {
			GameOverObserver goo = gameOverObservers.get(i);
			goo.updateGameOverObserver(gameState);
		}

	}

	/*
	 * REGISTRATION OF SUBMERGED SUBJECT STATE INTERFACES
	 */

	@Override
	public void registerObserver(SubmergedObserver so) {
		submergedObservers.add(so);
	}

	@Override
	public void removeObserver(SubmergedObserver so) {
		int i = submergedObservers.indexOf(so);
		if(i>=0) {
			submergedObservers.remove(i);
		}
	}

	@Override
	public void notifyObservers(SubmergedState submergedState) {
		for(int i = 0; i < submergedObservers.size(); i++) {
			SubmergedObserver so = submergedObservers.get(i);
			so.updateSubmergedState(submergedState);
		}
	}


	/*
	 * REGISTRATION OF ORIENTATION STATE INTERFACES
	 */

	@Override
	public void registerObserver(OrientationObserver oo) {
		orientationObservers.add(oo);
	}

	@Override
	public void removeObserver(OrientationObserver oo) {
		int i = orientationObservers.indexOf(oo);
		if(i>=0) {
			orientationObservers.remove(i);
		}
	}

	@Override
	public void notifyObservers(OrientationState orientationState) {
		for(int i = 0; i < orientationObservers.size(); i++) {
			OrientationObserver oo = orientationObservers.get(i);
			oo.updateOrientationState(orientationState);
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
		oxygenMeter.reset();
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

	public void setSubmergedState(SubmergedState state) {
		this.submergedState = state;
	}


	public SubmergedState getSubmergedState() {
		return this.submergedState;
	}

	public Body getSwimmerPhysicsBody() {
		return this.body;
	}

}