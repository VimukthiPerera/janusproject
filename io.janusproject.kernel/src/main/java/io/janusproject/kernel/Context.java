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
package io.janusproject.kernel;

import io.janusproject.services.LogService;
import io.janusproject.services.SpaceRepositoryListener;
import io.janusproject.util.TwoStepConstruction;
import io.sarl.lang.core.AgentContext;
import io.sarl.lang.core.EventSpace;
import io.sarl.lang.core.Space;
import io.sarl.lang.core.SpaceID;
import io.sarl.lang.core.SpaceSpecification;
import io.sarl.lang.util.SynchronizedCollection;
import io.sarl.util.OpenEventSpace;
import io.sarl.util.OpenEventSpaceSpecification;

import java.util.UUID;

import com.google.inject.Injector;
import com.hazelcast.core.HazelcastInstance;

/** Implementation of an agent context in the Janus platform.
 * 
 * @author $Author: srodriguez$
 * @author $Author: ngaud$
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
@TwoStepConstruction
class Context implements AgentContext{

	private final UUID id;

	private final SpaceRepository spaceRepository;
	
	private final UUID defaultSpaceID;
	private OpenEventSpace defaultSpace;
	
	
	/** Constructs a <code>Context</code>.
	 * <p>
	 * CAUTION: Do not miss to call {@link #postConstruction()}.
	 * 
	 * @param id - identifier of the context.
	 * @param defaultSpaceID - identifier of the default space in the context.
	 * @param factory - factory to use for creating the space repository.
	 * @param startUpListener - repository listener which is added just after the creation of the repository, but before the creation of the default space.
	 */
	Context(UUID id, UUID defaultSpaceID, SpaceRepositoryFactory factory, SpaceRepositoryListener startUpListener) {
		assert(factory!=null);
		this.id = id;
		this.defaultSpaceID = defaultSpaceID;
		this.spaceRepository = factory.newInstance(
				this,
				id.toString()+"-spaces", //$NON-NLS-1$
				startUpListener);
	}
	
	@Override
	public String toString() {
		return this.id.toString();
	}
	
	/** Create the default space in this context.
	 * 
	 * @return the created space.
	 */
	EventSpace postConstruction() {
		this.spaceRepository.postConstruction();
		this.defaultSpace = createSpace(OpenEventSpaceSpecification.class, this.defaultSpaceID);
		return this.defaultSpace;
	}
	
	/** Destroy any associated resources.
	 */
	public void destroy() {
		this.spaceRepository.destroy();
	}
	
	@Override
	public UUID getID() {
		return this.id;
	}

	@Override
	public OpenEventSpace getDefaultSpace() {
		return this.defaultSpace;
	}

	@Override
	public SynchronizedCollection<? extends io.sarl.lang.core.Space> getSpaces() {
		return this.spaceRepository.getSpaces();
	}

	@Override
	public <S extends io.sarl.lang.core.Space> S createSpace(Class<? extends SpaceSpecification<S>> spec,
			UUID spaceUUID, Object... creationParams) {
		return this.spaceRepository.createSpace(new SpaceID(this.id, spaceUUID, spec), spec, creationParams);
	}


	@Override
	public <S extends io.sarl.lang.core.Space> S getOrCreateSpace(
			Class<? extends SpaceSpecification<S>> spec, UUID spaceUUID,
			Object... creationParams) {
		return this.spaceRepository.getOrCreateSpace(spec, new SpaceID(this.id, spaceUUID, spec), creationParams);
	}

	/** {@inheritDoc}
	 */
	@Override
	public <S extends Space> SynchronizedCollection<S> getSpaces(Class<? extends SpaceSpecification<S>> spec) {
		return this.spaceRepository.getSpaces(spec);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends io.sarl.lang.core.Space> S getSpace(UUID spaceUUID) {
		//Type safety: assume that any ClassCastException will be thrown in the caller context.
		return (S) this.spaceRepository.getSpace(
				// The space specification parameter
				// could be null because it will
				// not be used during the search.
				new SpaceID(this.id, spaceUUID, null));
	}
	
	/** Listener on the events in the space repository.
	 * 
	 * @author $Author: sgalland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	private static class SpaceListener implements SpaceRepositoryListener {
		
		private final Context context;
		private final SpaceRepositoryListener relay;
		private final LogService logger;
		
		/**
		 * @param context
		 * @param logger
		 * @param relay
		 */
		public SpaceListener(Context context, LogService logger, SpaceRepositoryListener relay) {
			assert(context!=null);
			assert(logger!=null);
			assert(relay!=null);
			this.context = context;
			this.logger = logger;
			this.relay = relay;
		}

		/** {@inheritDoc}
		 */
		@Override
		public void spaceCreated(Space space, boolean isLocalCreation) {
			this.logger.info(Context.class, "SPACE_CREATED", space.getID()); //$NON-NLS-1$
			// Notify the relays (other services)
			this.relay.spaceCreated(space, isLocalCreation);
		}

		/** {@inheritDoc}
		 */
		@Override
		public void spaceDestroyed(Space space, boolean isLocalDestruction) {
			this.logger.info(Context.class, "SPACE_DESTROYED", space.getID()); //$NON-NLS-1$
			// Notify the relays (other services)
			this.relay.spaceDestroyed(space, isLocalDestruction);
		}
		
	}
	
	/** Factory for the space repository in a context.
	 * 
	 * @author $Author: sgalland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 */
	public static class DefaultSpaceRepositoryFactory implements SpaceRepositoryFactory {

		private final HazelcastInstance hzInstance;
		private final Injector injector;
		private final LogService logger;
		
		/**
		 * @param injector - instance of the injector to be used.
		 * @param hzInstance - instance of the hazelcast engine.
		 * @param logger - logging service.
		 */
		public DefaultSpaceRepositoryFactory(Injector injector, HazelcastInstance hzInstance, LogService logger) {
			this.hzInstance = hzInstance;
			this.injector = injector;
			this.logger = logger;
		}
		
		@Override
		public SpaceRepository newInstance(Context context, String distributedSpaceSetName, SpaceRepositoryListener listener) {
			return new SpaceRepository(
					distributedSpaceSetName,
					this.hzInstance,
					this.injector,
					this.logger,
					new SpaceListener(context, this.logger, listener));
		}
		
	}

}
