(ns clojure-scrapper.velo
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s]
            [clojure-scrapper.common :refer [fetch-url]]
            [hiccup.table]
            [hiccup.core]
            [clj-http.client :as client]
            [cemerick.url :as url]))


(def ain-url "https://www.leboncoin.fr/velos/offres/rhone_alpes/ain/?th=1&q=vtt&it=1&ps=12")
(def haute-savoie-url "https://www.leboncoin.fr/velos/offres/rhone_alpes/haute_savoie/?th=1&q=vtt&it=1&ps=12")



(defn get-urls [base-url]
  (let [vetoed-words (re-pattern
                      (s/join "|"
                              ["dh"
                               "enduro"
                               "descente"
                               "rockrider"
                               "taille m/l"
                               "taille l"
                               "taille m"
                               "xl"
                               "trial"
                               "taille : l"
                               "taille : m"
                               "freeride"
                               "elect"
                               "élect"
                               "1m8"
                               "180cm"]))

        no-enduro-no-dh (fn [item]
                          (let [title (s/lower-case (get-in item [:attrs :title]))]
                            (and (not (re-find vetoed-words title))
                                 (let [detail (fetch-url (str "https:"
                                                              (get-in item [:attrs :href])))
                                       text (s/lower-case
                                             (get-in (first (html/select detail
                                                                        [(html/attr= :name "description")])) [:attrs :content]))]
                                   (not (re-find vetoed-words text))))))]
    (filter no-enduro-no-dh (html/select (fetch-url base-url) [(html/attr= :itemtype "http://schema.org/Offer") :a]))))

(defn format-result [item]
  {:title (get-in item [:attrs :title])
   :url (get-in item [:attrs :href])})

(def veto #{"//www.leboncoin.fr/velos/1417663600.htm?ca=22_s"
            "//www.leboncoin.fr/velos/1413053717.htm?ca=22_s"
            "//www.leboncoin.fr/velos/1417482251.htm?ca=22_s"
            "//www.leboncoin.fr/velos/1417033848.htm?ca=22_s"
            "//www.leboncoin.fr/velos/1416850917.htm?ca=22_s"
            "//www.leboncoin.fr/velos/1416784917.htm?ca=22_s"})

(defn get-titles [items]
  (filter #(not (contains? veto (:url %)))
          (map format-result items)))

(defn -main []
  (println "Resultats dans l'Ain")
  (clojure.pprint/pprint (get-titles (get-urls ain-url)))

  (println "\n\n\nResultats en Haute-Savoie")
  (clojure.pprint/pprint (get-titles (get-urls haute-savoie-url))))
