/*
 * $Id$
 * 
 * Janus platform is an open-source multiagent platform.
 * More details on http://www.janusproject.io
 * 
 * Copyright (C) 2014 Sebastian RODRIGUEZ, Nicolas GAUD, Stéphane GALLAND.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.janusproject.kernel.bic;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import io.janusproject.services.executor.ChuckNorrisException;
import io.janusproject.services.spawn.SpawnService;
import io.janusproject.testutils.AbstractJanusTest;
import io.sarl.lang.core.Agent;
import io.sarl.lang.core.AgentContext;
import io.sarl.lang.core.AgentTraitUnitTestAccessor;
import io.sarl.lang.core.Event;

import java.util.UUID;

import javax.annotation.Nullable;

import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

/**
 * @author $Author: srodriguez$
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
@SuppressWarnings("all")
public class LifecycleSkillTest extends AbstractJanusTest {

	@Nullable
	private UUID agentId;
	
	@Mock
	private SpawnService spawnService;
	
	@Mock
	private InternalEventBusSkill eventBus;

	@InjectMocks
	private LifecycleSkill skill;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.agentId = UUID.randomUUID();
		Agent agent = new Agent(UUID.randomUUID()) {
			@SuppressWarnings("synthetic-access")
			@Override
			protected <S extends io.sarl.lang.core.Capacity> S getSkill(java.lang.Class<S> capacity) {
				return capacity.cast(LifecycleSkillTest.this.eventBus);
			}
			@SuppressWarnings("synthetic-access")
			@Override
			public UUID getID() {
				return LifecycleSkillTest.this.agentId;
			}
		};
		AgentTraitUnitTestAccessor.setOwner(this.skill, agent);
	}

	@Test
	public void spawnInContext() {
		Class type = Agent.class;
		AgentContext context = mock(AgentContext.class);
		this.skill.spawnInContext(type, context, 1, "String"); //$NON-NLS-1$
		ArgumentCaptor<AgentContext> argument1 = ArgumentCaptor.forClass(AgentContext.class);
		ArgumentCaptor<UUID> argument2 = ArgumentCaptor.forClass(UUID.class);
		ArgumentCaptor<Class> argument3 = ArgumentCaptor.forClass(Class.class);
		ArgumentCaptor<Object> argument4 = ArgumentCaptor.forClass(Object.class);
		verify(this.spawnService, times(1)).spawn(argument1.capture(), argument2.capture(),
				argument3.capture(), argument4.capture());
		assertSame(context, argument1.getValue());
		assertNull(argument2.getValue());
		assertEquals(Agent.class, argument3.getValue());
		assertArrayEquals(new Object[] { 1, "String" }, argument4.getAllValues().toArray()); //$NON-NLS-1$
	}

	@Test
	public void spawnInContextWithID() {
		Class type = Agent.class;
		AgentContext context = mock(AgentContext.class);
		this.skill.spawnInContextWithID(type, this.agentId, context, 1, "String"); //$NON-NLS-1$
		ArgumentCaptor<AgentContext> argument1 = ArgumentCaptor.forClass(AgentContext.class);
		ArgumentCaptor<UUID> argument2 = ArgumentCaptor.forClass(UUID.class);
		ArgumentCaptor<Class> argument3 = ArgumentCaptor.forClass(Class.class);
		ArgumentCaptor<Object> argument4 = ArgumentCaptor.forClass(Object.class);
		verify(this.spawnService, times(1)).spawn(argument1.capture(), argument2.capture(),
				argument3.capture(), argument4.capture());
		assertSame(context, argument1.getValue());
		assertSame(this.agentId, argument2.getValue());
		assertEquals(Agent.class, argument3.getValue());
		assertArrayEquals(new Object[] { 1, "String" }, argument4.getAllValues().toArray()); //$NON-NLS-1$
	}

	@Test
	public void killMe() throws Exception {
		try {
			this.skill.killMe();
			fail("killMe() must never return!"); //$NON-NLS-1$
		}
		catch(ChuckNorrisException _) {
			// Expected exception
		}
		catch(Exception e) {
			throw e;
		}
		ArgumentCaptor<Event> argument = ArgumentCaptor.forClass(Event.class);
		verify(this.eventBus, times(1)).selfEvent(argument.capture());
		assertThat(argument.getValue(), IsInstanceOf.instanceOf(AsynchronousAgentKillingEvent.class));
	}

}
