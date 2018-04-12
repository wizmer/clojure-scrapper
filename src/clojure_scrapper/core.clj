(ns clojure-scrapper.core
  (:gen-class))

(ns tutorial.scrape1
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s]
            [clj-http.client :as client]
            [cemerick.url :as url]))

(def ^:dynamic *base-url* "https://news.ycombinator.com/")

  (def ^:dynamic *base-url* "https://www.anibis.ch/fr/immobilier-immobilier-locations-gen%c3%a8ve--418/advertlist.aspx?aral=834_1200_1900&action=filter")

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))


(defn hn-headlines []
  (map html/text (html/select (fetch-url *base-url*) [:.list-item.listing :a])))

(defn hn-points []
  (map html/text (html/select (fetch-url *base-url*) [:td.subtext html/first-child])))


(defn get-urls [content]
  (map  #(str "https://www.anibis.ch"
              (get-in % [:attrs :href]))
        (html/select content [:.page-segment-item-listing :.list-item :a]) ))


(defn get-price [item]
  (read-string
   (get-in (first (html/select item [(net.cgrand.enlive-html/attr= :itemprop "price")])) [:attrs :content])))

(defn get-surface [item]
  (let [candidates (html/select item  [:div :.block])
        surface-item (first (filter
                             #(clojure.string/includes?  (html/text %) "Surface habitable")
                             candidates))
        surface-string (second (:content surface-item))]

    (when surface-string
      (read-string surface-string))))

(defn get-address [item]
  (let [street (first (html/texts (html/select item  [:div[:.street]])))
        city (first (html/texts (html/select item  [:div[:.city]])))]
    (s/trim (s/replace (str street " " city) #"[\t]+|[\n]+" " "))))

(defn google-map [data]
  (str "https://maps.googleapis.com/maps/api/directions/json?mode=bicycling&origin=" "Campus+Biotech,+Chemin+des+Mines+9,+1202+Genève" "&destination=" (url/url-encode (:address data)) "&key=" (slurp "google-map-api.key")))

(defn add-commute-time [data]
  (let [url (google-map data)]
    (get-in (client/get url {:as :json})
            [:body :routes 0 :legs 0 :duration :value])))

(defn aggregate-data [url]
  (let [item (fetch-url url)
        price (get-price item)
        surface (get-surface item)
        price-per-meter-square (if (and price surface)
                                 (/ price surface))
        data {:surface surface
              :price price
              :address (get-address item)
              :price-per-meter-square price-per-meter-square
              :url url}]
    (assoc data :commute-time (add-commute-time data))
    ))



(def urls
  (get-urls (fetch-url *base-url*)))

(def data (map aggregate-data urls))
(def d (last data))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
