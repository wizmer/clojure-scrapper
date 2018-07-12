(def project 'clojure-scrapper)
(def version "0.1.0-SNAPSHOT")

(set-env! :resource-paths #{"src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "1.9.0"]
                            [enlive "1.1.6"]
                            [hiccup-table "0.2.0"]
                            [org.clojure/data.json "0.2.6"]
                            [cheshire "5.5.0"]
                            [clj-http "3.8.0"]
                            [com.cemerick/url "0.1.1"]
                            [adzerk/boot-test "RELEASE" :scope "test"]])

(task-options!
 aot {:namespace   #{'clojure-scrapper.core}}
 pom {:project     project
      :version     version
      :description "FIXME: write description"
      :url         "http://example/FIXME"
      :scm         {:url "https://github.com/yourname/clojure-scrapper"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}}
 repl {:init-ns    'clojure-scrapper.core}
 jar {:main        'clojure-scrapper.core
      :file        (str "clojure-scrapper-" version "-standalone.jar")})

(deftask build
  "Build the project locally as a JAR."
  [d dir PATH #{str} "the set of directories to write to (target)."]
  (let [dir (if (seq dir) dir #{"target"})]
    (comp (aot) (pom) (uber) (jar) (target :dir dir))))

(deftask run
  "Run the project."
  [a args ARG [str] "the arguments for the application."]
  (with-pass-thru fs
    (require '[clojure-scrapper.core :as app])
    (apply (resolve 'app/-main) args)))

(deftask dev
  []
  (repl))


(require '[adzerk.boot-test :refer [test]])
