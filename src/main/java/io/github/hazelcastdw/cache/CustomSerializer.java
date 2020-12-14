package io.github.hazelcastdw.cache;

import com.hazelcast.nio.serialization.ByteArraySerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

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
@Slf4j
public class CustomSerializer<T> implements ByteArraySerializer<T>{

    private static int typeIdCounter = 1;

    private final int typeId;
    private final Class<T> clazz;

    public CustomSerializer(Class<T> clazz) {
        this.typeId = CustomSerializer.genTypeId();
        this.clazz = clazz;
        log.info("Creating serializer for {} with ID: {}", clazz, typeId);
    }

    private static synchronized int genTypeId() {
        return typeIdCounter++;
    }

    @Override
    public byte[] write(T object) throws IOException {
        return MapperUtils.mapper().writeValueAsBytes(object);
    }

    @Override
    public T read(byte[] buffer) throws IOException {
        return MapperUtils.mapper().readValue(buffer, clazz);
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public void destroy() {
        //not required
    }
}
