(ns teletext-history-api.storage-test
    (:require [clojure.test :refer :all]
              [teletext-history-api.storage :refer [fetch store ->FilesystemImageCache]])
  (:import (java.time Instant)
           (java.time.temporal ChronoUnit)
           (java.io File)))

(defn- instant+offset->epoch-milli [^Instant inst offset unit]
  (-> inst
      (.plus offset unit)
      (.toEpochMilli)
      (str)))

(defn- bytes->string [^bytes bs]
  (String. bs))

(deftest filesystem-image-cache
  (let [cache-dir (File. "/tmp/test-teletext-history-api")
        cache (->FilesystemImageCache "/tmp/test-teletext-history-api")
        now (Instant/now)
        now+1h (instant+offset->epoch-milli now 1 ChronoUnit/HOURS)
        now+2h (instant+offset->epoch-milli now 2 ChronoUnit/HOURS)
        page "100"
        subpage "13"
        time->content {now+1h "one hour from now, the world will be different"
                       now+2h "two hours from now, who knows?"}]
    (try
      ; Clean up: remove cache-dir and all directories and files within

      (store cache page subpage now+1h (.getBytes (time->content now+1h)))
      (store cache page subpage now+2h (.getBytes (time->content now+2h)))

      (testing "Lookups for a page and subpage with no stored contents returns nil"
        (is (nil? (fetch cache "this page" "is not present" (instant+offset->epoch-milli now 0 ChronoUnit/HOURS))))
        (is (nil? (fetch cache "this page" "is not present" (instant+offset->epoch-milli now -10 ChronoUnit/HOURS))))
        (is (nil? (fetch cache "this page" "is not present" (instant+offset->epoch-milli now 10 ChronoUnit/HOURS)))))

      (testing "Lookups for stored content return the right results"
        (testing "No content stored for time -> nil"
          (is (nil? (fetch cache page subpage (instant+offset->epoch-milli now 0 ChronoUnit/HOURS))))
          (is (nil? (fetch cache page subpage (instant+offset->epoch-milli now -10 ChronoUnit/MINUTES))))
          (is (nil? (fetch cache page subpage (instant+offset->epoch-milli now 10 ChronoUnit/MINUTES))))
          (is (nil? (fetch cache page subpage (instant+offset->epoch-milli now 59 ChronoUnit/MINUTES)))))
        (testing "Time is at least first timestamp stored but less than second -> return contents corresponding to first timestamp"
          (let [content-1h (fetch cache page subpage (instant+offset->epoch-milli now 60 ChronoUnit/MINUTES))]
            (is (= (time->content now+1h) (bytes->string content-1h)))
            (is (= (time->content now+1h)
                   (bytes->string (fetch cache page subpage (instant+offset->epoch-milli now 61 ChronoUnit/MINUTES)))
                   (bytes->string (fetch cache page subpage (instant+offset->epoch-milli now 90 ChronoUnit/MINUTES)))
                   (bytes->string (fetch cache page subpage (instant+offset->epoch-milli now 119 ChronoUnit/MINUTES)))))))
        (testing "Time is after last stored timestamp -> return last stored contents"
          (is (= (time->content now+2h)
                 (bytes->string (fetch cache page subpage (instant+offset->epoch-milli now 120 ChronoUnit/MINUTES)))
                 (bytes->string (fetch cache page subpage (instant+offset->epoch-milli now 3 ChronoUnit/HOURS)))
                 (bytes->string (fetch cache page subpage (instant+offset->epoch-milli now 10 ChronoUnit/DAYS)))))))
      (finally
        (doseq [f (->> (file-seq cache-dir)
                       (reverse))]
          (.delete f))))))
