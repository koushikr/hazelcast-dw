package io.github.hazelcastdw;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.github.hazelcastdw.cache.CacheManager;
import io.github.hazelcastdw.cache.MapperUtils;
import io.github.hazelcastdw.cache.ServiceInjector;
import io.github.hazelcastdw.config.CacheConfig;
import lombok.NoArgsConstructor;

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
@NoArgsConstructor
public abstract class HazelcastBundle<T extends Configuration> implements ConfiguredBundle<T> {

    public abstract CacheConfig getCacheConfig(T configuration);

    /**
     * if you are wondering, How should I use Guice (or any other injector) inside my bundle?
     * Well, don't! The accidental complexity of an injector inside a cohesive bundle is not balanced by reduced overall complexity.
     * Long answer, read here : https://blog.osgi.org/2014/09/how-should-i-use-guicespringblueprint.html
     */
    public abstract ServiceInjector getServiceInjector(T configuration);

    public abstract String[] getHandlerPackages(T configuration);

    @Override
    public void run(T configuration, Environment environment) {
        environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        environment.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        environment.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        environment.getObjectMapper().configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        environment.getObjectMapper().configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);

        MapperUtils.init(environment.getObjectMapper());

        environment.lifecycle().manage(new CacheManager(
                getHandlerPackages(configuration),
                getCacheConfig(configuration),
                getServiceInjector(configuration)
        ));
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {

    }


}
