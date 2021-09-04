(ns teletext-history-api.main
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [reitit.ring :as ring]
            [reitit.core :as r]
            [teletext-history-api.image-downloader :refer [get-image]]
            [clojure.string :as s]
            [clojure.walk :refer [keywordize-keys]]
            [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [teletext-history-api.storage :refer [store fetch ->FilesystemImageCache]]))

(defn image-response [handler]
  (fn [request]
    (let [response (handler request)]
      {:body    (with-out-str (clojure.pprint/pprint response))
       :headers {"Content-Type" "application/edn"}
       :status  200})))

(defn- query-string->map [query-string]
  (if (some? query-string)
    (->> (for [query-part (s/split query-string #"&")]
           (s/split query-part #"="))
         (into {}))
    {}))

(defn- routes []
  (ring/router ["/v1/:page/{subpage}.png" {:get {:handler    (fn [{:keys [path-params query-string]}]
                                                               (-> (merge
                                                                     (query-string->map query-string)
                                                                     path-params)
                                                                   (keywordize-keys)
                                                                   (get-image)))
                                                 :middleware [image-response]}}]))

(def handler
  (ring/ring-handler (routes) (ring/create-default-handler)))



(defn- main []
  (run-jetty #'handler {:port  8080
                        :join? false}))

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

  (let [cache (->FilesystemImageCache "/tmp")
        page "100"
        subpage "1"
        opts (merge {:page    page
                     :subpage subpage}
                    secrets)]
    #_(store cache page subpage "now" (.getBytes (:body (get-image opts))))
    (store cache page subpage "now" (:body (get-image opts))))

  (let [cache (->FilesystemImageCache "/tmp")
        page "100"
        subpage "1"
        opts (merge {:page    page
                     :subpage subpage}
                    secrets)]
    #_(store cache page subpage "now" (.getBytes (:body (get-image opts))))
    (store cache page subpage "now" (:body (get-image opts))))

  (store nil nil nil nil nil)
  )
