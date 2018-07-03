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


(def veto #{"https://www.leboncoin.fr/velos/1418033361.htm",
            "https://www.leboncoin.fr/velos/1103022894.htm"
            "https://www.leboncoin.fr/velos/1411196019.htm"
            "https://www.leboncoin.fr/velos/1413053717.htm"
            "https://www.leboncoin.fr/velos/1416669974.htm"
            "https://www.leboncoin.fr/velos/1415480310.htm"
            "https://www.leboncoin.fr/velos/1415636597.htm"
            "https://www.leboncoin.fr/velos/1415727095.htm"
            "https://www.leboncoin.fr/velos/1416784917.htm"
            "https://www.leboncoin.fr/velos/1416850917.htm"
            "https://www.leboncoin.fr/velos/1418116154.htm"})

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
                               "cadre l."
                               "taille : l"
                               "taille : m"
                               "freeride"
                               "elect"
                               "élect"
                               "décathlon"
                               "decathlon"
                               "1m8"
                               "180cm"]))

        no-enduro-no-dh (fn [item]
                          (let [title (s/lower-case (get-in item [:attrs :title]))
                                url (str "https:" (get-in item [:attrs :href]))]
                            (and (not (contains? veto (first (clojure.string/split url #"\?"))))
                                 (not (re-find vetoed-words title))
                                 (let [detail (fetch-url url)
                                       text (s/lower-case
                                             (get-in (first (html/select detail
                                                                        [(html/attr= :name "description")])) [:attrs :content]))]
                                   (not (re-find vetoed-words text))))))]
    (filter no-enduro-no-dh (html/select (fetch-url base-url) [(html/attr= :itemtype "http://schema.org/Offer") :a]))))

(defn format-result [item]
  {:title (get-in item [:attrs :title])
   :url (str "https:" (get-in item [:attrs :href]))})

(defn get-titles [items]
  (println "blah")
  (map format-result items))

(defn -main []
  (println "Haute-Savoie")
  (clojure.pprint/pprint (get-titles (get-urls haute-savoie-url)))
  (println "Ain")
  (clojure.pprint/pprint (get-titles (get-urls ain-url))))
