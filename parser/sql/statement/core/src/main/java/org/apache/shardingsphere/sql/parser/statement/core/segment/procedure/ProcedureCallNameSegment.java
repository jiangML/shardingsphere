/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.sql.parser.statement.core.segment.procedure;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.shardingsphere.sql.parser.statement.core.segment.SQLSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.packages.PackageSegment;
import org.apache.shardingsphere.sql.parser.statement.core.value.identifier.IdentifierValue;

import java.util.Optional;

/**
 * PL/SQL procedure call name segment.
 */
@RequiredArgsConstructor
@Getter
@Setter
public final class ProcedureCallNameSegment implements SQLSegment {
    
    private final int startIndex;
    
    private final int stopIndex;
    
    private final IdentifierValue identifier;
    
    private PackageSegment packageSegment;
    
    public Optional<PackageSegment> getPackageSegment() {
        return Optional.ofNullable(packageSegment);
    }
    
    @Override
    public String toString() {
        return getPackageSegment().map(optional -> optional.getIdentifier().getValueWithQuoteCharacters() + ".").orElse("") + identifier.getValueWithQuoteCharacters();
    }
}
