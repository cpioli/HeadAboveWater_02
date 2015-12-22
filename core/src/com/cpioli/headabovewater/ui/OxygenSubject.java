package com.cpioli.headabovewater.ui;

public interface OxygenSubject {
	public void registerObserver(OxygenObserver oo);
	public void removeObserver(OxygenObserver oo);
	public void notifyObservers(OxygenMeter.OxygenConsumptionState ocs);
}
