# Syksy

This is my base setup for Clojure/ClojureScript projects, the way I like it.

This library provides default integrant components and config for developing web apps.

## Doctrine

Syksy has plenty of opinions, if you have different opinions then this
library is probably not for you.

Most notably, following choices have been made:

* Logging is (logback)[https://logback.qos.ch/] via (slf4j)[https://www.slf4j.org/]
* Web server is (immutant)[http://immutant.org/tutorials/web/index.html] (uses (undertow)[http://undertow.io/])
* Components are (integrant)[https://github.com/weavejester/integrant] components
* Workflow is done in `user` namespace, and it's included in the lib
* main function for überjar is also included in lib
* HTTP content negotiation and data formatting is done by (metosin/muuntaja)[https://github.com/metosin/muuntaja]
* JSON formatting is done by (metosin/jsonista)[https://github.com/metosin/jsonista]
* Resources are served in handler provided by the lib
* Caching headers

Syksy is pretty much (omakase)[http://rubyonrails.org/doctrine/#omakase], most of the common decisions
and bootstrapping is done for you.

## Develop

... todo ...

## To-do

* proper documentation
* all those mysterious `X-...` security headers
* frontend setup
* lein template
* integrate with eines
* dev mode cache headers, in index etc

## License

Copyright © 2018 Jarppe

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
