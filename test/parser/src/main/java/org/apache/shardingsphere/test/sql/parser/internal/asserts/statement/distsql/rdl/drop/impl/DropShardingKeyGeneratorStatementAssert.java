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

package org.apache.shardingsphere.test.sql.parser.internal.asserts.statement.distsql.rdl.drop.impl;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.sharding.distsql.parser.statement.DropShardingKeyGeneratorStatement;
import org.apache.shardingsphere.test.sql.parser.internal.asserts.SQLCaseAssertContext;
import org.apache.shardingsphere.test.sql.parser.internal.cases.parser.domain.statement.distsql.rdl.drop.DropShardingKeyGeneratorStatementTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Drop sharding key generator statement assert.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DropShardingKeyGeneratorStatementAssert {
    
    /**
     * Assert drop sharding key generator statement is correct with expected parser result.
     *
     * @param assertContext assert context
     * @param actual actual drop sharding key generator statement
     * @param expected expected drop sharding key generator statement test case
     */
    public static void assertIs(final SQLCaseAssertContext assertContext, final DropShardingKeyGeneratorStatement actual, final DropShardingKeyGeneratorStatementTestCase expected) {
        if (null == expected.getKeyGeneratorName()) {
            assertNull(assertContext.getText("Actual key generator name should not exist."), actual);
        } else {
            assertThat(assertContext.getText("Key generator names assertion error: "), actual.getKeyGeneratorNames(), is(expected.getKeyGeneratorName()));
            assertThat(assertContext.getText("Contains exist clause assertion error: "), actual.isIfExists(), is(expected.isIfExists()));
        }
    }
}
