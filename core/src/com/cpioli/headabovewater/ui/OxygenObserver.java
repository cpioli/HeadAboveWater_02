package com.cpioli.headabovewater.ui;

public interface OxygenObserver {
	public void oxygenConsumed(OxygenMeter.OxygenConsumptionState ocs);
}