(ns clojure-scrapper.leboncoin
  (:require [clojure-scrapper.common :refer [fetch-url]]
            [net.cgrand.enlive-html :as html]
            [clojure.data.json :as json]
            [clojure.string :as s]))


(defn gogo []
  (if (empty? (@db :all))
    (swap! db #(assoc %
                      :all
                      (fetch-url "https://www.leboncoin.fr/recherche/?category=9&region=22&cities=Ferney-Voltaire_01210&real_estate_type=1,2&square=40-max&price=min-275000")))
    (do (println "from cache") @db)))

(defn get-item []
  (let [end-urls (map #(get-in % [:attrs :href])  (html/select ((gogo) :all) [(html/attr? :itemscope) :a ]))
        urls (map #(str "https://www.leboncoin.fr" %) end-urls)
        new-urls (filter #(not (contains? (:items @db) %)) urls)
        new-items   (map fetch-url new-urls)]
    (doall (map (fn [url item] (println "new-url: " url)(swap! db #(assoc-in % [:items url] item)))
         new-urls new-items)))
  (println "done"))

(defn get-json-data [item]
  (let [scripts (html/select (val item) [:script])
        script-with-content (filter #(not (nil? (:content %))) scripts)
        window-script (filter #(s/starts-with? (first (:content %)) "window.FLUX_STATE") script-with-content)
        content (first (:content (first window-script)))
        json-data (second (s/split content #"=" 2))]
    (json/read-str json-data )
    ))


;; (def db (atom {:all {}
;;                :items {}}))
;; (get-item)



(defn extract-pertinent-info [data]
  (let [attributes (get-in data  ["adview" "attributes"])
        extract {"body" ["adview" "body"]
                 "price" ["adview" "price"]}
        info (into {} (for [[k v] extract] [k (get-in data v)]))
        info2 (into {} (map #(vector (% "key_label") (% "value_label")) attributes))
        ]
    (merge info info2)
    ))

(extract-pertinent-info (get-json-data  (first (@db :items))))
;; (map find-script  (@db :items))
