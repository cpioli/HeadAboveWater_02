package com.cpioli.headabovewater.utils;


public interface GameOverSubject {
	public void registerObserver(GameOverObserver goo);
	public void removeObserver(GameOverObserver goo);
	public void notifyObservers(int gameState);
}