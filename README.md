# Listing 


## API

Every request requires a `CJSCPPUID` header, with a valid user id.

If you do not have one, you can turn off access control, by switching to the `atcm-vagrant` directory and running:

``` 
utils/accessControl off
```

However, before running any of the integration tests, ensure that you turn it back on.

``` 
utils/accessControl on
```

Finally, if you receive something like the following error, it's likely that it's already in that state; the command isn't idempotent.

``` 
{
    "outcome" => "failed",
    "failure-description" => "WFLYCTL0212: Duplicate resource [(\"system-property\" => \"uk.gov.justice.services.core.accesscontrol.disabled\")]",
    "rolled-back" => true
}

```


### Getting a single Hearing by its ID

This endpoint is primarily defined in [listing-query-api.raml](listing-query/listing-query-api/src/raml/listing-query-api.raml)

#### When one exists for that ID 

First, we'll need take the ID from one of the hearings:

``` 
curl -v -H "CJSCPPUID:441a514e-2437-4338-b72a-baa3d2f9d0be" -H  "Accept:application/vnd.listing.search.hearings+json" http://localhost:8080/listing-query-api/query/api/rest/listing/hearings/range-search\?allocated\=true |  jq '.hearings[0] | {id : .id}'
```

``` 
{
  "id": "c9b38f29-0d6e-4b47-be2b-5b2b8247089f"
}
```

Copying that ID, we can get the Hearing ...

``` 
curl -v -H "CJSCPPUID:441a514e-2437-4338-b72a-baa3d2f9d0be" -H "Accept:application/vnd.listing.search.hearing+json" http://localhost:8080/listing-query-api/query/api/rest/listing/hearings/c9b38f29-0d6e-4b47-be2b-5b2b8247089f | jq .
```

``` 
< HTTP/1.1 200 OK
< CPPID: 66c19677-0980-49e9-af03-802d4dcedaba
< X-Powered-By: Undertow/1
< Server: WildFly/10
< Content-Type: application/vnd.listing.search.hearing+json
< Content-Length: 6019
< Date: Tue, 19 Nov 2019 15:49:44 GMT
```

And [here is an example of the response](listing-query/listing-query-api/src/raml/json/listing.search.hearing.json).

#### When no Hearing exists for that ID

``` 
curl -v -H "CJSCPPUID:441a514e-2437-4338-b72a-baa3d2f9d0be" -H "Accept:application/vnd.listing.search.hearing+json" http://localhost:8080/listing-query-api/query/api/rest/listing/hearings/b471ab7b-f13f-4c4b-9017-c6b2ca24b79e | jq  
```

``` 
< HTTP/1.1 404 Not Found
< X-Powered-By: Undertow/1
< Server: WildFly/10
< Content-Length: 0
< Date: Tue, 19 Nov 2019 15:48:41 GMT
```

#### When the ID is not a valid UUID.

``` 
curl -v -H "CJSCPPUID:441a514e-2437-4338-b72a-baa3d2f9d0be" -H "Accept:application/vnd.listing.search.hearing+json" http://localhost:8080/listing-query-api/query/api/rest/listing/hearings/c9b38f29 | jq   
```

``` 
< HTTP/1.1 400 Bad Request
< X-Powered-By: Undertow/1
< Server: WildFly/10
< Content-Type: application/vnd.listing.search.hearing+json
< Content-Length: 54
< Date: Wed, 27 Nov 2019 11:01:14 GMT

{
  "error": "Please ensure that the id is a valid UUID."
}
```

### Requesting the publication of the next day's Hearing list for all of the Crown Courts

Please note it is the 'final' list for the next weekday.

As well, this can only be called by a 'system' user (presuming that access control is active).

This endpoint is defined in [listing-command-api.raml](listing-command/listing-command-api/src/raml/listing-command-api.raml)

``` 
curl -X POST \
-i \
-H "CJSCPPUID: ebc12dd6-9497-47bc-b3a0-864511612ae8" \
-H "Content-Type: application/vnd.listing.command.publish-court-lists-for-crown-courts+json" \
-d '{}' \
'http://localhost:8080/listing-service/command/api/rest/listing/publishCourtListsForCrownCourts'
```

``` 
HTTP/1.1 202 Accepted
X-Powered-By: Undertow/1
Server: WildFly/10
Content-Length: 0
Date: Tue, 03 Dec 2019 16:14:28 GMT
```