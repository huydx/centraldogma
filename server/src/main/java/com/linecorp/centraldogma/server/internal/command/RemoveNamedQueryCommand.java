/*
 * Copyright 2017 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.centraldogma.server.internal.command;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

public final class RemoveNamedQueryCommand extends ProjectCommand<Void> {

    private final String queryName;

    @JsonCreator
    RemoveNamedQueryCommand(@JsonProperty("projectName") String projectName,
                            @JsonProperty("queryName") String queryName) {
        super(CommandType.REMOVE_NAMED_QUERY, projectName);
        this.queryName = requireNonNull(queryName, "queryName");
    }

    @JsonProperty
    public String queryName() {
        return queryName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof RemoveNamedQueryCommand)) {
            return false;
        }

        final RemoveNamedQueryCommand that = (RemoveNamedQueryCommand) obj;
        return super.equals(obj) &&
               queryName.equals(that.queryName);
    }

    @Override
    public int hashCode() {
        return queryName.hashCode() * 31 + super.hashCode();
    }

    @Override
    ToStringHelper toStringHelper() {
        return super.toStringHelper().add("queryName", queryName);
    }
}
