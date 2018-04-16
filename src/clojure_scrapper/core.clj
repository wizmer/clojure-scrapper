(ns clojure-scrapper.core
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s]
            [clojure-scrapper.anibis :as anibis]
            [clojure-scrapper.homegate :as homegate]
            [clojure-scrapper.common :refer [fetch-url]]
            [hiccup.table]
            [hiccup.core]
            [clj-http.client :as client]
            [cemerick.url :as url])
  (:gen-class))




(def db (atom {}))

(def veto #{"https://www.anibis.ch/fr/d-immobilier-immobilier-locations-genève--418/a-echanger-uniquement-appartement-4-pièces-contre-3-pièces--23919200.aspx?aral=834_1200_1800,851_35_&view=2&fcid=418"})




(defn google-map-url [data]
  (str "https://maps.googleapis.com/maps/api/directions/json?mode=bicycling&origin=" "Campus+Biotech,+Chemin+des+Mines+9,+1202+Genève" "&destination=" (url/url-encode (:address data)) "&key=" (slurp "google-map-api.key")))


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


(defn custom-annotations [item]
  (let [transform {"https://www.anibis.ch/fr/d-immobilier-immobilier-locations-genève--418/a-remettre-bail-au-1er-mai-appart-3p,-pâquis--23972198.aspx?aral=834_1200_1900&view=2&fcid=418"
                   (fn [item] (assoc item :price 1680
                                     :price-per-meter-square 30.54))}]
    (if (contains? transform (item :url))
      ((transform (item :url)) item)
      item)))

(defn aggregate-data [url]
  (let [raw-data (if (clojure.string/starts-with? url "https://www.anibis")
                   (clojure-scrapper.anibis/get-raw-data (fetch-url url))
                   (clojure-scrapper.homegate/get-raw-data (fetch-url url)))
        price-per-meter-square (if (and (raw-data :price) (raw-data :surface))
                                 (/ (raw-data :price) (raw-data :surface)))

        data-with-address (assoc raw-data
                                 :address (reorder-address (:raw-address raw-data)))

        data (assoc data-with-address
                    :commute-time (add-commute-time data-with-address)
                    :price-per-meter-square price-per-meter-square
                    :url (url/url-decode url))]

    (custom-annotations data)))



(defn filters [item]
  (and
   (not (veto (item :url)))
   (item :commute-time)
   (< (:commute-time item) 1600)
   (not (contains?  #{"Chambre" "Bureau" "Commerce" "Studio"} (str (:type item)) ))))

(defn all-filters [data]
  (filter filters data))

(def data (atom {}))

(defn to-html [data]
  (hiccup.core/html
   (hiccup.table/to-table1d data [:price "price" :address "address"])))

(defn write-html! [html-data]
  (spit "index.html" html-data))

(defn html-item [item]

  (let [title
        (apply str (interpose " - " [(str (item :surface) "m2")
                                     (str (item :price) " CHF")
                                     (str (int (/ (item :commute-time) 60.)) " min")]))
        img (when (first (item :images))
              [:img {:src (first (item :images)) :height 200 :width 200}])]
    [:li [:a {:href (:url item)} (or img "link")]
     title]))




(defn -main
  "I don't do a whole lot ... yet."
  ([] (-main 1800 40))
  ([max-price min-surface]
   (let [urls (concat
               (anibis/get-urls max-price min-surface)
               (homegate/get-urls max-price min-surface))
         data (map aggregate-data urls)
         filtered (all-filters data)]

     (write-html!
      (hiccup.core/html [:lu (map html-item  filtered)]))
     filtered))
)
