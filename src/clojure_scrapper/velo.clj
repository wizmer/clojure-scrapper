(ns clojure-scrapper.velo
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s]
            [clojure-scrapper.common :refer [fetch-url]]
            [hiccup.table]
            [hiccup.core]
            [clj-http.client :as client]
            [cemerick.url :as url]))


(defn make-url [department]
  {}
  (str"https://www.leboncoin.fr/velos/offres/rhone_alpes/" department "/?th=1&q=vtt&it=1&ps=12"))


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

(def veto #{"1417663600"
            "1417482251"
            "1420624462"
            "1421643446"
            "1420499660"
            "1421270190"
            "1423570222"
            "1422036827"
            "1420913546"
            "1417033848"
            "1423846119"
            "1416850917"
            "1423529009"
            "1413053717"
            "1417988312"
            "1418117554"
            "1391589490"
            "1419714339"
            "1420192076"
            "1423853613"
            "1416669974"
            "1420561549"
            "1420833976"
            "1421765818"
            "1422238809"
            "1423194874"
            "1422276373"
            "1416784917"}
  )

(defn get-urls [base-url]
  (let [all-urls (html/select (fetch-url base-url)
                     [(html/attr= :itemtype "http://schema.org/Offer") :a])

        vetoed-words (re-pattern
                      (s/join "|"
                              ["dh"
                               "enduro"
                               "descente"
                               "rockrider"
                               "taille m/l"
                               "taille l"
                               "taille m"
                               "shimano alivio"
                               "shimano deore"
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
                          (let [item-url (str "https:" (get-in item [:attrs :href]))
                                item-id (second (re-find #"velos\/(.*)\.htm" item-url))
                                title (s/lower-case (get-in item [:attrs :title]))]
                            (and
                             (not (contains? veto item-id))
                             (not (re-find vetoed-words title))
                             (let [detail (fetch-url item-url)
                                       text (s/lower-case
                                             (get-in (first (html/select detail
                                                                        [(html/attr= :name "description")])) [:attrs :content]))]
                                   (not (re-find vetoed-words text))))))]
    (filter no-enduro-no-dh
            all-urls)))

(defn format-result [item]
  {:title (get-in item [:attrs :title])
   :url (str "https:" (get-in item [:attrs :href]))})

(defn get-titles [items]
  (println "blah")
  (map format-result items))

(def data (atom {}))

(defn fill-data! [department]
  (swap! data assoc department (get-titles (get-urls (make-url department)))))

(defn browse [data]
  (let [all-departments-data (apply concat (vals data))]
    (map #(clojure.java.browse/browse-url (:url %))
       all-departments-data)))
(defn -main []
  (doseq [department ["savoie" "haute_savoie" "ain"]]
    (fill-data! department))
  (browse @data)
  )
