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
package io.syndesis.common.util;

public class SyndesisConnectorException extends RuntimeException {

    private final String category;

    private static final long serialVersionUID = 3476018743129184217L;

    public SyndesisConnectorException() {
        this("UNKNOWN");
    }

    public SyndesisConnectorException(String category) {
        this(category, null, null);
    }

    public SyndesisConnectorException(String category, Throwable cause) {
        this(category, cause.getMessage(), cause);
    }

    public SyndesisConnectorException(String category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
    }

    /**
     * Obtains the category.
     * @return
     */
    public String getCategory() {
        return category;
    }
}
