(ns flight.search-engine
      (:require [clojure.string :as str]))

; Define a map of available flights where each key is a vector representing a route
; from a departure city to a destination city, and each value is a map with the price
; and number of connections for that flight.
(def available-flights
  {["Krakov" "Warsaw"]     {:price 100 :connections 1}
   ["Hamburg" "Berlin"]    {:price 100 :connections 1}
   ["Warsaw" "Berlin"]     {:price 300 :connections 1}
   ["Prague" "Berlin"]     {:price 200 :connections 1}
   ["Munich" "Berlin"]     {:price 100 :connections 1}
   ["Munich" "Innsbruck"]  {:price 100 :connections 1}
   ["Vienna" "Innsbruck"]  {:price 200 :connections 1}
   ["Vienna" "Budapest"]   {:price 300 :connections 1}
   ["Warsaw" "Budapest"]   {:price 400 :connections 1}
   ["Zagreb" "Budapest"]   {:price 200 :connections 1}
   ["Vienna" "Rome"]       {:price 400 :connections 1}
   ["Napoli" "Rome"]       {:price 200 :connections 1}
   ["Napoli" "Rijeka"]     {:price 100 :connections 1}
   ["Vienna" "Prague"]     {:price 200 :connections 1}
   ["Vienna" "Rijeka"]     {:price 400 :connections 1}
   ["Rijeka" "Zagreb"]     {:price 100 :connections 1}
   ["Vienna" "Zagreb"]     {:price 300 :connections 1}
   ["Munich" "Zagreb"]     {:price 400 :connections 1}
   ["Innsbruck" "Rome"]    {:price 400 :connections 1}
   ["Budapest" "Rome"]     {:price 400 :connections 1}
   ["Budapest" "Berlin"]   {:price 300 :connections 1}
   ["Prague" "Brno"]       {:price 100 :connections 1}
   ["Prague" "Budapest"]   {:price 300 :connections 1}})


(defn reverse-connections []
  (reduce (fn [acc input]
            (let  [curr (key input)
                   city (first curr)
                   destination (second curr)
                   price  (val input)]

              (assoc acc [destination city] price)
              ) ) {} available-flights)
  )



(defn available-flights-two-way []
  (conj available-flights (reverse-connections)))

(comment
  (available-flights-two-way)
  )


(defn find-flight-price-details-between [departure-input destination-input]
  (first (->> (available-flights-two-way)
              (filter
                #(and (= (first (first %1)) departure-input)
                      (= (second (first %1)) destination-input)))
              (map #(second %1)))))



(defn connecting-flights []
  (reduce (fn [acc , curr]
            (let  [city (first curr)
                   destination (second curr)]

              (if (contains?  acc (keyword city))
                (let [destinations (get acc (keyword city))
                      updated-destinations (conj destinations destination)]
                  (assoc acc (keyword city) updated-destinations))
                (assoc acc (keyword city) [destination])))) {} (keys (available-flights-two-way))))




(defn all-cities []
  (reduce (fn [acc , curr]
            (conj acc (first curr) (second curr) )
            ) #{} (keys (available-flights-two-way))))


; Connection is two way fix
(defn departure-cities []
  (all-cities))

(defn destination-cities []
  (all-cities))






(defn connecting-flights-from [city]
  (get (connecting-flights) (keyword city)))




(defn should-travel? [path next-city]
  (not (contains? (set path) next-city)))

(defn find-all-paths-between [departure-city destination-city current-city path]
  (cond
    (= current-city destination-city) [path]
    :else
    (let [destinations (connecting-flights-from  current-city)]
      (mapcat (fn [current-destination]
                (when (should-travel? path current-destination)
                  (find-all-paths-between departure-city destination-city current-destination (conj path current-destination))))
              destinations))))




(defn find-all-paths [departure-city destination-city]
  (find-all-paths-between departure-city destination-city departure-city [departure-city]))



(comment

  (connecting-flights )
  (find-all-paths "Vienna"  "Budapest" ) )

(defn find-price-for-path [path]
  (reduce (fn [acc,curr]
            (let [departure (first curr)
                  destination (second curr)
                  departure-details (find-flight-price-details-between departure destination)
                  existing-price (get acc :price)
                  existing-connections (get acc :connections)
                  price (get departure-details :price)
                  connections (get departure-details :connections)
                  updated-price (+ existing-price price)
                  updated-connections (+ existing-connections connections)]

              ;   (println  "departure =" departure "; destination =" destination  "; price =" price "; connections =" connections)

              (assoc acc :price updated-price :connections updated-connections)))

          {:price 0, :connections 0} (partition 2 1 path)))

(defn get-all-flight-hops [src dest]
  (letfn [(search [current-path visited?]
            (let [current-city (peek current-path)]
              (if (= current-city dest)
                (list current-path)
                (mapcat (fn [[[from to] _]]
                          (when (and (= current-city from)
                                     (not (visited? to)))
                            (search (conj current-path to) (conj visited? to))))
                        (available-flights-two-way)))))]
    (search [src] #{src})))

; Define the basic requirements for families and groups. Here, we use a map where the
; keys are the type of passenger group ('family' or 'group') and the values are maps
; that specify the maximum number of connections and the budget.
(def requirements
  {:family {:max-connections 2 :max-budget 700}
   :group  {:max-connections 3 :max-budget 1000}})

(defn convert-passenger-type [passenger-type]
  (cond
    (= passenger-type "f") :family
    (= passenger-type "g") :group))

(defn valid-departure-city? [city]
  (let [cities (departure-cities)]
    (clojure.core/contains? cities city)))

(defn valid-destination-city? [city]
  (let [cities (destination-cities)]
    (clojure.core/contains? cities city)))

(defn valid-family-type? [input]
  (let [family-type #{"f" "g"}]
    (contains? family-type input)))

(defn validate-input [departure destination typeofpassenger]

  (cond
    (str/blank? departure)
    (do (println "Departure city is missing")
        false)

    (str/blank? destination)
    (do (println "Destination city is missing")
        false)

    (not (valid-departure-city? departure))
    (do (println "Departure city is not valid")
        false)

    (not (valid-destination-city? destination))
    (do (println "Destination city is not valid")
        false)

    (not (valid-family-type? typeofpassenger))
    (do
      (println "Group is missing")
      false)
    :else [departure destination typeofpassenger]))

(defn find-routes-for [departure destination]
  (map  #(assoc (find-price-for-path %1)  :path %1)
        (find-all-paths  departure destination)))

(defn get-departure-with-prices [path]
  (->> path
       (partition 2 1)
       (map (fn [curr]
              (let [departure (first curr)
                    destination (second curr)
                    filght-and-price-details  (find-flight-price-details-between departure destination)
                    price (get filght-and-price-details :price)]
                (str departure "("  price  ")"))))
       (into [])))

(defn format-output [itenary]
  (let [depratures-with-price    (get-departure-with-prices (get  itenary :path))
        total-price (get itenary :price)
        total-connections (get itenary :connections)
        final-destination (last (get  itenary :path))
        first-line (str/join " > " (conj   depratures-with-price   final-destination))]
    (str "Route with prices for segments: " first-line  "\n"  "Total price: " total-price " \n" "Flights: " total-connections)))

(defn find-flights
  "Returns a list of possible routes from `departure` to `destination` within the given `max-connections` and `max-price`."
  [departure destination typeofpassenger]
  (let [routes   (find-routes-for departure destination)
        passenger-requirment (requirements typeofpassenger)
        max-connections (get passenger-requirment :max-connections)
        max-budget (get passenger-requirment :max-budget)]
    (->> routes
         (filter (fn [input]
                   (and
                     (<=  (get input :connections) max-connections)
                     (<=  (get input :price)  max-budget))))
         (sort-by :price)
         last
         (format-output))))

(defn parse-and-validate-input
  "Parses and validates user input. Expected input format: 'departure-city,destination-city,passenger-type'."
  [input-string]
  (let [[departure destination passenger-type] (clojure.string/split input-string #",")]
    (validate-input departure destination passenger-type)))

;;-main function for launching from IDE also from command line
(defn -main
  "The main function that ties everything together in the travel application."
  [& [input]]
  (let [input-string (or input "Munich,Prague,f")]
    ; Parse and validate the input
    (when-let [[departure destination type] (parse-and-validate-input input-string)]
      (let [; Find feasible travel plans based on the criteria
            formatted-plan (find-flights departure destination  (convert-passenger-type type))]
        (println formatted-plan)
        formatted-plan))))

(comment
  ;
  ;  Tests result:
  ;  1) Vienna-Budapest, g. It shows
  ;Route with prices for segments: Vienna(300) > Zagreb(200) > Budapest(300) > Berlin
  ;Total price: 800
  ;Flights: 3
  ;correct answer: Vienna  --> Innsbruck  --> Rome --> Budapest
  ;1000



  (println (find-flights "Vienna" "Budapest"  :group))


  ;2) Warsaw-Napoli
  ;destination city is invalid.
  ;I believe it is absent in your list of connections. Btw, a connection can be performed in both ways.
  ;
  (println (find-flights "Warsaw" "Napoli"  :group))


  ;3) Munich-Prague
  ;for Families = no result
  ;for Group = no result, basically, the program failed all tests. Look, you have 24 hours to fix it. In any case, sooner or later, you need to do this because ICA 2 is heavily dependent on this program and its correct behavior. If you are completely stuck, you may check "week10\path builder" or bfs algorithm with a queue (one team successfully submitted a solution like this). Or fix bugs in your solution a


  ;Munich-Prague
  (println (find-flights "Munich" "Prague"  :family))
  (println (find-flights "Munich" "Prague"  :group))


  )

