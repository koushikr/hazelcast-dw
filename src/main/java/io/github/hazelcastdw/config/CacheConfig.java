package io.github.hazelcastdw.config;/*
 * Copyright 2020 Koushik R <rkoushik.14@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheConfig {

    private String name;
    private String marathonEndpoint;
    private String appId;
    private int portIndex;
    private boolean localMode;

    private Map<String, HazelcastMapConfig> mapConfigs = Maps.newHashMap();

    @JsonIgnore
    public HazelcastMapConfig getMapConfig(String cacheName) {
        if (mapConfigs.containsKey(cacheName)) {
            return mapConfigs.get(cacheName);
        }

        return HazelcastMapConfig.builder()
                .ttl(600)
                .maxKeyCount(5000)
                .nearCacheEnabled(true)
                .nearCacheTTL(60)
                .build();

    }
}
