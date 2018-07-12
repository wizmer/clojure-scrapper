(ns clojure-scrapper.leboncoin
  (:require [clojure-scrapper.common :refer [fetch-url]]
            [net.cgrand.enlive-html :as html]
            [clojure.data.json :as json]
            [clojure.string :as s]))

(def db (atom {:all {}
               :items {}}))


(defn main-page-data []
  (if (empty? (@db :all))
    (swap! db #(assoc %
                      :all
                      (fetch-url "https://www.leboncoin.fr/recherche/?category=9&region=22&cities=Ferney-Voltaire_01210&real_estate_type=1,2&square=40-max&price=min-275000")))
    (do (println "from cache") @db)))

(defn get-urls []
  (let [end-urls (map #(get-in % [:attrs :href])  (html/select ((main-page-data) :all) [(html/attr? :itemscope) :a ]))]
		(map #(str "https://www.leboncoin.fr" %) end-urls)))


(defn get-json-data [item]
  (let [scripts (html/select item [:script])
        script-with-content (filter #(not (nil? (:content %))) scripts)
        window-script (filter #(s/starts-with? (first (:content %)) "window.FLUX_STATE") script-with-content)
        content (first (:content (first window-script)))
        json-data (second (s/split content #"=" 2))]
    (json/read-str json-data )
    ))



(defn get-raw-data [item]
  (let [data (get-json-data item)
				attributes (get-in data  ["adview" "attributes"])
        extract {"body" ["adview" "body"]
                 "price" ["adview" "price"]
								 "url" ["adview" "owner" "url"]
								 "thumb_url" ["adview" "images" "thumb_url"]
								 "images" ["adview" "images" "urls"]}
        info (into {} (for [[k v] extract] [k (get-in data v)]))
        info2 (into {} (map #(vector (% "key_label") (% "value_label")) attributes))
				all-info(merge info info2)
        ]

		{:price (first (all-info "price"))
		 :surface (try (read-string (s/join "" (drop-last 3 (all-info "Surface"))))
									 (catch Exception e nil))
		 :images (all-info "images")
		 :type "maison"}
    )
	)
