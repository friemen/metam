(ns samples.wsdl.generator
  (:require [samples.wsdl.metamodel :as wsdl])
  (:use [metam.textgen]
        [metam.core :only [metatype? metatype]]))

(defn base-ns [service] (gen-with service "http://acme.com/sample/" :name))

;; XML Schema generator

(defn xsd-target-ns [service] (str (base-ns service) "/types"))

(defmulti t-typeref metatype)
(defmethod t-typeref ::wsdl/complextype [type] (gen-with type :name "Type"))
(defmethod t-typeref ::wsdl/simpletype [type] (gen-with type "xsd:" :name))


(defn t-element [e]
  (gen-with e "        <xsd:element name='" :name "' type='" (t-typeref (:type e)) "'"
            (if (= (:mult e) :many) (gen-with e " minOccurs='0' maxOccurs='unbounded'"))
            "/>"))

(defn t-complextype [dt]
  (gen-with dt "
    <xsd:complexType name='" :name "Type'>
    <xsd:sequence>
        " (gen-map t-element (:elements dt) "\n        ") "
    </xsd:sequence>
    </xsd:complexType>
"))

(defn t-schema [service]
  {:pre [(metatype? ::wsdl/service service)]}
  (gen-with service
"<xsd:schema 
        xmlns:xsd='http://www.w3.org/2001/XMLSchema' 
        xmlns:xmime='http://www.w3.org/2005/05/xmlmime'
	targetNamespace='" xsd-target-ns "'
	xmlns='" xsd-target-ns "'>
    " (gen-map t-complextype (filter #(= ::wsdl/complextype (metatype %)) (wsdl/referenced-types service)) "\n    ")
"</xsd:schema>"))



;; WSDL generator

(defn wsdl-target-ns [service] (str (base-ns service) "/service"))
(defn service-name [service] (gen-with service :name "Service"))
(defn porttype-name [service] (gen-with service :name "PT"))
(defn binding-name [service] (gen-with service service-name "Binding"))
(defn port-name [service] (gen-with service service-name "Port"))

(defn t-op-element [op suffix elements]
  (if-not (empty? elements)
    (gen-with op "
            <xsd:element name='" :name suffix "'>
                <xsd:complexType>
                <xsd:sequence>
            " (gen-map t-element elements "\n            ") "
                </xsd:sequence>
                </xsd:complexType>
            </xsd:element>")))

(defn t-op-elements [op]
  (str
   (t-op-element op "Request" (:in-elements op))
   (t-op-element op "Response" (:out-elements op))))

(defn t-types [service]
  (gen-with service "
    <wsdl:types>
        <xsd:schema targetNamespace='" wsdl-target-ns "'>
            <xsd:import schemaLocation='' namespace='" xsd-target-ns "'/>"
            (gen-map t-op-elements (:operations service) "\n        ")
        "</xsd:schema>
    </wsdl:types>"))

(defn t-op-message [op]
  (gen-with op "
    <wsdl:message name='" :name  "Request'>
    " (if-not (empty? (:in-elements op)) (gen-with op "
        <wsdl:part name='request' element='tns:" :name  "Request'/>\n    "))
    "</wsdl:message>"
    (if-not (empty? (:out-elements op)) (gen-with op "
    <wsdl:message name='" :name "Response'>
        <wsdl:part name='response' element='tns:" :name "Response'/>
    </wsdl:message>"))))

(defn t-messages [service]
  (gen-map t-op-message (:operations service)))

(defn t-op-porttype [op]
  (gen-with op "
            <wsdl:operation name='" :name "'>
                <wsdl:input name='" :name "Request' message='tns:" :name "Request' />
                " (if-not (empty? (:out-elements op)) (gen-with op "<wsdl:output name='" :name "Response' message='tns:" :name "Response' />")) "
                <wsdl:fault name='soapFaultMessage' message='tns:soapFaultMessage'/>
            </wsdl:operation>"))

(defn t-porttype [service]
  (gen-with service "
    <wsdl:portType name='" porttype-name "'>"
    (gen-map t-op-porttype (service :operations) "\n       ") "
    </wsdl:portType>"))

(defn t-op-binding [target-ns op]
  (gen-with op "
        <wsdl:operation name='" :name "'>
            <soap:operation soapAction='" target-ns "/" :name "' />
            " (if-not (empty? (:in-elements op)) (gen-with op "
            <wsdl:input name='" :name "Request'>
                <soap:body use='literal' />
            </wsdl:input>
            ")) "
            " (if-not (empty? (:out-elements op)) (gen-with op "
            <wsdl:output name='" :name "Response'>
                <soap:body use='literal' />
            </wsdl:output>
            ")) "
            <wsdl:fault name='soapFaultMessage'>
                <soap:fault name='soapFaultMessage' use='literal'/>
            </wsdl:fault>
        </wsdl:operation>"))

(defn t-binding [service]
  (gen-with service "
    <wsdl:binding name='" binding-name "' type='tns:" porttype-name "'>
        <soap:binding style='document' transport='http://schemas.xmlsoap.org/soap/http' />
        " (gen-map (partial t-op-binding (wsdl-target-ns service)) (:operations service) "\n       ") "
    </wsdl:binding>"))

(defn t-service [service]
  (gen-with service "
    <wsdl:service name='" service-name "'>
        <wsdl:port binding='tns:" binding-name "' name='" port-name "'>
            <soap:address location='http://localhost:8080/app/services/" porttype-name "'/>
        </wsdl:port>
   </wsdl:service>"))

(defn t-wsdl [service]
  {:pre [(metatype? ::wsdl/service service)]}
  (gen-with service "
<wsdl:definitions name='" service-name "'
    targetNamespace='" wsdl-target-ns "'
    xmlns:tns='" wsdl-target-ns "'
    xmlns:wsdl='http://schemas.xmlsoap.org/wsdl/'
    xmlns:xsd='http://www.w3.org/2001/XMLSchema'
    xmlns:soap='http://schemas.xmlsoap.org/wsdl/soap/'
    xmlns:xmime='http://www.w3.org/2005/05/xmlmime'
    xmlns:jms='http://cxf.apache.org/transports/jms'
   >
" t-types "
" t-messages "
" t-porttype "
" t-binding "
" t-service "
</wsdl:definitions>"))
