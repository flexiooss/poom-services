# Caches and Caches management

## Rationale

**poom-caches-api** defines a generic logic for accessing / subscribing to caches through the 
`org.codingmatters.poom.caches.Cache` interface.

**poom-caches-management** defines a management entry point for caches through the 
`org.codingmatters.poom.caches.management.CacheManager` class :

 * the static constructors enables to create caches with different backup stores 
 * the most straight forward is to use the 
 `org.codingmatters.poom.caches.management.CacheManager.newHashMapBackedCache` static constructor
 * caches use an LRU logic to keep the size of the cache *near* the capacity
 * value invalidity is decided upon access 
 
In general, the business code should only depend upon **poom-caches-api**. Only top level code (i.e. service
defining code, near a main method) should depend upon **poom-caches-management**.