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
package io.syndesis.common.util.cache;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class LRUCacheManagerTest {

    @ParameterizedTest(name = "LRUCacheManagerTest(soft={0})")
    @ValueSource(booleans = {true, false})
    public void testEviction(final boolean soft) {
        CacheManager manager = new LRUCacheManager(2);
        Cache<String, Object> cache = manager.getCache("cache", soft);

        String one = "1";
        String two = "2";
        String three = "3";

        cache.put(one, one);
        cache.put(two, two);
        cache.put(three, three);

        assertThat(cache.size()).isEqualTo(2);
        assertThat(cache.get(one)).isNull();
        assertThat(cache.get(two)).isNotNull();
        assertThat(cache.get(three)).isNotNull();
    }

    @ParameterizedTest(name = "LRUCacheManagerTest(soft={0})")
    @ValueSource(booleans = {true, false})
    public void testIdentity(final boolean soft) {
        CacheManager manager = new LRUCacheManager(2);
        Cache<String, String> cache1 = manager.getCache("cache", soft);
        Cache<String, String> cache2 = manager.getCache("cache", soft);
        // same cache, but warning printed
        Cache<String, String> cache3 = manager.getCache("cache", !soft);

        assertThat(cache1).isEqualTo(cache2);
        assertThat(cache1).isEqualTo(cache3);
    }
}
