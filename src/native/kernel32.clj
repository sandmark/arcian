(ns native.kernel32
  (:require [clj-native.direct :as n]))

(n/defclib kernel32
  (:libname "kernel32")
  (:functions
   (GetCurrentProcess [] void*)
   (SetPriorityClass [void* int] bool)))

(def HIGH_PRIORITY_CLASS 0x80)

(defn raise-process-priority []
  (SetPriorityClass (GetCurrentProcess) HIGH_PRIORITY_CLASS))
