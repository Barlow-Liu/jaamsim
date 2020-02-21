/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.ProcessFlow.StateUserEntity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.states.StateEntity;

public class EntitySystem extends StateEntity {

	@Keyword(description = "An expression returning a string that sets this object's present "
	                     + "state.",
	         exampleList = {"'[Server1].Working || [Server2].Working ? \"Working\" : \"Idle\"'"})
	protected final ExpressionInput stateExp;

	private final ArrayList<StateUserEntity> entityList = new ArrayList<>();

	{
		stateExp = new ExpressionInput("StateExpression", KEY_INPUTS, null);
		stateExp.setResultType(ExpResType.STRING);
		stateExp.setRequired(true);
		this.addInput(stateExp);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		entityList.clear();
		for (StateUserEntity stateEnt : getJaamSimModel().getClonesOfIterator(StateUserEntity.class)) {
			if (stateEnt.getEntitySystem() == this)
				entityList.add(stateEnt);
		}
	}

	public void performUpdate() {
		if (updateHandle.isScheduled())
			return;
		EventManager.scheduleTicks(0L, 11, false, updateTarget, updateHandle);
	}

	private final EventHandle updateHandle = new EventHandle();
	private final ProcessTarget updateTarget = new ProcessTarget() {

		@Override
		public void process() {
			setPresentState();
		}

		@Override
		public String getDescription() {
			return "setPresentState";
		}

	};

	public void setPresentState() {
		double simTime = getSimTime();

		// Calculate the state from the StateExpression input
		try {
			ExpResult res = ExpEvaluator.evaluateExpression(stateExp.getValue(), simTime);
			setPresentState(res.stringVal);
		}
		catch (ExpError e) {
			throw new ErrorException(this, e);
		}
	}

	@Output(name = "EntityList",
	 description = "Entities included in this system.",
	    sequence = 1)
	public ArrayList<StateUserEntity> getEntityList(double simTime) {
		return entityList;
	}

}
