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

package org.apache.shardingsphere.encrypt.metadata.reviser.column;

import org.apache.shardingsphere.encrypt.rule.table.EncryptTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EncryptColumnExistedReviserTest {
    
    @Test
    void assertIsExistedWithCipherColumn() {
        EncryptTable encryptTable = mock(EncryptTable.class);
        when(encryptTable.isCipherColumn("cipher_column")).thenReturn(true);
        EncryptColumnExistedReviser reviser = new EncryptColumnExistedReviser(encryptTable);
        assertTrue(reviser.isExisted("cipher_column"));
    }
    
    @Test
    void assertIsExistedWithAssistedQueryColumn() {
        EncryptTable encryptTable = mock(EncryptTable.class);
        when(encryptTable.isAssistedQueryColumn("assisted_query_column")).thenReturn(true);
        EncryptColumnExistedReviser reviser = new EncryptColumnExistedReviser(encryptTable);
        assertFalse(reviser.isExisted("assisted_query_column"));
    }
    
    @Test
    void assertIsExistedWithLikeQueryColumn() {
        EncryptTable encryptTable = mock(EncryptTable.class);
        when(encryptTable.isLikeQueryColumn("like_query_column")).thenReturn(true);
        EncryptColumnExistedReviser reviser = new EncryptColumnExistedReviser(encryptTable);
        assertFalse(reviser.isExisted("like_query_column"));
    }
    
    @Test
    void assertIsExistedWithNormalColumn() {
        EncryptTable encryptTable = mock(EncryptTable.class);
        EncryptColumnExistedReviser reviser = new EncryptColumnExistedReviser(encryptTable);
        assertTrue(reviser.isExisted("normal_column"));
    }
}
