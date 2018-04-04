(ns syksy.util.mode)

(def modes #{:dev :prod})

(defn mode []
  {:post [(modes %)]}
  (-> (System/getProperty "syksy.mode")
      (or "dev")
      (keyword)))

(defn dev-mode? []
  (= (mode) :dev))

(defn prod-mode? []
  (= (mode) :prod))
