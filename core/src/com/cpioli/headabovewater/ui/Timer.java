package com.cpioli.headabovewater.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.cpioli.headabovewater.Assets;

public class Timer extends Group {
	
	private Label time;
	private float timePassed;
	private String MSDivisor;
	private String SHDivisor;
	private boolean paused;
	private int minutes;
	private int seconds;
	private int hundredths;
	
	public Timer(float x, float y) {
		timePassed = 0.0f;
		time = Assets.bigClockNumbers;
		time.setAlignment(Align.right);
		time.setPosition(x, y);//, 250.0f, 50.0f);
		this.addActor(time);
		hundredths = 0;
		minutes = 0;
		seconds = 0;
	}
	
	@Override
	public void act(float delta) {
		super.act(delta);

		if(!paused) {
			timePassed += delta;
		}
		minutes = ((int)timePassed) / 60;
		seconds = ((int)timePassed) % 60;
		hundredths = (int) ((timePassed - minutes * 60 - seconds) * 100);
		
		
		if(seconds < 10) {
			MSDivisor = ":0";
		} else if (seconds == 0) {
			MSDivisor = ":00";
		} else {
			MSDivisor = ":";
		}
		
		if(hundredths < 10) {
			SHDivisor = ".0";
		} else if (hundredths == 0) {
			SHDivisor = ".00";
		} else {
			SHDivisor = ".";
		}
		
		time.setText("" + Integer.toString(minutes) + MSDivisor + Integer.toString(seconds) + SHDivisor + Integer.toString(hundredths));
		//System.out.println("Time: " + timePassed);
	}
	
	public void restart() {
		timePassed = 0.0f;
	}
	
	public void pause() {
		paused = true;
	}
	
	public void resume() {
		paused = false;
	}
	
	public int getMinutes() {
		return minutes;
	}
	
	public int getSeconds() {
		return seconds;
	}
	
	public int getHundredths() {
		return hundredths;
	}
	
	public Label getTime() {
		return time;
	}
}