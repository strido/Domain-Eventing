/*
    Copyright 2012, Strategic Gains, Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
 */
package com.strategicgains.eventing.hazelcast;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.ITopic;
import com.strategicgains.eventing.EventBus;
import com.strategicgains.eventing.EventHandler;

/**
 * Leverages Hazelcast to create a distrubuted EventQueue implementation to
 * support intra-cluster eventing.
 * 
 * @author toddf
 * @since Jun 27, 2012
 */
public class HazelcastEventBus<T extends Serializable>
extends EventBus
{
	private String name;
	private Set<Class<? extends Serializable>> publishableEventTypes = new HashSet<Class<? extends Serializable>>();

	public HazelcastEventBus(String queueName, List<EventHandler> subscribers)
	{
		super(new HazelcastTopicQueueAdapter(Hazelcast.getTopic(queueName)));
		addSubscribers(queueName, subscribers);
	}

	public HazelcastEventBus(String queueName, Config config,
	    List<EventHandler> subscribers)
	{
		super(new HazelcastTopicQueueAdapter(Hazelcast.init(config).getTopic(queueName)));
		addSubscribers(queueName, subscribers);
	}

    public boolean addPublishableEventType(Class<? extends Serializable> eventType)
    {
		return publishableEventTypes.add(eventType);
    }

	@Override
	public boolean canPublish(Class<?> eventType)
	{
		if (publishableEventTypes.isEmpty()) return true;
		
		return publishableEventTypes.contains(eventType);
	}

    @Override
    public boolean subscribe(EventHandler handler)
    {
		ITopic<Object> t = Hazelcast.getTopic(name);
		t.addMessageListener(new EventHandlerAdapter(handler));
		return true;
    }

    @Override
    public boolean unsubscribe(EventHandler handler)
    {
		ITopic<Object> t = Hazelcast.getTopic(name);
		t.removeMessageListener(new EventHandlerAdapter(handler));
		return true;
    }

	@Override
	public void shutdown()
	{
		Hazelcast.shutdownAll();
	}

	/**
	 * @param queueName the name of the event bus.
	 * @param subscribers a List of EventHandler instances that subscribed to the event bus.
	 */
	private void addSubscribers(String queueName, List<EventHandler> subscribers)
	{
		ITopic<Object> t = Hazelcast.getTopic(queueName);

		for (EventHandler handler : subscribers)
		{
			t.addMessageListener(new EventHandlerAdapter(handler));
		}
	}
}