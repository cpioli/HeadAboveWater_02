package com.cpioli.headabovewater.ui;


public interface SubmergedSubject {
	public void registerObserver(SubmergedObserver so);
	public void removeObserver(SubmergedObserver so);
	public void notifyObservers(Swimmer.SubmergedState submergedState);
}