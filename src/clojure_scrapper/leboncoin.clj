(ns clojure-scrapper.leboncoin
  (:require [clojure-scrapper.common :refer [fetch-url]]
            [net.cgrand.enlive-html :as html]
            [clojure.string :as s]))

;; (def db (atom {:all {}
;;                :items {}}))

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

(defn find-script []
  (let [scripts (html/select (val (first (@db :items))) [:script])]
    (filter #(and (not (nil? (:content %)))
                 (s/starts-with? (:content (first (html/select (val (first (@db :items))) [:script]))) "window")
                 )

            scripts)))

(get-item)
