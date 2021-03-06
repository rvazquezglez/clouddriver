/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.appengine.deploy.converters

import com.netflix.spinnaker.clouddriver.appengine.AppengineOperation
import com.netflix.spinnaker.clouddriver.appengine.deploy.description.StartStopAppengineDescription
import com.netflix.spinnaker.clouddriver.appengine.deploy.ops.StartAppengineAtomicOperation
import com.netflix.spinnaker.clouddriver.appengine.security.AppengineNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsConverter
import org.springframework.stereotype.Component

@AppengineOperation(AtomicOperations.START_SERVER_GROUP)
@Component
class StartAppengineAtomicOperationConverter extends AbstractAtomicOperationsCredentialsConverter<AppengineNamedAccountCredentials> {
  AtomicOperation convertOperation(Map input) {
    new StartAppengineAtomicOperation(convertDescription(input))
  }

  StartStopAppengineDescription convertDescription(Map input) {
    AppengineAtomicOperationConverterHelper.convertDescription(input, this, StartStopAppengineDescription)
  }
}
