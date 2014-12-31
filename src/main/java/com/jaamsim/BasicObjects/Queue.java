/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2003-2011 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;

public class Queue extends LinkedComponent {

	@Keyword(description = "The amount of graphical space shown between DisplayEntity objects in the queue.",
	         example = "Queue1 Spacing { 1 m }")
	private final ValueInput spacing;

	@Keyword(description = "The number of queuing entities in each row.",
			example = "Queue1 MaxPerLine { 4 }")
	protected final IntegerInput maxPerLine; // maximum items per sub line-up of queue

	protected ArrayList<DisplayEntity> itemList;
	private ArrayList<Double> timeAddedList;

	//	Statistics
	protected double timeOfLastUpdate; // time at which the statistics were last updated
	protected double startOfStatisticsCollection; // time at which statistics collection was started
	protected int minElements; // minimum observed number of entities in the queue
	protected int maxElements; // maximum observed number of entities in the queue
	protected double elementSeconds;  // total time that entities have spent in the queue
	protected double squaredElementSeconds;  // total time for the square of the number of elements in the queue
	protected DoubleVector queueLengthDist;  // entry at position n is the total time the queue has had length n

	{
		this.setDefaultSize(new Vec3d(0.5, 0.5, 0.5));
		testEntity.setHidden(true);
		nextComponentInput.setHidden(true);

		spacing = new ValueInput("Spacing", "Key Inputs", 0.0d);
		spacing.setUnitType(DistanceUnit.class);
		spacing.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(spacing);

		maxPerLine = new IntegerInput("MaxPerLine", "Key Inputs", Integer.MAX_VALUE);
		maxPerLine.setValidRange(1, Integer.MAX_VALUE);
		this.addInput(maxPerLine);
	}

	public Queue() {
		itemList = new ArrayList<>();
		timeAddedList = new ArrayList<>();
		queueLengthDist = new DoubleVector(10,10);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		// Clear the entries in the queue
		itemList.clear();
		timeAddedList.clear();

		// Clear statistics
		this.clearStatistics();
	}

	// ******************************************************************************************************
	// QUEUE HANDLING METHODS
	// ******************************************************************************************************

	@Override
	public void addDisplayEntity(DisplayEntity ent) {
		super.addDisplayEntity(ent);
		this.addLast(ent);
	}

	/**
	 * Inserts the specified element at the specified position in this Queue.
	 * Shifts the element currently at that position (if any) and any subsequent elements to the right (adds one to their indices).
	 */
	private void add(int i, DisplayEntity ent) {
		int queueSize = itemList.size();  // present number of entities in the queue
		this.updateStatistics(queueSize, queueSize+1);
		itemList.add(i, ent);
		timeAddedList.add(i, this.getSimTime());
	}

	/**
	 * Add an entity to the end of the queue
	 */
	public void addLast(DisplayEntity ent) {
		this.add(itemList.size(), ent);
	}

	/**
	 * Removes the entity at the specified position in the queue
	 */
	public DisplayEntity remove(int i) {
		if (i >= itemList.size() || i < 0)
			error("Index: %d is beyond the end of the queue.", i);

		int queueSize = itemList.size();  // present number of entities in the queue
		this.updateStatistics(queueSize, queueSize-1);

		DisplayEntity ent = itemList.remove(i);
		timeAddedList.remove(i);
		this.incrementNumberProcessed();
		return ent;
	}

	/**
	 * Removes the first entity from the queue
	 */
	public DisplayEntity removeFirst() {
		return this.remove(0);
	}

	/**
	 * Number of entities in the queue
	 */
	public int getCount() {
		return itemList.size();
	}

	/**
	 * Returns the number of seconds spent by the first object in the queue
	 */
	public double getQueueTime() {
		return this.getSimTime() - timeAddedList.get(0);
	}

	/**
	 * Update the position of all entities in the queue. ASSUME that entities
	 * will line up according to the orientation of the queue.
	 */
	@Override
	public void updateGraphics(double simTime) {

		Vec3d queueOrientation = getOrientation();
		Vec3d qSize = this.getSize();
		Vec3d tmp = new Vec3d();

		double distanceX = 0.5d * qSize.x;
		double distanceY = 0;
		double maxWidth = 0;

		// find widest vessel
		if (itemList.size() >  maxPerLine.getValue()){
			for (DisplayEntity ent : itemList) {
				 maxWidth = Math.max(maxWidth, ent.getSize().y);
			 }
		}

		// update item locations
		for (int i = 0; i < itemList.size(); i++) {

			// if new row is required, set reset distanceX and move distanceY up one row
			if( i > 0 && i % maxPerLine.getValue() == 0 ){
				 distanceX = 0.5d * qSize.x;
				 distanceY += spacing.getValue() + maxWidth;
			}

			DisplayEntity item = itemList.get(i);
			// Rotate each transporter about its center so it points to the right direction
			item.setOrientation(queueOrientation);
			Vec3d itemSize = item.getSize();
			distanceX += spacing.getValue() + 0.5d * itemSize.x;
			tmp.set3(-distanceX / qSize.x, distanceY/qSize.y, 0.0d);

			// increment total distance
			distanceX += 0.5d * itemSize.x;

			// Set Position
			Vec3d itemCenter = this.getGlobalPositionForAlignment(tmp);
			item.setPositionForAlignment(new Vec3d(), itemCenter);
		}
	}

	// *******************************************************************************************************
	// STATISTICS
	// *******************************************************************************************************

	/**
	 * Clear queue statistics
	 */
	@Override
	public void clearStatistics() {
		double simTime = this.getSimTime();
		startOfStatisticsCollection = simTime;
		timeOfLastUpdate = simTime;
		minElements = itemList.size();
		maxElements = itemList.size();
		elementSeconds = 0.0;
		squaredElementSeconds = 0.0;
		queueLengthDist.clear();
	}

	private void updateStatistics(int oldValue, int newValue) {

		minElements = Math.min(newValue, minElements);
		maxElements = Math.max(newValue, maxElements);

		// Add the necessary number of additional bins to the queue length distribution
		int n = newValue + 1 - queueLengthDist.size();
		for (int i = 0; i < n; i++) {
			queueLengthDist.add(0.0);
		}

		double simTime = this.getSimTime();
		double dt = simTime - timeOfLastUpdate;
		if (dt > 0.0) {
			elementSeconds += dt * oldValue;
			squaredElementSeconds += dt * oldValue * oldValue;
			queueLengthDist.addAt(dt,oldValue);  // add dt to the entry at index queueSize
			timeOfLastUpdate = simTime;
		}
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	@Output(name = "QueueLength",
	 description = "The present number of entities in the queue.",
	    unitType = DimensionlessUnit.class)
	public double getQueueLength(double simTime) {
		return itemList.size();
	}

	@Output(name = "QueueLengthAverage",
	 description = "The average number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public double getQueueLengthAverage(double simTime) {
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemList.size();
		double totalTime = simTime - startOfStatisticsCollection;
		if (totalTime > 0.0) {
			return (elementSeconds + dt*queueSize)/totalTime;
		}
		return 0.0;
	}

	@Output(name = "QueueLengthStandardDeviation",
	 description = "The standard deviation of the number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public double getQueueLengthStandardDeviation(double simTime) {
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemList.size();
		double mean = this.getQueueLengthAverage(simTime);
		double totalTime = simTime - startOfStatisticsCollection;
		if (totalTime > 0.0) {
			return Math.sqrt( (squaredElementSeconds + dt*queueSize*queueSize)/totalTime - mean*mean );
		}
		return 0.0;
	}

	@Output(name = "QueueLengthMinimum",
	 description = "The minimum number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public Integer getQueueLengthMinimum(double simTime) {
		return minElements;
	}

	@Output(name = "QueueLengthMaximum",
	 description = "The maximum number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public Integer getQueueLengthMaximum(double simTime) {
		// An entity that is added to an empty queue and removed immediately
		// does not count as a non-zero queue length
		if (maxElements == 1 && queueLengthDist.get(1) == 0.0)
			return 0;
		return maxElements;
	}

	@Output(name = "QueueLengthDistribution",
	 description = "The fraction of time that the queue has length 0, 1, 2, etc.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public DoubleVector getQueueLengthDistribution(double simTime) {
		DoubleVector ret = new DoubleVector(queueLengthDist);
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemList.size();
		double totalTime = simTime - startOfStatisticsCollection;
		if (totalTime > 0.0) {
			if (ret.size() == 0)
				ret.add(0.0);
			ret.addAt(dt, queueSize);  // adds dt to the entry at index queueSize
			for (int i = 0; i < ret.size(); i++) {
				ret.set(i, ret.get(i)/totalTime);
			}
		}
		else {
			ret.clear();
		}
		return ret;
	}

	@Output(name = "AverageQueueTime",
	 description = "The average time each entity waits in the queue.  Calculated as total queue time to date divided " +
			"by the total number of entities added to the queue.",
	    unitType = TimeUnit.class,
	  reportable = true)
	public double getAverageQueueTime(double simTime) {
		int n = this.getNumberAdded();
		if (n == 0)
			return 0.0;
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemList.size();
		return (elementSeconds + dt*queueSize)/n;
	}

}