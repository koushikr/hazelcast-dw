package io.github.hazelcastdw.cache;/*
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
import com.hazelcast.core.HazelcastInstance;
import com.marathon.hazelcast.servicediscovery.MarathonDiscoveryStrategyFactory;
import io.dropwizard.lifecycle.Managed;
import io.github.hazelcastdw.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.reflections.Reflections;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class CacheManager implements Managed{

    private final ServiceInjector serviceInjector;
    private final CacheConfig cacheConfig;
    private Map<String, Cache> handlers;

    public CacheManager(String[] handlerPackages, final CacheConfig cacheConfig,
                            final ServiceInjector serviceInjector) {
        this.handlers = new HashMap<>();
        this.cacheConfig = cacheConfig;
        this.serviceInjector = serviceInjector;

        Arrays.stream(handlerPackages).forEach(this::registerCache);
    }

    private void registerCache(final String handlerPackage) {
        final Reflections reflections = new Reflections(handlerPackage);
        final Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(CacheMeta.class);

        typesAnnotatedWith.forEach(klass -> {
            final CacheMeta annotation = klass.getAnnotation(CacheMeta.class);
            final Cache cache = (Cache) this.serviceInjector.getInstance(klass);
            this.handlers.put(annotation.cacheName().toUpperCase(), cache);

            log.info("{} registered handler: {} for {}. ", this.getClass().getName(), klass.getName(),
                    annotation.cacheName().getClass().getName());
        });
    }

    private void initialize(final Config config) {
        val appId = this.cacheConfig.getAppId().replace("/", "").trim();
        log.info("Application Id: " + appId);
        config.setClusterName(cacheConfig.getName());
        config.setProperty("hazelcast.discovery.enabled", "true");
        config.setProperty("hazelcast.discovery.public.ip.enabled", "true");
        config.setProperty("hazelcast.socket.client.bind.any", "true");
        config.setProperty("hazelcast.socket.bind.any", "true");
        final NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPublicAddress(System.getenv("HOST") + ":" + System.getenv("PORT_5701"));

        final JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getTcpIpConfig().setEnabled(false);
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getAwsConfig().setEnabled(false);
        final DiscoveryConfig discoveryConfig = joinConfig.getDiscoveryConfig();
        final DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(
                new MarathonDiscoveryStrategyFactory());
        discoveryStrategyConfig.addProperty("marathon-endpoint", this.cacheConfig.getMarathonEndpoint());
        discoveryStrategyConfig.addProperty("app-id", appId);
        discoveryStrategyConfig.addProperty("port-index", Integer.toString(this.cacheConfig.getPortIndex()));
        discoveryConfig.addDiscoveryStrategyConfig(discoveryStrategyConfig);
    }

    @Override
    public void start() throws Exception {
        log.info("Starting cache manager");

        final Config config = new Config();
        if (!this.cacheConfig.isLocalMode()) {
            initialize(config);
        }

        this.handlers.values()
                .forEach(cache -> cache.loadConfig(config));

        log.info("Hazelcast config: {}", config);
        final HazelcastInstance instance = com.hazelcast.core.Hazelcast.newHazelcastInstance(config);

        this.handlers.values()
                .forEach(cache -> cache.initialize(instance));

        log.info("Started Cache Manager successfully");
    }

    @Override
    public void stop() {
        log.info("Stopping the cache manager");
    }


    @SuppressWarnings("unchecked")
    private <K, V> Cache<K, V> cache(final String cacheName) {
        return this.handlers.get(cacheName.toUpperCase());
    }

}
