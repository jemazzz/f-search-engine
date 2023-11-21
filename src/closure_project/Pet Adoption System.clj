(ns closure-project.Pet Adoption System)

(ns pet-adoption.core
  (:gen-class))

(def pet-db (atom [])) ;; Atom to store a vector of pets

(defn create-pet [name species age breed description available]
  {:name name
   :species species
   :age age
   :breed breed
   :description description
   :available available})

(defn list-available-pets []
  (filter :available @pet-db))

(defn adopt-pet [index]
  (let [pet (nth @pet-db index)]
    (if (= (:available pet) true)
      (do
        (swap! pet-db update-in [index] assoc :available false)
        (println "You have adopted" (:name pet))
        true)
      false)))

(defn interact-with-pet [index action user-info]
  (let [pet (nth @pet-db index)]
    (if (and (not= (:available pet) true) (:species pet))
      (let [action-result
            (case action
              :walk-dog (if (= (:species pet) "dog") true false)
              :cuddle-cat (if (= (:species pet) "cat") true false))
            pet-id (count @pet-db)]
        (if action-result
          (do
            (println "You have interacted with" (:name pet))
            (conj! pet-db
                   {:id (inc pet-id)
                    :user-info user-info
                    :pet-info pet
                    :action action})
            true)
          false))
      false)))

(defn main-menu []
  (println "Welcome to the Pet Adoption System")
  (println "1. List Available Pets")
  (println "2. Adopt a Pet")
  (println "3. Interact with a Pet")
  (println "4. Exit"))

(defn adopt-menu []
  (println "Adopt a Pet:")
  (let [available-pets (list-available-pets)
        num-pets (count available-pets)]
    (if (empty? available-pets)
      (println "No pets available for adoption.")
      (do
        (doseq [i (range num-pets)]
          (let [pet (nth available-pets i)]
            (println (str (inc i) ". " (:name pet) " - " (:species pet)))))
        (print "Enter the number of the pet you want to adopt: ")
        (let [choice (Integer. (read-line))]
          (if (and (<= choice num-pets) (> choice 0))
            (if (adopt-pet (dec choice))
              (println "Congratulations! You have adopted a pet.")
              (println "Sorry, the pet is no longer available for adoption."))
            (println "Invalid choice. Please select a valid number.")))))))

(defn interact-menu []
  (println "Interact with a Pet:")
  (print "Enter the pet's index: ")
  (let [index (Integer. (read-line))]
    (if (and (<= index (dec (count @pet-db)) (> index -1))
             (let [pet (nth @pet-db index)]
               (if (= (:species (:pet-info pet)) "dog")
                 (do
                   (print "Enter your information (First Name Last Name DateOfBirth Email Phone Address): ")
                   (let [user-info (read-line)]
                     (if (interact-with-pet index :walk-dog user-info)
                       (println "You have taken the dog for a walk.")
                       (println "Sorry, you can't walk this pet."))
                     (print "Press any key to continue...")
                     (read-line)))
                 (if (= (:species (:pet-info pet)) "cat")
                   (do
                     (print "Enter your information (First Name Last Name DateOfBirth Email Phone Address): ")
                     (let [user-info (read-line)]
                       (if (interact-with-pet index :cuddle-cat user-info)
                         (println "You have spent time with the cat.")
                         (println "Sorry, you can't spend time with this pet."))
                       (print "Press any key to continue...")
                       (read-line))))))))
    (do
      (println "Invalid index. Please enter a valid index.")
      (print "Press any key to continue...")
      (read-line)))))

(defn -main [& args]
  (loop []
    (main-menu)
    (print "Enter your choice (1/2/3/4): ")
    (let [choice (Integer. (read-line))]
      (cond
        (= choice 1) (adopt-menu)
        (= choice 2) (adopt-menu)
        (= choice 3) (interact-menu)
        (= choice 4) (System/exit 0)
        :else (println "Invalid choice. Please enter a valid choice."))
      (recur))))

(-main)
