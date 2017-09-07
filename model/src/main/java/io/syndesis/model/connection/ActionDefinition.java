/**
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.syndesis.model.connection;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.syndesis.model.ImmutablesStyle;
import io.syndesis.model.WithName;
import io.syndesis.model.WithProperties;

import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ActionDefinition.Builder.class)
public interface ActionDefinition extends Serializable {

    final class Builder extends ImmutableActionDefinition.Builder {
        // make ImmutableActionDefinition.Builder accessible

        public Builder withActionDefinitionStep(final String name, final String description,
            final Consumer<ActionDefinitionStep.Builder> stepConfigurator) {
            final ActionDefinitionStep.Builder stepBuilder = new ActionDefinitionStep.Builder().name(name)
                .description(description);

            stepConfigurator.accept(stepBuilder);

            addPropertyDefinitionStep(stepBuilder.build());

            return this;
        }
    }

    @Value.Immutable
    @JsonDeserialize(builder = ActionDefinitionStep.Builder.class)
    @ImmutablesStyle
    interface ActionDefinitionStep extends WithName, WithProperties, Serializable {

        final class Builder extends ImmutableActionDefinitionStep.Builder {
            // make ImmutableActionDefinitionStep.Builder accessible
        }

        String getDescription();
    }

    List<ActionDefinitionStep> getPropertyDefinitionSteps();

    Optional<DataShape> getInputDataShape();

    Optional<DataShape> getOutputDataShape();
}
