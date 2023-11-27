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

  (defn get-all-flight-hops [src dest]
        (letfn [(search [current-path visited?]
                  (let [current-city (peek current-path)]
                    (if (= current-city dest)
                      (list current-path)
                      (mapcat (fn [[[from to] _]]
                                (when (and (= current-city from)
                                           (not (visited? to)))
                                  (search (conj current-path to) (conj visited? to))))
                              available-flights))))]
          (search [src] #{src})))

  (defn get-all-flight-paths [src dest]
        (let [hops (get-all-flight-hops src dest)]
          (map (fn [hop]
                 (->> hop
                      (partition 2 1)
                      (map vec)
                      (select-keys available-flights)))
               hops)))

  ; Define the basic requirements for families and groups. Here, we use a map where the
  ; keys are the type of passenger group ('family' or 'group') and the values are maps
  ; that specify the maximum number of connections and the budget.
  (def requirements
       {:family {:max-connections 2 :max-budget 700}
        :group  {:max-connections 3 :max-budget 1000}})

  (defn route-price-and-connections [route]
        {:route-connections (->> route vals (map :connections) (reduce +))
         :route-price       (->> route vals (map :price) (reduce +))})

  ; Example function to check if a route meets the family or group requirements
  (defn route-meets-requirements?
        [route-type route]
        (let [{:keys [max-connections max-budget]} (requirements route-type)
              {:keys [route-connections route-price]} (route-price-and-connections route)]
          (and (<= route-connections max-connections)
               (<= route-price max-budget))))

  (defn find-flights
        "Returns a list of possible routes from `departure` to `destination` within the given `max-connections` and `max-price`."
        [departure destination type]
        (->> (get-all-flight-paths departure destination)
             (filter #(route-meets-requirements? type %))))

  (defn format-travel-plans
        "Formats a list of travel plans for display."
        [travel-routes]
    ; Define a local function to format a single travel plan
        (let [format-plan (fn [route]
                            (let [{:keys [route-connections route-price]} (route-price-and-connections route)]
                              (prn "--------" route)
                              (str "Travel Plan:\n"
                                   "  From: " (-> route first first first) "\n" ; Append the departure city
                                   "  To: " (-> route first first second) "\n" ; Append the destination city
                                   "  Total Price: $" route-price "\n" ; Append the total price
                                   "  Total Connections: " route-connections "\n")))] ; Append the total number of connections
          ; Apply the format-plan function to each travel plan in the list and concatenate the results into a single string
          (apply str (map format-plan travel-routes))))

  (defn parse-and-validate-input
        "Parses and validates user input. Expected input format: 'departure-city,destination-city,passenger-type'."
        [input-string]
        (let [[departure destination type] (clojure.string/split input-string #",")
              type (keyword type)]
          (println "[validate-input] departure =" departure "; destination =" destination "; type =" type)
          (cond
            (str/blank? departure)
            (do (println "Departure city is missing")
                false)

            (str/blank? destination)
            (do (println "Destination city is missing")
                false)

            (not (#{:family :group} type))
            (do
              (println "Group is missing")
              false)
            :else [departure destination type])))

  ;;-main function for launching from IDE also from command line
  (defn -main
        "The main function that ties everything together in the travel application."
        [& [input]]
        (let [input-string (or input "Prague,Berlin,family")]
          ; Parse and validate the input
          (when-let [[departure destination type] (parse-and-validate-input input-string)]
            (let [; Find feasible travel plans based on the criteria
                  travel-plans (find-flights departure destination type)
                  formatted-plan (format-travel-plans travel-plans)]
              (prn formatted-plan)
              formatted-plan))))