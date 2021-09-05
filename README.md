# teletext-history-api

A backend that serves historical content for the teletext pages served via Yle's (the Finnish public broadcasting company) APIs.

# Setup

First, acquire API credentials at https://tunnus.yle.fi/api-avaimet. Ensure the credentials are available for the app in `resources/secret.edn`.

The contents of the file should look like the following:
```clojure
{:app_id <YOUR_APP_ID>
 :app_key <YOUR_APP_KEY>}
```

# Usage

Invoke the function `teletext-history-api.main/-main`. 

This starts an HTTP server responding to requests at `localhost:8080/v1/<page>/<subpage>.png?time=<ms-since-epoch>`.
Simultaneously, a background process is launched to start walking through the pages and images available via Yle's teletext APIs.
The background process stores images it has fetched to the filesystem, under `/tmp/teletext-history-api/`, with the path of each stored image corresponding
to the page, subpage and publication time of the image.
