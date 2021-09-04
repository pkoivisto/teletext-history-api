(ns teletext-history-api.image-downloader
  (:require [clj-http.client :as http]))

(defn- image-url [page subpage]
  (str "https://external.api.yle.fi/v1/teletext/images/" page "/" subpage ".png"))

(defn- page-url [page]
  (str "https://external.api.yle.fi/v1/teletext/pages/" page ".json"))

(defn- get-image [{:keys [page subpage app_id app_key]}]
  {:pre [(string? page)
         (string? subpage)
         (string? app_id)
         (string? app_key)]}
  (let [url (image-url page subpage)
        opts {:query-params {:app_id  app_id
                             :app_key app_key}
              :as           :byte-array}
        response (http/get url opts)]
    (case (:status response)
      200 (:body response)
      nil)))

(defn- get-page [{:keys [page app_id app_key]}]
  {:pre [(string? page)
         (string? app_id)
         (string? app_key)]}
  (let [url (page-url page)
        opts {:query-params {:app_id  app_id
                             :app_key app_key}}
        response (http/get url opts)]
    (case (:status response)
      200 (:body response)
      nil)))

(defprotocol APIClient
  (get-image-png [this page subpage])
  (get-page-json [this page]))

(defrecord APIClientImpl
  [app_key app_id]
  APIClient
  (get-image-png [_ page subpage]
    (get-image {:app_key app_key
                :app_id  app_id
                :page    page
                :subpage subpage}))
  (get-page-json [_ page]
    (get-page {:app_key app_key
               :app_id  app_id
               :page    page})))

