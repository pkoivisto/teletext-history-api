(ns teletext-history-api.logging
  (:require [clojure.tools.logging :refer [log]]
            [clojure.string :as str]))

(defn error [e & msg-parts]
  (log :error e (str/join " " msg-parts)))