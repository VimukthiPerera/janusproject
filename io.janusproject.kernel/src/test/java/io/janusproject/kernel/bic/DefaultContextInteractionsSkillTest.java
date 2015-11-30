/*
 * $Id$
 * 
 * Janus platform is an open-source multiagent platform.
 * More details on http://www.janusproject.io
 * 
 * Copyright (C) 2014-2015 Sebastian RODRIGUEZ, Nicolas GAUD, Stéphane GALLAND.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.janusproject.testutils.AbstractJanusTest;
import io.sarl.core.Lifecycle;
import io.sarl.lang.core.Address;
import io.sarl.lang.core.Agent;
import io.sarl.lang.core.AgentContext;
import io.sarl.lang.core.BuiltinCapacitiesProvider;
import io.sarl.lang.core.Capacity;
import io.sarl.lang.core.Event;
import io.sarl.lang.core.EventSpace;
import io.sarl.lang.core.EventSpaceSpecification;
import io.sarl.lang.core.Scope;
import io.sarl.lang.core.Space;
import io.sarl.lang.core.SpaceID;
import io.sarl.util.Scopes;

import java.util.UUID;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;

/**
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
@SuppressWarnings("all")
public class DefaultContextInteractionsSkillTest extends AbstractJanusTest {

	@Nullable
	private EventSpace defaultSpace;

	@Nullable
	private AgentContext parentContext;

	@Nullable
	private DefaultContextInteractionsSkill skill;

	@Nullable
	private Address address;

	@Nullable
	private Lifecycle lifeCapacity;

	@Nullable
	private SpaceID defaultSpaceID;

	@Nullable
	private UUID defaultSpaceUUID;

	@Nullable
	private UUID defaultContextUUID;

	@Before
	public void setUp() throws Exception {
		this.defaultSpaceUUID = UUID.randomUUID();
		this.defaultContextUUID = UUID.randomUUID();

		this.defaultSpaceID = new SpaceID(
				this.defaultContextUUID,
				this.defaultSpaceUUID,
				EventSpaceSpecification.class);

		this.address = new Address(
				this.defaultSpaceID,
				UUID.randomUUID());

		this.defaultSpace = mock(EventSpace.class);
		when(this.defaultSpace.getAddress(Matchers.any(UUID.class))).thenReturn(this.address);
		when(this.defaultSpace.getID()).thenReturn(this.defaultSpaceID);

		this.parentContext = mock(AgentContext.class);
		when(this.parentContext.getDefaultSpace()).thenReturn(this.defaultSpace);
		when(this.parentContext.getID()).thenReturn(this.defaultContextUUID);

		this.lifeCapacity = mock(Lifecycle.class);

		Agent agent = new Agent(
				Mockito.mock(BuiltinCapacitiesProvider.class),
				UUID.randomUUID(),
				null) {
			@Override
			protected <S extends Capacity> S getSkill(Class<S> capacity) {
				return capacity.cast(DefaultContextInteractionsSkillTest.this.lifeCapacity);
			}
		};
		this.skill = new DefaultContextInteractionsSkill(agent, this.parentContext);
	}

	@Test
	public void getDefaultContext() {
		assertSame(this.parentContext, this.skill.getDefaultContext());
	}

	@Test
	public void getDefaultSpace() {
		assertNull(this.skill.getDefaultSpace());
		this.skill.install();
		assertSame(this.defaultSpace, this.skill.getDefaultSpace());
	}

	@Test
	public void getDefaultAddress() {
		this.skill.install();
		assertSame(this.address, this.skill.getDefaultAddress());
	}

	@Test
	public void install() {
		assertNull(this.skill.getDefaultSpace());
		this.skill.install();
		assertSame(this.defaultSpace, this.skill.getDefaultSpace());
	}

	@Test
	public void emitEventScope() {
		this.skill.install();
		Event event = mock(Event.class);
		Scope<Address> scope = Scopes.allParticipants();
		this.skill.emit(event, scope);
		ArgumentCaptor<Event> argument1 = ArgumentCaptor.forClass(Event.class);
		ArgumentCaptor<Scope> argument2 = ArgumentCaptor.forClass(Scope.class);
		verify(this.defaultSpace, new Times(1)).emit(argument1.capture(), argument2.capture());
		assertSame(event, argument1.getValue());
		assertSame(scope, argument2.getValue());
		ArgumentCaptor<Address> argument3 = ArgumentCaptor.forClass(Address.class);
		verify(event).setSource(argument3.capture());
		assertEquals(this.address, argument3.getValue());
	}

	@Test
	public void emitEvent() {
		this.skill.install();
		Event event = mock(Event.class);
		this.skill.emit(event);
		ArgumentCaptor<Event> argument1 = ArgumentCaptor.forClass(Event.class);
		verify(this.defaultSpace, new Times(1)).emit(argument1.capture());
		assertSame(event, argument1.getValue());
		ArgumentCaptor<Address> argument2 = ArgumentCaptor.forClass(Address.class);
		verify(event).setSource(argument2.capture());
		assertEquals(this.address, argument2.getValue());
	}

	@Test
	public void spawn() {
		this.skill.install();
		this.skill.spawn(Agent.class, "a", "b", "c"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		ArgumentCaptor<Class> argument1 = ArgumentCaptor.forClass(Class.class);
		ArgumentCaptor<AgentContext> argument2 = ArgumentCaptor.forClass(AgentContext.class);
		ArgumentCaptor<String> argument3 = ArgumentCaptor.forClass(String.class);
		verify(this.lifeCapacity, times(1)).spawnInContext(argument1.capture(),
				argument2.capture(), argument3.capture());
		assertEquals(Agent.class, argument1.getValue());
		assertSame(this.parentContext, argument2.getValue());
		assertArrayEquals(new String[] { "a", "b", "c" }, argument3.getAllValues().toArray()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	}

	@Test(expected = NullPointerException.class)
	public void isDefaultSpaceSpace_null() {
		this.skill.install();
		this.skill.isDefaultSpace((Space) null);
	}

	@Test
	public void isDefaultSpaceSpace_defaultSpace() {
		this.skill.install();
		assertTrue(this.skill.isDefaultSpace(this.defaultSpace));
	}

	@Test
	public void isDefaultSpaceSpace_otherSpace() {
		this.skill.install();
		UUID id = UUID.randomUUID();
		SpaceID spaceId = mock(SpaceID.class);
		when(spaceId.getID()).thenReturn(id);
		EventSpace otherSpace = mock(EventSpace.class);
		when(otherSpace.getID()).thenReturn(spaceId);
		//
		assertFalse(this.skill.isDefaultSpace(otherSpace));
	}

	@Test(expected = NullPointerException.class)
	public void isDefaultSpaceSpaceID_null() {
		this.skill.install();
		this.skill.isDefaultSpace((SpaceID) null);
	}

	@Test
	public void isDefaultSpaceSpaceID_defaultSpace() {
		this.skill.install();
		assertTrue(this.skill.isDefaultSpace(this.defaultSpace.getID()));
	}

	@Test
	public void isDefaultSpaceSpaceID_otherSpace() {
		this.skill.install();
		UUID id = UUID.randomUUID();
		SpaceID spaceId = mock(SpaceID.class);
		when(spaceId.getID()).thenReturn(id);
		//
		assertFalse(this.skill.isDefaultSpace(spaceId));
	}

	@Test(expected = NullPointerException.class)
	public void isDefaultSpaceUUID_null() {
		this.skill.install();
		this.skill.isDefaultSpace((UUID) null);
	}

	@Test
	public void isDefaultSpaceUUID_defaultSpace() {
		this.skill.install();
		assertTrue(this.skill.isDefaultSpace(this.defaultSpaceUUID));
	}

	@Test
	public void isDefaultSpaceUUID_otherSpace() {
		this.skill.install();
		UUID id = UUID.randomUUID();
		assertFalse(this.skill.isDefaultSpace(id));
	}

	@Test
	public void isInDefaultSpaceEvent_null() {
		this.skill.install();
		assertFalse(this.skill.isInDefaultSpace(null));
	}

	@Test
	public void isInDefaultSpaceEvent_defaultSpace() {
		this.skill.install();
		Event event = mock(Event.class);
		when(event.getSource()).thenReturn(this.address);
		//
		assertTrue(this.skill.isInDefaultSpace(event));
	}

	@Test
	public void isInDefaultSpaceEvent_otherSpace() {
		this.skill.install();
		SpaceID spaceID = new SpaceID(this.defaultSpaceID.getContextID(), UUID.randomUUID(), EventSpaceSpecification.class);
		Address adr = mock(Address.class);
		when(adr.getSpaceId()).thenReturn(spaceID);
		Event event = mock(Event.class);
		when(event.getSource()).thenReturn(adr);
		//
		assertFalse(this.skill.isInDefaultSpace(event));
	}

	@Test(expected = NullPointerException.class)
	public void isDefaultContextAgentContext_null() {
		this.skill.install();
		this.skill.isDefaultContext((AgentContext) null);
	}

	@Test
	public void isDefaultContextAgentContext_defaultContext() {
		this.skill.install();
		assertTrue(this.skill.isDefaultContext(this.parentContext));
	}

	@Test
	public void isDefaultContextAgentContext_otherContext() {
		this.skill.install();
		AgentContext context = mock(AgentContext.class);
		when(context.getID()).thenReturn(UUID.randomUUID());
		//
		assertFalse(this.skill.isDefaultContext(context));
	}

	@Test(expected = NullPointerException.class)
	public void isDefaultContextUUID_null() {
		this.skill.install();
		this.skill.isDefaultContext((UUID) null);
	}

	@Test
	public void isDefaultContextUUID_defaultContext() {
		this.skill.install();
		assertTrue(this.skill.isDefaultContext(this.parentContext.getID()));
	}

	@Test
	public void isDefaultContextUUID_otherContext() {
		this.skill.install();
		assertFalse(this.skill.isDefaultContext(UUID.randomUUID()));
	}

}
