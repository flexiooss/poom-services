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