(ns clojure-scrapper.velo
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s]
            [clojure-scrapper.core :refer [fetch-url]]
            [hiccup.table]
            [hiccup.core]
            [clj-http.client :as client]
            [cemerick.url :as url]))


;; (def base-url "https://www.leboncoin.fr/velos/offres/rhone_alpes/ain/?th=1&q=vtt&it=1&ps=12")
(def base-url "https://www.leboncoin.fr/velos/offres/rhone_alpes/haute_savoie/?th=1&q=vtt&it=1&ps=12")



(defn get-urls []
  (let [no-enduro-no-dh (fn [item]
                          (let [title (s/lower-case (get-in item [:attrs :title]))]
                            (and (not (s/includes? title "dh"))
                                 (not (s/includes? title "enduro"))
                                 (not (s/includes? title "descente"))
                                 (not (s/includes? title "rockrider"))
                                 (not (s/includes? title "taille m/l"))
                                 (not (s/includes? title "taille l"))
                                 (not (s/includes? title "taille m"))
                                 (not (s/includes? title "xl"))
                                 (not (s/includes? title "trial"))
                                 (not (s/includes? title "taille : l"))
                                 (not (s/includes? title "taille : m"))
                                 (not (s/includes? title "elect"))
                                 (not (s/includes? title "élect"))

                                 (let [detail (fetch-url (str "https:"
                                                              (get-in item [:attrs :href])))
                                       text (s/lower-case
                                             (get-in (first (html/select detail
                                                                        [(html/attr= :name "description")])) [:attrs :content]))]
                                   (and (not (s/includes? text "dh"))
                                        (not (s/includes? text "enduro"))
                                        (not (s/includes? text "descente"))
                                        (not (s/includes? text "taille m/l"))
                                        (not (s/includes? text "taille l"))
                                        (not (s/includes? text "taille m"))
                                        (not (s/includes? text "xl"))
                                        (not (s/includes? text "1m8"))
                                        (not (s/includes? text "180cm"))
                                        (not (s/includes? text "taille : l"))
                                        (not (s/includes? text "taille : m"))
                                        (not (s/includes? text "elect"))
                                        (not (s/includes? text "élect")))))))]
    (filter no-enduro-no-dh (html/select (fetch-url base-url) [(html/attr= :itemtype "http://schema.org/Offer") :a]))))

(defn format [item]
  {:title (get-in item [:attrs :title])
   :url (get-in item [:attrs :href])})

(defn get-titles [items]
  (map format items))
