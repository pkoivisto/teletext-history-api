(ns teletext-history-api.history-service
  (:require [clj-http.client :as http]))

(defn- page+subpage->url [page subpage]
  (str "https://external.api.yle.fi/v1/teletext/images/" page "/" subpage ".png"))

(defn get-history-page [{:keys [page subpage timestamp app_id app_key] :as m}]
  {:pre [(string? page)
         (string? subpage)
         (string? app_id)
         (string? app_key)]}
  (clojure.pprint/pprint m)
  (let [url-to-get (page+subpage->url page subpage)
        opts {:query-params {:app_id  app_id
                             :app_key app_key}}]
    (http/get url-to-get opts)))