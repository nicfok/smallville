package io.github.nickm980.smallville.update;

import io.github.nickm980.smallville.World;
import io.github.nickm980.smallville.entities.Agent;
import io.github.nickm980.smallville.entities.AgentLocation;
import io.github.nickm980.smallville.entities.SimulatedObject;

public class UpdateAgentExactLocation extends AgentUpdate {

    @Override
    public boolean update(IChatService converter, World world, Agent agent) {
	String objectName = converter.getExactLocation(agent);
	SimulatedObject object = world.getObjectByName(objectName);
	agent.setLocation(new AgentLocation(agent.getLocation(), object));
	
	return next(converter, world, agent);
    }
}