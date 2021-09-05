(ns teletext-history-api.repl
  (:require [teletext-history-api.main :refer [start-jetty! secrets]]
            [teletext-history-api.storage :refer [->FilesystemImageCache]]
            [teletext-history-api.api-client :refer [->APIClientImpl]]
            [teletext-history-api.image-downloader :refer [->DownloadSchedulerImpl
                                                           start-download-schedule!]]))

(def server (atom nil))
(def downloader (atom nil))

(defn stop-server! []
  (assert (some? @server))
  (.stop @server)
  (reset! server nil))

(defn start-server! [cache]
  (assert (nil? @server))
  (let [jetty (start-jetty! cache {:port 8080, :join? false})]
    (reset! server jetty)))

(defn stop-downloader! []
  (assert (some? @downloader))
  (.interrupt @downloader)
  (reset! downloader nil))

(defn start-downloader! [cache]
  (assert (nil? @downloader))
  (let [api-client (->APIClientImpl (:app_key secrets) (:app_id secrets))
        download-scheduler (->DownloadSchedulerImpl cache api-client (atom {}))
        downloader-thread (Thread. (reify Runnable
                                     (run [_]
                                       (start-download-schedule! download-scheduler))))]
    (.start downloader-thread)
    (reset! downloader downloader-thread)))

(comment
  (def cache (->FilesystemImageCache "/tmp/teletext-history-api"))
  ; Evaluate to start serving content from cache.
  (start-server! cache)

  ; Evaluate to stop serving content.
  (stop-server!)

  ; Evaluate to start downloading content and populating cache.
  (start-downloader! cache)

  ; Evaluate to stop downloading new content to cache.
  (stop-downloader!)

  )
