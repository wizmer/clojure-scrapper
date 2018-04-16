(ns clojure-scrapper.homegate
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s]
            [hiccup.table]
            [clojure-scrapper.common :refer [fetch-url]]
            [hiccup.core]
            [clj-http.client :as client]
            [cemerick.url :as url]))


(defn homegate-url [max-price min-surface]
  (str "https://www.homegate.ch/louer/appartement/lieu-geneve/liste-annonces?ag=1000&ah="
       max-price
       "&ak="
       min-surface
       "&tab=list&o=sortToplisting-desc"))

(defn get-urls [max-price min-surface]
  (let [format-fun (fn [data]
                     (str "https://www.homegate.ch"
                          (get-in data [:attrs :href])))]
    (map format-fun (html/select (fetch-url (homegate-url max-price min-surface)) [:.detail-page-link]))))


(defn get-price [item]
  (let [select (html/select item [:.detail-price (html/attr= :itemprop "price")])
        content (first (:content (first select)))]
    (read-string (clojure.string/replace content #"–|'" ""))))
(map get-price )

(defn get-surface [item]
  (let [raw (first (filter #(clojure.string/ends-with? % " m2")(map html/text (html/select item [:.detail-key-data :li :.text--shout]))))]
        (read-string (apply str (drop-last 3 raw)))))

(defn get-address [item]
  (first (:content (first (html/select item [:.detail-address :h2])))))

(defn get-type [item]
  nil)

(defn get-images [item]
  nil)
(defn get-raw-data [item]
  {:surface (get-surface item)
   :price (get-price item)
   :raw-address (get-address item)
   :type (get-type item)
   :images (get-images item)})
