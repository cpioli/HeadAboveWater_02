package com.cpioli.headabovewater.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.scenes.scene2d.Actor;

/*
 * TODO: call the MeshActor's fillRemainderStretch when entering or exiting a particular cell
 */

public class MeshActor extends Actor {
	public static int HORIZONTAL_GRADIENT = 0;
	public static int VERTICAL_GRADIENT = 1;
	//Initialization of Object fields
	ShapeRenderer renderer;
	
	//if it's a gradient, then we'll use a different ShapeRenderer.rect object
	boolean gradient;
	Color secondaryColor;
	int gradientOrientation;
	
	
	//constructors and copy constructors
	public MeshActor() {
		this(0.0f, 0.0f, 0.0f, 0.0f, new Color(), null, null);
		renderer = null;
		gradient = false;
		secondaryColor = null;
		gradientOrientation = HORIZONTAL_GRADIENT;
		
	}
	
	public MeshActor(float x, float y, float width, float height, Color color, String name, ShapeRenderer renderer) {
		//setting all super values
		super.setX(x);
		super.setY(y);
		super.setWidth(width);
		super.setHeight(height);
		super.setColor(color);
		super.setName(name);
		this.renderer = renderer;

		//this.origin = transformOrigin.LOWER_LEFT;
	}

	
	/**
	 * this draw method is called by Group and Stage objects.
	 * Although we'll never really need a batcher, the parentAlpha
	 * is important.
	 * 
	 * This takes the place of the render function. the SpriteBatch will not
	 * be used
	 */
	@Override
	public void draw(Batch batch, float parentAlpha) {
		batch.end();

		//float tempColor = Color.toFloatBits(super.getColor().r, super.getColor().g, super.getColor().b, super.getColor().a * parentAlpha);
		if(parentAlpha < 1.0f) {
			Gdx.gl.glEnable(GL20.GL_BLEND);
		}
		renderer.begin(ShapeType.Filled);
			if(gradient == false) {
				renderer.setColor(super.getColor());
				renderer.rect(super.getX(), super.getY(), super.getWidth(), super.getHeight());
			} else if (gradientOrientation == HORIZONTAL_GRADIENT) {
				renderer.rect(super.getX(), super.getY(), super.getWidth(), super.getHeight(), super.getColor(), super.getColor(), secondaryColor, secondaryColor);
			} else if (gradientOrientation == VERTICAL_GRADIENT) {
				renderer.rect(super.getX(), super.getY(), super.getWidth(), super.getHeight(), super.getColor(), secondaryColor, secondaryColor, super.getColor());
			}
		renderer.end();
			
		if(parentAlpha < 1.0f) {
			Gdx.gl.glDisable(GL20.GL_BLEND);
		}
		batch.begin();
	}

	/**
	 * returns true if the point specified by the two floating point parameters
	 * lies within the range of the actor.
	 * returns false otherwise
	 * @param x
	 * @param y
	 * @return
	 */
	public static boolean contains(MeshActor cell, float x, float y) {
		return (cell.getX() < x && cell.getRight() > x && cell.getY() < y && cell.getTop() > y);
	}
	
	/*-----------------------
	 * Getter and Setter methods
	 ------------------------*/
	
	@Override
	public float getWidth(){
		return super.getWidth();
	}
	
	@Override
	public float getHeight() {
		return super.getHeight();
	}

	public Color getColor() {
		return super.getColor();
	}
	
	public void setX(float x) {
		super.setX(x);
	}
	
	public void setY(float y) {
		super.setY(y);
	}
	
	public void setXY(float x, float y) {
		super.setX(x);
		super.setY(y);
	}

}