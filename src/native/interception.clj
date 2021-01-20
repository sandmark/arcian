(ns native.interception
  (:require [clj-native.direct :as n]))

(n/defclib interception
  (:libname "interception")
  (:structs
   (key-stroke :code short :state short :information int :x int :y int :rolling short))
  (:callbacks
   (predicate [int] int))
  (:functions
   (interception_create_context [] void*)
   (interception_destroy_context [void*] void)
   (interception_wait [void*] int)
   (interception_receive [void* int key-stroke* int] int)
   (interception_set_filter [void* predicate short] void)
   (interception_is_keyboard [int] int)
   (interception_is_mouse [int] int)
   (interception_send [void* int key-stroke* int] int)
   (interception_get_hardware_id [void* int void* int] int)))

(def INTERCEPTION_KEY_E0 0x02)
(def INTERCEPTION_KEY_E1 0x04)
(def INTERCEPTION_FILTER_KEY_DOWN 0x01)
(def INTERCEPTION_FILTER_KEY_UP 0x02)
