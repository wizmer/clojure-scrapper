(ns clojure-scrapper.core
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s]
            [hiccup.table]
            [clj-http.client :as client]
            [cemerick.url :as url])
  (:gen-class))



(def ^:dynamic *base-url* "https://news.ycombinator.com/")

  (def ^:dynamic *base-url* "https://www.anibis.ch/fr/immobilier-immobilier-locations-gen%c3%a8ve--418/advertlist.aspx?aral=834_1200_1900&action=filter")

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))


(defn hn-headlines []
  (map html/text (html/select (fetch-url *base-url*) [:.list-item.listing :a])))




(defn get-urls [content]
  (map  #(str "https://www.anibis.ch"
              (get-in % [:attrs :href]))
        (html/select content [:.page-segment-item-listing :.list-item :a]) ))


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

(defn google-map-url [data]
  (str "https://maps.googleapis.com/maps/api/directions/json?mode=bicycling&origin=" "Campus+Biotech,+Chemin+des+Mines+9,+1202+Genève" "&destination=" (url/url-encode (:address data)) "&key=" (slurp "google-map-api.key")))

(def db (atom {}))

(defn add-commute-time [data]
  (let [url (google-map-url data)]
    (if (contains? @db url)
      (@db url)
      (do (let [time (get-in (client/get url {:as :json})
                             [:body :routes 0 :legs 0 :duration :value])]
            (swap! db assoc url time)
            time)))))

(defn reorder-address [address]
  (let [m (re-find #"(rue|avenue|street) ([0-9]+) (.*)" (s/lower-case address))]
    (if m
      (apply str (interpose " " [(nth m 2) (second m)  (nth m 3)]))
      address)))

(defn aggregate-data [url]
  (let [item (fetch-url url)
        price (get-price item)
        surface (get-detail item "Surface habitable")
        price-per-meter-square (if (and price surface)
                                 (/ price surface))
        data {:surface surface
              :price price
              :raw-address (get-address item)
              :type (get-detail item "Type d'objet")
              :address (reorder-address (get-address item))
              :price-per-meter-square price-per-meter-square
              :url (url/url-decode url)}]
    (assoc data :commute-time (add-commute-time data))))




(def urls
  (get-urls (fetch-url *base-url*)))

(defn filters [item]
  (and (< (:commute-time item) 3000)
       (not (= (:type item) "Bureau"))))

(defn all-filters [data]
  (filter filters data))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [data (map aggregate-data urls)
        d (last data)]
    (hiccup.core/html
     (hiccup.table/to-table1d data [:price "price" :address "address"]))
    )
)
