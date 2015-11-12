package com.cpioli.headabovewater.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Group;

public class Border extends Group{
	
	public float thickness;
	public float x;
	public float y;
	public float width;
	public float height;
	public Color color;
	private MeshActor[] lines;
	ShapeRenderer renderer;
	
	public Border(float thickness, float x, float y, float width, float height, Color color, ShapeRenderer renderer) {
		this.thickness = thickness;
		
		lines = new MeshActor[4];
		lines[0] = new MeshActor(                    x,                      y,     width,               thickness, color, "south", renderer);//lower _
		lines[1] = new MeshActor(x + width - thickness,          y + thickness, thickness, height - 2.0f*thickness, color, "east", renderer);//right |
		lines[2] = new MeshActor(                    x, y + height - thickness,     width,               thickness, color, "north", renderer); //upper _
		lines[3] = new MeshActor(                    x,          y + thickness, thickness, height - 2.0f*thickness, color, "west", renderer); //left |
		for(int i = 0; i < 4; i++) {
			this.addActor(lines[i]);
		}
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
}