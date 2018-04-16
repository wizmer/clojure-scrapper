(ns clojure-scrapper.anibis
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s]
            [clojure-scrapper.common :refer [fetch-url]]
            [hiccup.table]
            [hiccup.core]
            [clj-http.client :as client]
            [cemerick.url :as url]))

(def ^:dynamic *base-url* )
(defn get-price [item]
  (read-string
   (get-in (first (html/select item [(net.cgrand.enlive-html/attr= :itemprop "price")])) [:attrs :content])))


(defn get-detail [item pattern]
  (let [surface-item (first (filter
                             #(clojure.string/includes?  (html/text %) pattern)
                             (html/select item  [:div :.block])))

        surface-string (second (:content surface-item))]

    (when surface-string
      (read-string surface-string))))


(defn get-address [item]
  (let [street (first (html/texts (html/select item  [:div[:.street]])))
        city (first (html/texts (html/select item  [:div[:.city]])))]
    (s/trim (s/replace (str street " " city) #"[\t]+|[\n]+" " "))))

(defn get-urls [max-price min-surface]
  (let [base-url (str
                  "https://www.anibis.ch/fr/immobilier-immobilier-locations-gen%c3%a8ve--418/advertlist.aspx?aral=834_1200_"
                  max-price
                  ",851_"
                  min-surface
                  "_&action=filter")]
    (map  #(str "https://www.anibis.ch"
                (get-in % [:attrs :href]))
          (html/select (fetch-url base-url) [:.page-segment-item-listing :.list-item :a]) )))

(defn get-images [item]
  (map #(get-in % [:attrs :href]) (html/select item [:.page-segment-gallery :.swiper-wrapper :a])))

(defn get-surface [item]
  (get-detail item "Surface habitable"))

(defn get-type [item]
  (get-detail item "Type d'objet"))


(defn get-raw-data [item]
  {:surface (get-surface item)
   :price (get-price item)
   :raw-address (get-address item)
   :type (get-type item)
   :images (get-images item)})
