(ns teletext-history-api.storage
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io File ByteArrayOutputStream)))

(defprotocol PageImageCache
  (fetch
    [this page subpage time]
    "Fetches image for given page and subpage corresponding to point in time given.
     Return either an array of bytes corresponding to the image or nil if image corresponding
     to given arguments is not found.")
  (store
    [this page subpage time ^bytes blob]
    "Stores the blob as an entry corresponding to the combination of page, subpage and time given.
     Returns nil."))

(defrecord FilesystemImageCache [root-dir]
  PageImageCache
  (fetch [_ page subpage time]
    (let [match->time (fn [^File match]
                        (let [[name _] (-> (.getName match)
                                             (str/split #"\."))]
                          (Long/parseLong name)))
          stored-images (filter #(.isFile ^File %) (file-seq (io/file root-dir page subpage)))
          time-as-long (Long/parseLong time)
          match (->> (sort-by match->time stored-images)
                     (take-while #(<= (match->time %) time-as-long))
                     (last))
          file->bytes (fn [^File file]
                        (with-open [in (io/input-stream file)
                                    out (ByteArrayOutputStream.)]
                          (io/copy in out)
                          (.toByteArray out)))]
      (when match
        (file->bytes match))))
  (store [_ page subpage time blob]
    (let [file (io/file root-dir page subpage (str time ".png"))
          _ (io/make-parents file)]
      (io/copy blob file))))

(comment
  (let [cache (->FilesystemImageCache "/tmp/yle")])


  (sort-by #(.getName ^File %) (filter #(.isFile ^File %) (file-seq (io/file "/tmp" "100" "1"))))
  (io/make-parents (io/file "/tmp/yle/dev" "teletext-history-api" "moiccu"))

  (->FilesystemImageCache "/tmp")
  (compare "2021-03-15" "2021-03-16"))