(ns teletext-history-api.image-downloader
  (:require [teletext-history-api.storage :refer [store]]
            [teletext-history-api.api-client :refer [get-page-json get-image-png]])
  (:import [java.time LocalDateTime ZoneOffset]))

(defprotocol DownloadScheduler
  (schedule-page-download [this page])
  (schedule-image-download [this page subpage time]))


; The API docs state that the rate of requests may not exceed 10/s.
; Thus, wait for 100ms between requests.
(def ^:private interval-ms 100)

; The teletext pages start at index 100.
(def ^:private start-page "100")

(defn- date-time->epoch-millis [^String date-time]
  (some-> date-time
          (LocalDateTime/parse)
          (.toInstant ZoneOffset/UTC)
          (.toEpochMilli)))

(defrecord DownloadSchedulerImpl
  [cache api-client page->latest-fetched]
  DownloadScheduler
  (schedule-page-download [this page]
    (Thread/sleep interval-ms)
    (let [page-data (-> (get-page-json api-client page)
                        (get-in ["teletext" "page"]))
          page-time (-> (get page-data "time")
                        (date-time->epoch-millis)
                        (str))
          next-page (get page-data "nextpg")
          subpage-count (some-> (get page-data "subpagecount")
                                (Integer/parseInt))]
      (when-not (= page-time (get @page->latest-fetched page))
        (swap! page->latest-fetched assoc page page-time)
        (doseq [subpage (map str (range 1 (inc subpage-count)))]
          (schedule-image-download this page subpage page-time)))
      (if next-page
        (schedule-page-download this next-page)
        (schedule-page-download this start-page))))
  (schedule-image-download [_ page subpage time]
    (Thread/sleep interval-ms)
    (let [blob (get-image-png api-client page subpage)]
      (store cache page subpage time blob))))

(defn start-download-schedule! [scheduler]
  (schedule-page-download scheduler start-page))
