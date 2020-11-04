# ETag requests support

Generic support for etag requests (get, head, put and patch).

Spec defined in two traits :
 * GET / HEAD : https://raw.githubusercontent.com/flexiooss/poom-api-specs/1.9.0/apis/core/traits/etagged/etagged-read.raml
 * PUT / PATCH : https://raw.githubusercontent.com/flexiooss/poom-api-specs/1.9.0/apis/core/traits/etagged/etagged-change.raml
 
## Usage

Import module poom-etag-support-handlers and simply wrap your handlers with :
 * **ETaggedRead** : for GET and HEAD
 * **ETaggedChange** : for PUT and PATCH

Example :

```java
    new TestApiHandlers.Builder()
                .resourceGetHandler(new ETaggedRead<>(this.etags, "must-revalidate", getHandler, ResourceGetResponse.class))
                .resourcePutHandler(new ETaggedChange<>(this.etags, "must-revalidate", putHandler, ResourcePutResponse.class))
                .build();
```

The wrapped handler is responsible for calculating the etag and optionally set the cache-control.

## Etag calculation hint

Etags can typically be calculated with the org.codingmatters.poom.services.support.hash.HashProcessor class providing
the hashed value as key/value pairs or map. 

For a value object, the toMap method will get you the hash material :

```java
    String etag = new HashProcessor().hash(HashMaterial.create().with(value.toMap()))
```

or, even more consisely :

```java
    String etag = new HashProcessor().hash(value.toMap())
```
