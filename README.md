# Hazelcastdw [![Travis build status](https://travis-ci.org/koushikr/hazelcast-dw.svg?branch=master)](https://travis-ci.org/koushikr/hazelcast-dw)

> We are both perpetrators and victims of our biases
> - by Koushik Ramachandra

This is for using hazelcast over marathon within application paradigm without setting your hair on fire about the internal client details.

### Build instructions
  - Clone the source:

        git clone github.com/koushikr/hazelcast-dw

  - Build

        mvn install

### Using the bundle

#### 1. Defining your own caches
```java
    class MyCache extends Cache<String, String>{
    
        protected MyCache(String cacheName, HazelcastMapConfig hcMapConfig, Class<String> valueClass, MapLoader<String, String> mapLoader) {
            super(cacheName, hcMapConfig, valueClass, mapLoader);
        }
    }

```

#### 1. Annotate with the cacheMeta
```java
       @io.github.hazelcastdw.cache.CacheMeta(cacheName = "CACHE_1") 
       class MyCache extends Cache<String, String>{
        
            protected MyCache(String cacheName, HazelcastMapConfig hcMapConfig, Class<String> valueClass, MapLoader<String, String> mapLoader) {
                super(cacheName, hcMapConfig, valueClass, mapLoader);
            }
        }
```

#### 2. Define your serviceInjector (service loader, service finder, injector)
```java
       class MyServiceInjector implements io.github.hazelcastdw.cache.ServiceInjector{
            
            @javax.inject.Inject Injector injector;
            
            public Object getInstance(Class<?> klass){
                return injector.getInstance(klass);
            }
       }
```

#### 4. Bootstrap
```java
    HazelcastBundle<T> hazelcastBundle = new HazelcastBundle<T>() {
               @Override
               public CacheConfig getCacheConfig(T configuration) {
                   return cacheConfig;
               }
   
               @Override
               public ServiceInjector getServiceInjector(T configuration) {
                   return myServiceInjector;
               }
   
               @Override
               public String[] getHandlerPackages(T configuration) {
                   return new String[0];
               }
           }  
            
    @Override
    public void initialize(final Bootstrap...) {
        bootstrap.addBundle(hazelcastBundle);
    }
```

### Maven Dependency
Use the following repository:
```xml
<repository>
    <id>clojars</id>
    <name>Clojars repository</name>
    <url>https://clojars.org/repo</url>
</repository>
```
Use the following maven dependency
```xml
<dependency>
    <groupId>io.github.hazelcastdw</groupId>
    <artifactId>hazelcastdw</artifactId>
    <version>2.0.15-1</version>
</dependency>
```

### Version support
| dropwizard               
| -----------------------
| 2.0.15                           

### Configuration
```yaml
cacheConfig:
  marathonEndpoint: http://example.marathon.int:8080
  appId: test-app
  portIndex:  2
  localMode: true
  mapConfigs:
    CACHE_1:
      ttl:  600
      maxKeyCount:  5000
      nearCacheEnabled: true
      nearCacheTTL: 60
    CACHE_2:
      ttl:  600
      maxKeyCount:  5000
      nearCacheEnabled:
```

Contributors
------------
* [Koushik R](https://github.com/koushikr) 

LICENSE
-------

Copyright 2020 Koushik R <rkoushik.14@gmail.com>.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
