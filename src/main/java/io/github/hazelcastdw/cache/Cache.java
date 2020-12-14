package io.github.hazelcastdw.cache;

/*
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

import com.hazelcast.config.*;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.MapEvent;
import com.hazelcast.map.MapLoader;
import io.github.hazelcastdw.config.HazelcastMapConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Cache<K, V> {

    private final String key;
    private final MapConfig mapConfig;
    private final Class<V> valueClass;
    private IMap<K, V> cacheImpl;

    protected Cache(String cacheName,
                    HazelcastMapConfig hcMapConfig,
                    final Class<V> valueClass,
                    final MapLoader<K, V> mapLoader) {
        key = cacheName.toUpperCase();
        this.valueClass = valueClass;
        EvictionConfig evictionConfig = new EvictionConfig();
        evictionConfig.setSize(10);
        evictionConfig.setMaxSizePolicy(MaxSizePolicy.FREE_HEAP_SIZE);
        evictionConfig.setEvictionPolicy(EvictionPolicy.LRU);

        mapConfig = new MapConfig();
        mapConfig.setName(key);
        mapConfig.setTimeToLiveSeconds(hcMapConfig.getTtl());
        mapConfig.setEvictionConfig(evictionConfig);
        mapConfig.setBackupCount(1);
        mapConfig.setStatisticsEnabled(true);
        MapStoreConfig mapStoreConfig = mapConfig.getMapStoreConfig();
        mapStoreConfig.setEnabled(true);
        mapStoreConfig.setImplementation(mapLoader);
        mapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.LAZY);

        if (hcMapConfig.isDebugEntryLifecycle()) {
            EntryListenerConfig entryListenerConfig
                    = new EntryListenerConfig().setImplementation(new EntryListener() {
                @Override
                public void entryExpired(EntryEvent event) {
                    log.info("{}::Expired::{}", cacheName, event.getKey());
                }

                @Override
                public void entryAdded(EntryEvent event) {
                    log.info("{}::Added::{}", cacheName, event.getKey());
                }

                @Override
                public void entryEvicted(EntryEvent event) {
                    log.info("{}::Evicted::{}", cacheName, event.getKey());
                }

                @Override
                public void entryRemoved(EntryEvent event) {
                    log.info("{}::Removed::{}", cacheName, event.getKey());
                }

                @Override
                public void entryUpdated(EntryEvent event) {
                    log.info("{}::Updated::{}", cacheName, event.getKey());
                }

                @Override
                public void mapCleared(MapEvent event) {
                    log.info("{}::Cleared", cacheName);
                }

                @Override
                public void mapEvicted(MapEvent event) {
                    log.info("{}::All evicted", cacheName);
                }
            });
            mapConfig.getEntryListenerConfigs().add(entryListenerConfig);
        }
        if (hcMapConfig.isNearCacheEnabled()) {
            NearCacheConfig nearCacheConfig = new NearCacheConfig();
            nearCacheConfig.setTimeToLiveSeconds(hcMapConfig.getNearCacheTTL());
            evictionConfig.setSize(hcMapConfig.getMaxKeyCount());
            nearCacheConfig.setEvictionConfig(evictionConfig);
            mapConfig.setNearCacheConfig(nearCacheConfig);
            mapConfig.getNearCacheConfig().setInvalidateOnChange(true);
            mapConfig.getNearCacheConfig()
                    .setLocalUpdatePolicy(NearCacheConfig.LocalUpdatePolicy.INVALIDATE);
            mapConfig.getNearCacheConfig().getEvictionConfig().setEvictionPolicy(EvictionPolicy.LRU);
        }
        log.info("Created cache config: {}", cacheName);
    }

    public MapConfig loadConfig(Config config) {
        config.getMapConfigs().put(key, mapConfig);
        if (config.getSerializationConfig().getSerializerConfigs().stream()
                .noneMatch(serializerConfig -> serializerConfig.getTypeClass().equals(valueClass))) {
            SerializerConfig serializerConfig = new SerializerConfig();
            serializerConfig.setTypeClass(valueClass);
            serializerConfig.setImplementation(new CustomSerializer<>(valueClass));
            config.getSerializationConfig().getSerializerConfigs().add(serializerConfig);
        }
        return mapConfig;
    }

    public void initialize(HazelcastInstance hazelcastInstance) {
        cacheImpl = hazelcastInstance.getMap(key);
        log.info("{} Cache implementation is now set", key);
    }

    public V get(K key) {
        return cacheImpl.get(key);
    }

    public void put(K key, V value) {
        cacheImpl.put(key, value);
    }

    public void delete(K lookupKey) {
        cacheImpl.delete(lookupKey);
    }

}
