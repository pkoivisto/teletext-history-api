(ns teletext-history-api.main
  (:require [teletext-history-api.api-client :refer [->APIClientImpl get-image-png get-page-json]]
            [teletext-history-api.storage :refer [store fetch PageImageCache ->FilesystemImageCache]]
            [teletext-history-api.image-downloader :refer [->DownloadSchedulerImpl start-download-schedule!]]
            [ring.adapter.jetty :refer [run-jetty]]
            [reitit.ring :as ring]
            [reitit.core :as r]
            [clojure.string :as s]
            [clojure.walk :refer [keywordize-keys]]
            [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defonce secrets (-> (io/resource "secret.edn")
                     (slurp)
                     (edn/read-string)))

(defn- image-response [handler]
  (fn [request]
    (let [response (handler request)]
      {:body    response
       :headers {"Content-Type" "image/png"}
       :status  200})))

(defn- wrap-errors-as-not-found [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception _
        {:status 404}))))

(defn- wrap-nil-as-not-found [handler]
  (fn [request]
    (let [response (handler request)]
      (if (some? (:body response))
        response
        {:status 404}))))

(defn- query-string->map [query-string]
  (if (some? query-string)
    (->> (for [query-part (s/split query-string #"&")]
           (s/split query-part #"="))
         (into {}))
    {}))

(defn- routes [cache]
  (ring/router ["/v1/:page/{subpage}.png" {:get {:handler    (fn [{:keys [path-params query-string]}]
                                                               (let [{:keys [page subpage]} path-params
                                                                     {:keys [time]} (-> query-string
                                                                                        (query-string->map)
                                                                                        (keywordize-keys))]
                                                                 (fetch cache page subpage time)))
                                                 :middleware [wrap-errors-as-not-found
                                                              wrap-nil-as-not-found
                                                              image-response]}}]))

(defn- handler [cache]
  (ring/ring-handler (routes cache) (ring/create-default-handler)))

(defn -main []
  (let [cache (->FilesystemImageCache "/tmp/teletext-history-api")
        api-client (->APIClientImpl (:app_key secrets) (:app_id secrets))
        download-scheduler (->DownloadSchedulerImpl cache api-client (atom {}))
        downloader-thread (Thread. (reify Runnable
                                     (run [_]
                                       (start-download-schedule! download-scheduler))))]
    (try
      (.start downloader-thread)
      (run-jetty (handler cache)
                 {:port  8080
                  :join? true})
      (finally
        (.interrupt downloader-thread)))))

(comment
  (def server (main))
  (.stop server)

  (def secrets (-> (io/resource "secret.edn")
                   (slurp)
                   (edn/read-string)))

  (http/head "https://external.api.yle.fi/v1/teletext/pages/400.json"
             {:query-params secrets})

  (:headers (http/get "https://external.api.yle.fi/v1/teletext/images/100/4.png"
                      {:query-params secrets}))


  (http/get "http://localhost:8080/v1/100/1.png" {:query-params secrets})
  (r/match-by-path routes "/v1/moicculi/cuccaceppi?api_key=foo")

  (let [cache (->FilesystemImageCache "/tmp/teletext-history-api")
        client (->APIClientImpl (:app_key secrets) (:app_id secrets))
        page "100"
        subpage "1"
        opts (merge {:page    page
                     :subpage subpage}
                    secrets)
        blob (get-image-png client page subpage)]
    (store cache page subpage "now" blob))



  (let [page "100"
        client (->APIClientImpl (:app_key secrets) (:app_id secrets))]
    (let [res (get-page-json client page)
          page (get-in res ["teletext" "page"])]
      (Integer/parseInt (get page "subpagecount"))))

  (let [page "100"
        client (->APIClientImpl (:app_key secrets) (:app_id secrets))]
    (keys (get-page-json client page)))

  (let [cache (->FilesystemImageCache "/tmp")
        page "100"
        subpage "1"
        opts (merge {:page    page
                     :subpage subpage}
                    secrets)]
    #_(store cache page subpage "now" (.getBytes (:body (get-image opts))))
    (store cache page subpage "now" (:body (get-image opts))))


  (.toEpochMilli (.toInstant (LocalDateTime/parse "2021-09-04T21:26:12") ZoneOffset/UTC))
  )
