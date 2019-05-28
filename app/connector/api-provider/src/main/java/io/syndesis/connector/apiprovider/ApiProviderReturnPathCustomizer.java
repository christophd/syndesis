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
package io.syndesis.connector.apiprovider;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import io.syndesis.common.model.DataShape;
import io.syndesis.common.model.DataShapeAware;
import io.syndesis.common.model.DataShapeKinds;
import io.syndesis.common.util.Json;
import io.syndesis.common.util.SyndesisConnectorException;
import io.syndesis.connector.support.processor.HttpRequestUnwrapperProcessor;
import io.syndesis.connector.support.processor.util.SimpleJsonSchemaInspector;
import io.syndesis.integration.component.proxy.ComponentProxyComponent;
import io.syndesis.integration.component.proxy.ComponentProxyCustomizer;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiProviderReturnPathCustomizer implements ComponentProxyCustomizer, CamelContextAware, DataShapeAware {

    private static final Logger LOG = LoggerFactory.getLogger(ApiProviderReturnPathCustomizer.class);

    private static final ObjectReader READER = Json.reader().forType(JsonNode.class);

    private static final String HTTP_RESPONSE_CODE_PROPERTY = "httpResponseCode";
    private static final String HTTP_ERROR_RESPONSE_CODES_PROPERTY = "errorResponseCodes";

    private CamelContext context;

    private DataShape inputDataShape;

    private DataShape outputDataShape;

    @Override
    public void customize(ComponentProxyComponent component, Map<String, Object> options) {
        if (inputDataShape != null && inputDataShape.getKind() == DataShapeKinds.JSON_SCHEMA && inputDataShape.getSpecification() != null) {
            try {
                final JsonNode schema = READER.readTree(inputDataShape.getSpecification());
                Set<String> properties = SimpleJsonSchemaInspector.getProperties(schema);
                Set<String> extraneousProperties = new HashSet<>(properties);
                extraneousProperties.removeAll(Arrays.asList("parameters", "body"));

                if (!properties.isEmpty() && extraneousProperties.isEmpty()) {
                    component.setBeforeProducer(new HttpRequestUnwrapperProcessor(schema));
                }
            } catch (IOException e) {
                throw new RuntimeCamelException(e);
            }
        }

        try {
            Map<String, List<String>> errorResponseCodeMappings = Optional.ofNullable(options.remove(HTTP_ERROR_RESPONSE_CODES_PROPERTY))
                    .map(Object::toString)
                    .map(ApiProviderReturnPathCustomizer::extractMappings)
                    .orElse(Collections.emptyMap());

            consumeOption(this.context, options, HTTP_RESPONSE_CODE_PROPERTY, Integer.class, code ->
                component.setAfterProducer(statusCodeUpdater(code, errorResponseCodeMappings))
            );
        } catch (@SuppressWarnings("PMD.AvoidCatchingGenericException") Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Map<String, List<String>> extractMappings(String property) {
        try {
            if (ObjectHelper.isEmpty(property)) {
                return Collections.emptyMap();
            }

            return Json.reader().forType(new TypeReference<Map<String, List<String>>>(){}).readValue(property);
        } catch (IOException e) {
            LOG.warn(String.format("Failed to read error code mapping property %s: %s", property, e.getMessage()), e);
            return Collections.emptyMap();
        }
    }

    private Processor statusCodeUpdater(Integer responseCode, Map<String, List<String>> errorResponseCodeMappings) {
        return exchange -> {
            if (exchange.getException() != null) {
                int errorResponseCode = 500;
                if (exchange.getException() instanceof SyndesisConnectorException) {
                    for (Map.Entry<String, List<String>> mapping : errorResponseCodeMappings.entrySet()) {
                        if (mapping.getValue().stream().anyMatch(((SyndesisConnectorException) exchange.getException()).getCategory()::equals)) {
                            errorResponseCode = Integer.valueOf(mapping.getKey());
                            break;
                        }
                    }
                }

                exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, errorResponseCode);
            } else if (responseCode != null) {
                // Let's not override the return code in case of exceptions in the route execution
                exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);
            }
        };
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.context = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return this.context;
    }

    @Override
    public void setInputDataShape(DataShape dataShape) {
        this.inputDataShape = dataShape;
    }

    @Override
    public DataShape getInputDataShape() {
        return this.inputDataShape;
    }

    @Override
    public void setOutputDataShape(DataShape dataShape) {
        this.outputDataShape = dataShape;
    }

    @Override
    public DataShape getOutputDataShape() {
        return this.outputDataShape;
    }
}
