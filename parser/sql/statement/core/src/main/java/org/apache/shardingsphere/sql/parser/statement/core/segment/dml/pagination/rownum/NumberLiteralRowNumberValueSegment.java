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

package org.apache.shardingsphere.sql.parser.statement.core.segment.dml.pagination.rownum;

import lombok.Getter;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.pagination.NumberLiteralPaginationValueSegment;

/**
 * Row number value segment for number literal.
 */
@Getter
public final class NumberLiteralRowNumberValueSegment extends RowNumberValueSegment implements NumberLiteralPaginationValueSegment {
    
    private final Long value;
    
    public NumberLiteralRowNumberValueSegment(final int startIndex, final int stopIndex, final Long value, final boolean boundOpened) {
        super(startIndex, stopIndex, boundOpened);
        this.value = value;
    }
}
