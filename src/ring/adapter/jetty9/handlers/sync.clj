(ns ring.adapter.jetty9.handlers.sync
  (:require
    [ring.adapter.jetty9.common :as common]
    [ring.adapter.jetty9.servlet :as servlet]
    [ring.adapter.jetty9.websocket :as ws])
  (:import [jakarta.servlet.http HttpServletRequest HttpServletResponse]
           [org.eclipse.jetty.server Request])
  (:gen-class
    :name ring.adapter.jetty9.handlers.SyncProxyHandler
    :extends org.eclipse.jetty.servlet.ServletHandler
    :state ringHandler
    :init init
    :constructors {[clojure.lang.IFn
                    clojure.lang.IPersistentMap] []}
    :prefix "-"))

(defn -init
  [ring-handler opts]
  [[] (with-meta ring-handler opts)])

(defn -doHandle
  "Synchronous override for `ServletHandler.doHandle"
  [^ring.adapter.jetty9.handlers.SyncProxyHandler this
   _
   ^Request base-request
   ^HttpServletRequest request
   ^HttpServletResponse response]
  (try
    (let [handler      (.ringHandler this)
          response-map (-> request
                           common/build-request-map
                           handler
                           common/normalize-response)]
      (if-let [ws (common/websocket-upgrade-response? response-map)]
        (ws/upgrade-websocket request response ws (meta handler))
        (servlet/update-servlet-response response response-map)))
    (catch Throwable e
      (.sendError response 500 (.getMessage e)))
    (finally
      (.setHandled base-request true))))
