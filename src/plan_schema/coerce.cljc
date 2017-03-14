;; Copyright © 2016 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns plan-schema.coerce
  (:require [clojure.pprint :refer :all]
            [clojure.set :as set]))

; TODO network and network-id serve the same purpose.
; Consider network-id deprecated and removed in future.
; TODO :type and :tpn-type serve the same purpose.
; Consider :tpn-type deprecated and removed in future.

(defn check-value-nil? [key value]
  (if (nil? value)
    (println "Warning: nil value: " key "->" value)))

(defn to-keyword [key value]
  (check-value-nil? key value)
  {key (keyword value)})

(defn to-set-of-keywords [key value]
  {key (into #{} (map keyword value))})

(defn to-vector-of-keywords [key value]
  {key (into [] (map keyword value))})

(defn to-boolean [key value]
  (if-not (instance? Boolean value)
    (println "Warning: to-boolean. value is not boolean."))
  {key value})

;;; Functions for each HTN and/or TPN key ;;;
(defmulti convert-property "Coercion functions for each key" (fn [key value]
                                                               key))

(defmethod convert-property :default [key value]
  (println "Warning: convert-property: No conversion for" key "->" value)
  {key value})

(defmethod convert-property nil [key value]
  (println "Error: Bad key nil:" key "->" value)
  {key value})

(defmethod convert-property :network-id [key value]
  (to-keyword key value))

(defmethod convert-property :network [key value]
  (to-keyword key value))

(defmethod convert-property :uid [key value]
  (to-keyword key value))

(defmethod convert-property :begin-node [key value]
  (to-keyword key value))

(defmethod convert-property :end-node [key value]
  (to-keyword key value))

(defmethod convert-property :controllable [key value]
  (to-boolean key value))

(defmethod convert-property :constraints [key value]
  (to-set-of-keywords key value))

(defmethod convert-property :label [key value]
  ;(println "Warning: Verify me" key "->" value);TODO
  {key value})

(defmethod convert-property :name [key value]
  {key value})

(defmethod convert-property :htn-node [key value]
  (to-keyword key value))

(defmethod convert-property :tpn-type [key value]
  ;(println "Warning: deprecated key" key (to-keyword key value));TODO
  (to-keyword key value))

(defmethod convert-property :type [key value]
  (to-keyword key value))

(defmethod convert-property :argsmap [key value]
  {key value})

(defmethod convert-property :command [key value]
  ;(println "Warning: deprecated key" key)                   ;TODO
  {key value})

(defmethod convert-property :display-name [key value]
  {key value})

(defmethod convert-property :activities [key value]
  (to-set-of-keywords key value))

(defmethod convert-property :incidence-set [key value]
  (to-set-of-keywords key value))

(defmethod convert-property :value [key value]
  (to-set-of-keywords key value))

(defmethod convert-property :args [key value]
  {key value})

(defmethod convert-property :htn-expanded-nonprimitive-task [key value]
  (to-keyword key value))

(defmethod convert-property :edges [key value]
  (to-vector-of-keywords key value))

(defmethod convert-property :edge-type [key value]
  (to-keyword key value))

(defmethod convert-property :rootnodes [key value]
  (to-set-of-keywords key value))

(defmethod convert-property :interface [key value]
  (to-keyword key value))

(defmethod convert-property :plantid [key value]
  (to-keyword key value))

(defmethod convert-property :plant-part [key value]
  (to-keyword key value))

;;;
; TODO label is reserved for pamela label semantics. Here label should be display-name?
(def delay-activity-slots #{:uid :tpn-type :name :htn-node :constraints :label :controllable :end-node})
(def activity-slots #{:uid :tpn-type :name :htn-node :constraints :label :controllable :end-node :args :argsmap :command :display-name}) ;FIXME :args-mapping
(def activity-slots-optional #{:interface :plantid :plant-part}) ;FIXME :args-mapping
(def null-activity-slots #{:constraints :uid :tpn-type :end-node})
(def state-slots #{:uid :tpn-type :constraints :activities :incidence-set})
(def state-slots-optional #{:end-node :htn-node})
(def temporal-constraint-slots #{:uid :tpn-type :value :end-node})
(def network-slots #{:uid :tpn-type :begin-node :end-node})
(def begin-slots #{:constraints :uid :tpn-type :htn-node :end-node :activities :incidence-set})
(def end-slots #{:constraints :uid :tpn-type :activities :incidence-set})


; HTN slots
(def htn-network-slots #{:uid :type :label :rootnodes})
(def htn-network-slots-optional #{:display-name})

(def htn-expanded-nonprimitive-task-slots #{:uid :type :label :incidence-set :edges})
(def htn-expanded-nonprimitive-task-slots-optional #{:name :display-name})

(def htn-expanded-method-slots #{:uid :type :label :network :incidence-set :edges})
(def htn-expanded-method-slots-optional #{:display-name})

(def edge-slots #{:uid :type :end-node})
(def edge-slots-optional #{:label :edge-type})

(def htn-primitive-task-slots #{:uid :name :type :label :display-name :incidence-set :edges})
(def htn-primitive-task-slots-optional #{:args :argsmap :interface :plantid :plant-part})

; Check for nil values
; Check for nil keys
; Check for missing keys
; Check for extra keys
(defn find-nil-key
  "Given a map, if it containts nil? key and value,
  returns a map with nil as a key and it's value"
  [m]
  (if (contains? m nil)
    {nil (get m nil)}
    {}))

(defn find-nil-values
  "Given a map return a map containing only keys that have nil their value"
  [m]
  (into {} (filter (fn [[k v]]
                     (nil? v)) m)))

(defn find-missing-keys
  "Given a map and a set of expected-keys, return a set of keys that do not
  exists in the map"
  [m expected-keys]
  (set/difference expected-keys (into #{} (keys m))))

(defn find-extra-keys
  "Given a map and a set of expected-keys, return a map containing keys that are
  in the expected set."
  [m expected-keys]
  (select-keys m (set/difference (into #{} (keys m)) expected-keys)))

(defn check-and-print-object-issues [m expected-keys & [optional-keys]]
  ;(println "optional keys" optional-keys)
  ;(println "optional keys" expected-keys)
  (let [issues {:extra-keys   (find-extra-keys m (set/union expected-keys optional-keys))
                :missing-keys (find-missing-keys m expected-keys)
                :nil-key      (find-nil-key m)
                :nil-values   (find-nil-values m)}
        issues (into {} (remove (fn [[k v]]
                                  (empty? v)) issues))
        ]
    (when (pos? (count issues))
      (println "Found issues in map -----")
      (pprint m)
      (println "expected-keys:" expected-keys)
      (pprint issues)
      )))

(defmulti check-object (fn [m]
                         (keyword (or (:tpn-type m) (:type m)))))

(defmethod check-object :default [m]
  ;(pprint m)
  (println "Warning: check-object need impl for: " (or (:tpn-type m) (:type m)))
  (println "Keys:" (or (:tpn-type m) (:type m)) "-slots" (into #{} (keys m)))
  (pprint m))

(defmethod check-object :delay-activity [m]
  (check-and-print-object-issues m delay-activity-slots))

(defmethod check-object :null-activity [m]
  (check-and-print-object-issues m null-activity-slots))

(defmethod check-object :activity [m]
  (check-and-print-object-issues m activity-slots activity-slots-optional))

(defmethod check-object :state [m]
  (check-and-print-object-issues m state-slots state-slots-optional))

(defmethod check-object :temporal-constraint [m]
  (check-and-print-object-issues m temporal-constraint-slots))

(defmethod check-object :p-begin [m]
  (check-and-print-object-issues m begin-slots))

(defmethod check-object :c-begin [m]
  (check-and-print-object-issues m begin-slots))

(defmethod check-object :p-end [m]
  (check-and-print-object-issues m end-slots))

(defmethod check-object :c-end [m]
  (check-and-print-object-issues m end-slots))

;;; HTN objects
(defmethod check-object :network [m]
  (check-and-print-object-issues m network-slots))

(defmethod check-object :htn-network [m]
  (check-and-print-object-issues m htn-network-slots htn-network-slots-optional))

(defmethod check-object :htn-expanded-nonprimitive-task [m]
  (check-and-print-object-issues m htn-expanded-nonprimitive-task-slots htn-expanded-nonprimitive-task-slots-optional))

(defmethod check-object :htn-expanded-method [m]
  (check-and-print-object-issues m htn-expanded-method-slots htn-expanded-method-slots-optional))

(defmethod check-object :edge [m]
  (check-and-print-object-issues m edge-slots edge-slots-optional))

(defmethod check-object :htn-primitive-task [m]
  (check-and-print-object-issues m htn-primitive-task-slots htn-primitive-task-slots-optional))


;;; Top level dispatch ;;;
(defmulti convert "Function to dispatch coercion of top level scalars or maps of HTN and TPN"
          (fn [key value]
            (if (map? value) :map :scalar)))

(defmethod convert :default [key value]
  (println "Warning: plan-schema convert" key " -> " value)
  {key value})

(defmethod convert :scalar [key value]
  ;(println "Converting scalar" key value)
  (check-value-nil? key value)
  (convert-property key value))

(defmethod convert :map [outer-key m]
  ;(println "Converting map")
  (check-object m)
  {outer-key (reduce (fn [result [key value]]
                       (check-value-nil? key value)
                       (conj result (convert-property key value))) {} m)})

(defn coerce [data]
  (reduce (fn [result [key value]]
            ;(pprint result)
            ;(println key " -> " value "-- done")

            (conj result (convert key value))) {} data))