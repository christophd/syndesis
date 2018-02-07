/*
 * Copyright (C) 2016 Red Hat, Inc.
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
package io.syndesis.rest.metrics.prometheus;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.syndesis.core.Json;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

/**
 * Query result from Prometheus HTTP API.
 * @author dhirajb
 */
@Value.Immutable
@JsonDeserialize(builder = QueryResult.Builder.class)
public interface QueryResult extends Serializable {

    static <T> Optional<T> getResponseValue(QueryResult response, Class<? extends T> clazz) {
        final List<Data.Result> result = response.getData().orElseThrow(IllegalArgumentException::new)
            .getResult();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        else {
            try {
                return Optional.of(Json.reader().forType(clazz).readValue(result.get(0).getValue().get(1).toString()));
            } catch (IOException e) {
                throw new IllegalArgumentException("Error parsing metric value " + e.getMessage());
            }
        }
    }

    class Builder extends ImmutableQueryResult.Builder {
        // make ImmutableQueryResult.Builder accessible
    }

    @Value.Immutable
    @JsonDeserialize(builder = Data.Builder.class)
    interface Data {

        class Builder extends ImmutableData.Builder {
            // make ImmutableData.Builder accessible
        }

        @Value.Immutable
        @JsonDeserialize(builder = Result.Builder.class)
        interface Result {

            class Builder extends ImmutableResult.Builder {
                // make ImmutableResult.Builder accessible
            }

            Map<String, String> getMetric();

            List<Object> getValue();
        }

        String getResultType();

        List<Result> getResult();
    }

    String getStatus();

    Optional<String> getErrorType();

    Optional<String> getError();

    Optional<Data> getData();

    @JsonIgnore
    default boolean isError() {
        return "error".equals(getStatus());
    }
}
