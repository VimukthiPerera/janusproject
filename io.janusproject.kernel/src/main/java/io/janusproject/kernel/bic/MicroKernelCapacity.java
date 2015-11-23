/*
 * $Id$
 *
 * Janus platform is an open-source multiagent platform.
 * More details on http://www.janusproject.io
 *
 * Copyright (C) 2014-2015 the original authors or authors.
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

import com.google.common.util.concurrent.Service;

import io.sarl.lang.core.Capacity;

/** Capacity that provides an access to the micro kernel,
 * according to the FIPA specs.
 *
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public interface MicroKernelCapacity extends Capacity {

	/** Replies a kernel service that is alive.
	 *
	 * @param <S> - type of the service to reply.
	 * @param type - type of the service to reply.
	 * @return the service, or <code>null</code>.
	 */
	<S extends Service> S getService(Class<S> type);

}
