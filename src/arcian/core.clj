(ns arcian.core
  (:gen-class)
  (:require [clj-native.callbacks :as cb]
            [clj-native.direct :as n]
            [clj-native.structs :as st]
            [clojure.java.io :as io]
            [native.interception :as i]
            [native.kernel32 :as k32]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders])
  (:import [com.sun.jna Memory Structure]))

(defonce system (atom nil))
(defonce settings (atom nil))

(def logfile "arcian.log")
(def settings-file "settings.edn")

(timbre/merge-config!
 {:output-fn (partial timbre/default-output-fn {:stacktrace-fonts {}})
  :appenders {:spit (appenders/spit-appender {:fname logfile})}})

(defn read-config []
  (try
    (-> (or (io/resource settings-file)
            (io/as-file settings-file))
        slurp
        read-string)
    (catch java.io.FileNotFoundException _
      (timbre/fatal "Settings file not found: " settings-file)
      nil)))

(defn destroy-context []
  (when-let [context (:context @system)]
    (i/interception_destroy_context context)))

(defn create-context
  "Creates interception context in system.
   Returns context itself or nil if failed."
  []
  (swap! system assoc :context (i/interception_create_context))
  (:context @system))

(defn set-filter [callback & flags]
  (i/interception_set_filter (:context @system)
                             callback
                             (apply bit-or flags)))

(defn get-hardware-id [device]
  (let [size 500
        buf  (Memory. size)]
    (i/interception_get_hardware_id (:context @system) device buf size)
    (.getWideString buf 0)))

(defn allocate-stroke-buffer []
  (swap! system assoc :stroke (st/byref i/key-stroke Structure/ALIGN_NONE)))

(defn wait
  "Waits next hardware input and returns its device code."
  []
  (i/interception_wait (:context @system)))

(defn receive
  "Receives key stroke of `device`, `n` times, returning succeed(1) or failed(0).
   The key stroke will be saved in `system` as a mutable value."
  [device n]
  (i/interception_receive (:context @system) device (:stroke @system) n))

(defn wait-and-receive []
  (let [device (wait)]
    [device (receive device 1)]))

(defn send-key
  "Sends `device`'s key-stroke, saved in `system`, `n` times."
  [device n]
  (i/interception_send (:context @system)
                       device
                       (:stroke @system)
                       n))

(defn remap-keycode
  "Modifies key-code of key-stroke, saved in `system`, as `keycode`."
  [keycode]
  (set! (. (:stroke @system) code) keycode))

(defn remap-send! [device]
  (let [keycode     (.code (:stroke @system))
        new-keycode (get-in @settings [:core/remap-keys keycode])]
    (if-not new-keycode
      (do (println "Key " keycode " not remapped.")
          (send-key device 1))
      (do (println (str "Key " keycode " -> " new-keycode))
          (if (vector? new-keycode)
            (doseq [code new-keycode]
              (remap-keycode code)
              (send-key device 1))
            (do (remap-keycode new-keycode)
                (send-key device 1)))))))

(defn -main [& args]
  (n/loadlib k32/kernel32)
  (n/loadlib i/interception)
  (k32/raise-process-priority)
  (if-let [m (read-config)]
    (swap! settings (constantly m))
    (System/exit 1))

  (let [razer?
        (cb/callback
         i/predicate
         (fn [device]
           (let [id     (get-hardware-id device)
                 target (:core/device-id @settings)]
             (timbre/debug "Filtering device: " id)
             (if (and (= (i/interception_is_keyboard device) 1)
                      (= id target))
               (do (swap! system update :razer-found? (constantly true))
                   1)
               0))))]

    (when-not (create-context)
      (println "Failed to allocate Interception context.")
      (System/exit 1))

    (set-filter razer?
                i/INTERCEPTION_FILTER_KEY_DOWN
                i/INTERCEPTION_FILTER_KEY_UP
                i/INTERCEPTION_KEY_E0
                i/INTERCEPTION_KEY_E1)

    (when-not (:razer-found? @system)
      (timbre/fatal "Razer Tartarus V2 device not found: " (:core/device-id @settings)))

    (allocate-stroke-buffer)

    (println "Begin remapping...")
    (loop [[device received] (wait-and-receive)]
      (when (zero? received)
        (println "Failed to receive key stroke, quitting.")
        (destroy-context)
        (System/exit 1))

      (remap-send! device)
      (recur (wait-and-receive)))

    (destroy-context)))
