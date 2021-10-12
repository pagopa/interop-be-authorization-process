[info] welcome to sbt 1.4.6 (AdoptOpenJDK Java 11.0.11)
[info] loading global plugins from /Users/galales/.sbt/1.0/plugins
[info] loading settings for project pdnd-interop-uservice-authorization-process-build from plugins.sbt ...
[info] loading project definition from /Users/galales/dev/pdnd-interop-uservice-authorization-process/project
[info] compiling 4 Scala sources to /Users/galales/dev/pdnd-interop-uservice-authorization-process/project/target/scala-2.12/sbt-1.0/classes ...
[info] done compiling
[info] loading settings for project root from build.sbt ...
[info] set current project to pdnd-interop-uservice-authorization-process (in build file:/Users/galales/dev/pdnd-interop-uservice-authorization-process/)
[info] it.pagopa:pdnd-interop-uservice-authorization-process-client_2.13:0.0.0
[info]   +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   |   
[info]   +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | +-com.typesafe:config:1.4.0
[info]   | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | 
[info]   | +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | | +-com.typesafe:config:1.4.0
[info]   | | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | | 
[info]   | +-org.reactivestreams:reactive-streams:1.0.3
[info]   | 
[info]   +-de.heikoseeberger:akka-http-json4s_2.13:1.38.2 [S]
[info]   | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | |   
[info]   | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   |   
[info]   +-org.json4s:json4s-ext_2.13:4.0.3 [S]
[info]   | +-joda-time:joda-time:2.10.10
[info]   | +-org.joda:joda-convert:2.2.1
[info]   | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   |   
[info]   +-org.json4s:json4s-jackson_2.13:4.0.3 [S]
[info]     +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]     | +-com.thoughtworks.paranamer:paranamer:2.8
[info]     | +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]     | +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]     | 
[info]     +-org.json4s:json4s-jackson-core_2.13:4.0.3 [S]
[info]       +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]       | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]       | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]       | 
[info]       +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]       
[warn] There may be incompatibilities among your library dependencies; run 'evicted' to see detailed eviction warnings.
[info] it.pagopa:pdnd-interop-uservice-authorization-process_2.13:0.0.0 [S]
[info]   +-ch.qos.logback:logback-classic:1.2.6
[info]   | +-ch.qos.logback:logback-core:1.2.6
[info]   | +-org.slf4j:slf4j-api:1.7.32
[info]   | 
[info]   +-com.bettercloud:vault-java-driver:5.1.0
[info]   +-com.github.spullara.mustache.java:compiler:0.9.10
[info]   +-com.lightbend.akka.management:akka-management_2.13:1.1.1
[info]   | +-com.typesafe.akka:akka-actor_2.13:2.6.14 (evicted by: 2.6.16)
[info]   | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | +-com.typesafe:config:1.4.0
[info]   | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | 
[info]   | +-com.typesafe.akka:akka-http-spray-json_2.13:10.2.0 (evicted by: 10.2.6)
[info]   | +-com.typesafe.akka:akka-http-spray-json_2.13:10.2.6 [S]
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | |   
[info]   | | +-io.spray:spray-json_2.13:1.3.6 [S]
[info]   | | 
[info]   | +-com.typesafe.akka:akka-http_2.13:10.2.0 (evicted by: 10.2.6)
[info]   | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | |   
[info]   | +-com.typesafe.akka:akka-stream_2.13:2.6.14 (evicted by: 2.6.16)
[info]   | +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   |   | +-com.typesafe:config:1.4.0
[info]   |   | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   |   | 
[info]   |   +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   |   +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   |   | +-com.typesafe:config:1.4.0
[info]   |   | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   |   | 
[info]   |   +-org.reactivestreams:reactive-streams:1.0.3
[info]   |   
[info]   +-com.nimbusds:nimbus-jose-jwt:9.15.2
[info]   | +-com.github.stephenc.jcip:jcip-annotations:1.0-1
[info]   | 
[info]   +-com.thesamet.scalapb:scalapb-runtime_2.13:0.11.5 [S]
[info]   | +-com.google.protobuf:protobuf-java:3.15.8
[info]   | +-com.thesamet.scalapb:lenses_2.13:0.11.5 [S]
[info]   | | +-org.scala-lang.modules:scala-collection-compat_2.13:2.5.0 [S]
[info]   | | 
[info]   | +-org.scala-lang.modules:scala-collection-compat_2.13:2.5.0 [S]
[info]   | 
[info]   +-com.typesafe.akka:akka-actor-typed_2.13:2.6.16 [S]
[info]   | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | +-com.typesafe:config:1.4.0
[info]   | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | 
[info]   | +-com.typesafe.akka:akka-slf4j_2.13:2.6.16 [S]
[info]   | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe:config:1.4.0
[info]   | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | 
[info]   | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | 
[info]   | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | +-org.slf4j:slf4j-api:1.7.32
[info]   | 
[info]   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | +-com.typesafe:config:1.4.0
[info]   | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | 
[info]   +-com.typesafe.akka:akka-cluster-tools_2.13:2.6.16 [S]
[info]   | +-com.typesafe.akka:akka-cluster_2.13:2.6.16 [S]
[info]   | | +-com.typesafe.akka:akka-coordination_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | |   +-com.typesafe:config:1.4.0
[info]   | | |   +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | |   
[info]   | | +-com.typesafe.akka:akka-remote_2.13:2.6.16 [S]
[info]   | |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | |   | +-com.typesafe:config:1.4.0
[info]   | |   | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | |   | 
[info]   | |   +-com.typesafe.akka:akka-pki_2.13:2.6.16 [S]
[info]   | |   | +-com.hierynomus:asn-one:0.5.0
[info]   | |   | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | |   | | +-com.typesafe:config:1.4.0
[info]   | |   | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | |   | | 
[info]   | |   | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | |   | +-org.slf4j:slf4j-api:1.7.32
[info]   | |   | 
[info]   | |   +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | |   | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | |   | | +-com.typesafe:config:1.4.0
[info]   | |   | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | |   | | 
[info]   | |   | +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | |   | +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | |   | | +-com.typesafe:config:1.4.0
[info]   | |   | | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | |   | | 
[info]   | |   | +-org.reactivestreams:reactive-streams:1.0.3
[info]   | |   | 
[info]   | |   +-org.agrona:agrona:1.9.0
[info]   | |   
[info]   | +-com.typesafe.akka:akka-coordination_2.13:2.6.16 [S]
[info]   |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   |     +-com.typesafe:config:1.4.0
[info]   |     +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   |     
[info]   +-com.typesafe.akka:akka-http-spray-json_2.13:10.2.6 [S]
[info]   | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | |   
[info]   | +-io.spray:spray-json_2.13:1.3.6 [S]
[info]   | 
[info]   +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   |   
[info]   +-com.typesafe.akka:akka-serialization-jackson_2.13:2.6.16 [S]
[info]   | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | 
[info]   | +-com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:2.11.4
[info]   | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | |   +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | |   +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |   
[info]   | +-com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.11.4
[info]   | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | |   +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | |   +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |   
[info]   | +-com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.4
[info]   | | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | |   +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | |   +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |   
[info]   | +-com.fasterxml.jackson.module:jackson-module-parameter-names:2.11.4
[info]   | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | |   +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | |   +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |   
[info]   | +-com.fasterxml.jackson.module:jackson-module-scala_2.13:2.11.4 [S]
[info]   | | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | | | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | | 
[info]   | | +-com.fasterxml.jackson.module:jackson-module-paranamer:2.11.4
[info]   | |   +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | |   | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | |   | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |   | 
[info]   | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | |   
[info]   | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | +-com.typesafe:config:1.4.0
[info]   | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | 
[info]   | +-org.lz4:lz4-java:1.8.0
[info]   | 
[info]   +-com.typesafe.akka:akka-slf4j_2.13:2.6.16 [S]
[info]   | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | +-com.typesafe:config:1.4.0
[info]   | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | 
[info]   | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | +-org.slf4j:slf4j-api:1.7.32
[info]   | 
[info]   +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | +-com.typesafe:config:1.4.0
[info]   | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | 
[info]   | +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | | +-com.typesafe:config:1.4.0
[info]   | | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | | 
[info]   | +-org.reactivestreams:reactive-streams:1.0.3
[info]   | 
[info]   +-io.kamon:kamon-bundle_2.13:2.2.3 [S]
[info]   | +-com.github.oshi:oshi-core:5.7.5
[info]   | | +-net.java.dev.jna:jna-platform:5.8.0
[info]   | | | +-net.java.dev.jna:jna:5.8.0
[info]   | | | 
[info]   | | +-net.java.dev.jna:jna:5.8.0
[info]   | | +-org.slf4j:slf4j-api:1.7.30 (evicted by: 1.7.32)
[info]   | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | 
[info]   | +-io.kamon:kamon-core_2.13:2.2.3 [S]
[info]   |   +-com.typesafe:config:1.3.1 (evicted by: 1.4.0)
[info]   |   +-com.typesafe:config:1.4.0
[info]   |   +-org.slf4j:slf4j-api:1.7.25 (evicted by: 1.7.32)
[info]   |   +-org.slf4j:slf4j-api:1.7.32
[info]   |   
[info]   +-io.kamon:kamon-prometheus_2.13:2.2.3 [S]
[info]   | +-com.squareup.okhttp3:okhttp:3.14.7
[info]   | | +-com.squareup.okio:okio:1.17.2
[info]   | | 
[info]   | +-io.kamon:kamon-core_2.13:2.2.3 [S]
[info]   |   +-com.typesafe:config:1.3.1 (evicted by: 1.4.0)
[info]   |   +-com.typesafe:config:1.4.0
[info]   |   +-org.slf4j:slf4j-api:1.7.25 (evicted by: 1.7.32)
[info]   |   +-org.slf4j:slf4j-api:1.7.32
[info]   |   
[info]   +-it.pagopa:generated_2.13:0.0.0 [S]
[info]   | +-ch.qos.logback:logback-classic:1.2.6
[info]   | | +-ch.qos.logback:logback-core:1.2.6
[info]   | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | 
[info]   | +-com.bettercloud:vault-java-driver:5.1.0
[info]   | +-com.github.spullara.mustache.java:compiler:0.9.10
[info]   | +-com.lightbend.akka.management:akka-management_2.13:1.1.1
[info]   | | +-com.typesafe.akka:akka-actor_2.13:2.6.14 (evicted by: 2.6.16)
[info]   | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe:config:1.4.0
[info]   | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | 
[info]   | | +-com.typesafe.akka:akka-http-spray-json_2.13:10.2.0 (evicted by: 10.2.6)
[info]   | | +-com.typesafe.akka:akka-http-spray-json_2.13:10.2.6 [S]
[info]   | | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | | |   
[info]   | | | +-io.spray:spray-json_2.13:1.3.6 [S]
[info]   | | | 
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.0 (evicted by: 10.2.6)
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | |   
[info]   | | +-com.typesafe.akka:akka-stream_2.13:2.6.14 (evicted by: 2.6.16)
[info]   | | +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | |   | +-com.typesafe:config:1.4.0
[info]   | |   | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | |   | 
[info]   | |   +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | |   +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | |   | +-com.typesafe:config:1.4.0
[info]   | |   | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | |   | 
[info]   | |   +-org.reactivestreams:reactive-streams:1.0.3
[info]   | |   
[info]   | +-com.nimbusds:nimbus-jose-jwt:9.15.2
[info]   | | +-com.github.stephenc.jcip:jcip-annotations:1.0-1
[info]   | | 
[info]   | +-com.typesafe.akka:akka-actor-typed_2.13:2.6.16 [S]
[info]   | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe:config:1.4.0
[info]   | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | 
[info]   | | +-com.typesafe.akka:akka-slf4j_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe:config:1.4.0
[info]   | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | 
[info]   | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | 
[info]   | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | 
[info]   | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | +-com.typesafe:config:1.4.0
[info]   | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | 
[info]   | +-com.typesafe.akka:akka-cluster-tools_2.13:2.6.16 [S]
[info]   | | +-com.typesafe.akka:akka-cluster_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe.akka:akka-coordination_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | |   +-com.typesafe:config:1.4.0
[info]   | | | |   +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | |   
[info]   | | | +-com.typesafe.akka:akka-remote_2.13:2.6.16 [S]
[info]   | | |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | |   | +-com.typesafe:config:1.4.0
[info]   | | |   | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | |   | 
[info]   | | |   +-com.typesafe.akka:akka-pki_2.13:2.6.16 [S]
[info]   | | |   | +-com.hierynomus:asn-one:0.5.0
[info]   | | |   | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | |   | | +-com.typesafe:config:1.4.0
[info]   | | |   | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | |   | | 
[info]   | | |   | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | |   | +-org.slf4j:slf4j-api:1.7.32
[info]   | | |   | 
[info]   | | |   +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | | |   | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | |   | | +-com.typesafe:config:1.4.0
[info]   | | |   | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | |   | | 
[info]   | | |   | +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | | |   | +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | | |   | | +-com.typesafe:config:1.4.0
[info]   | | |   | | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | | |   | | 
[info]   | | |   | +-org.reactivestreams:reactive-streams:1.0.3
[info]   | | |   | 
[info]   | | |   +-org.agrona:agrona:1.9.0
[info]   | | |   
[info]   | | +-com.typesafe.akka:akka-coordination_2.13:2.6.16 [S]
[info]   | |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | |     +-com.typesafe:config:1.4.0
[info]   | |     +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | |     
[info]   | +-com.typesafe.akka:akka-http-spray-json_2.13:10.2.6 [S]
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | |   
[info]   | | +-io.spray:spray-json_2.13:1.3.6 [S]
[info]   | | 
[info]   | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | |   
[info]   | +-com.typesafe.akka:akka-serialization-jackson_2.13:2.6.16 [S]
[info]   | | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | | | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | | 
[info]   | | +-com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:2.11.4
[info]   | | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | | +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | | |   +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | |   +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | |   
[info]   | | +-com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.11.4
[info]   | | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | | +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | | |   +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | |   +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | |   
[info]   | | +-com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.4
[info]   | | | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | | +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | | |   +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | |   +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | |   
[info]   | | +-com.fasterxml.jackson.module:jackson-module-parameter-names:2.11.4
[info]   | | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | | +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | | |   +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | |   +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | |   
[info]   | | +-com.fasterxml.jackson.module:jackson-module-scala_2.13:2.11.4 [S]
[info]   | | | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | | +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | | | | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | | | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | | | 
[info]   | | | +-com.fasterxml.jackson.module:jackson-module-paranamer:2.11.4
[info]   | | |   +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | | |   | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | |   | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | |   | 
[info]   | | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | | |   
[info]   | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe:config:1.4.0
[info]   | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | 
[info]   | | +-org.lz4:lz4-java:1.8.0
[info]   | | 
[info]   | +-com.typesafe.akka:akka-slf4j_2.13:2.6.16 [S]
[info]   | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe:config:1.4.0
[info]   | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | 
[info]   | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | 
[info]   | +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe:config:1.4.0
[info]   | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | 
[info]   | | +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | | +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | | | +-com.typesafe:config:1.4.0
[info]   | | | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | | | 
[info]   | | +-org.reactivestreams:reactive-streams:1.0.3
[info]   | | 
[info]   | +-io.kamon:kamon-bundle_2.13:2.2.3 [S]
[info]   | | +-com.github.oshi:oshi-core:5.7.5
[info]   | | | +-net.java.dev.jna:jna-platform:5.8.0
[info]   | | | | +-net.java.dev.jna:jna:5.8.0
[info]   | | | | 
[info]   | | | +-net.java.dev.jna:jna:5.8.0
[info]   | | | +-org.slf4j:slf4j-api:1.7.30 (evicted by: 1.7.32)
[info]   | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | 
[info]   | | +-io.kamon:kamon-core_2.13:2.2.3 [S]
[info]   | |   +-com.typesafe:config:1.3.1 (evicted by: 1.4.0)
[info]   | |   +-com.typesafe:config:1.4.0
[info]   | |   +-org.slf4j:slf4j-api:1.7.25 (evicted by: 1.7.32)
[info]   | |   +-org.slf4j:slf4j-api:1.7.32
[info]   | |   
[info]   | +-io.kamon:kamon-prometheus_2.13:2.2.3 [S]
[info]   | | +-com.squareup.okhttp3:okhttp:3.14.7
[info]   | | | +-com.squareup.okio:okio:1.17.2
[info]   | | | 
[info]   | | +-io.kamon:kamon-core_2.13:2.2.3 [S]
[info]   | |   +-com.typesafe:config:1.3.1 (evicted by: 1.4.0)
[info]   | |   +-com.typesafe:config:1.4.0
[info]   | |   +-org.slf4j:slf4j-api:1.7.25 (evicted by: 1.7.32)
[info]   | |   +-org.slf4j:slf4j-api:1.7.32
[info]   | |   
[info]   | +-it.pagopa:pdnd-interop-uservice-agreement-management-client_2.13:0.1.0-S..
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.4 (evicted by: 10.2.6)
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | |   
[info]   | | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.15 (evicted by: 2.6.16)
[info]   | | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe.akka:akka-actor-typed_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | | +-com.typesafe:config:1.4.0
[info]   | | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | | 
[info]   | | | | +-com.typesafe.akka:akka-slf4j_2.13:2.6.16 [S]
[info]   | | | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | | | +-com.typesafe:config:1.4.0
[info]   | | | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | | | 
[info]   | | | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | | | 
[info]   | | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | | 
[info]   | | | +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | | |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | |   | +-com.typesafe:config:1.4.0
[info]   | | |   | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | |   | 
[info]   | | |   +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | | |   +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | | |   | +-com.typesafe:config:1.4.0
[info]   | | |   | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | | |   | 
[info]   | | |   +-org.reactivestreams:reactive-streams:1.0.3
[info]   | | |   
[info]   | | +-de.heikoseeberger:akka-http-json4s_2.13:1.37.0 (evicted by: 1.38.2)
[info]   | | +-de.heikoseeberger:akka-http-json4s_2.13:1.38.2 [S]
[info]   | | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | | |   
[info]   | | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | | |   
[info]   | | +-org.json4s:json4s-ext_2.13:4.0.3 [S]
[info]   | | | +-joda-time:joda-time:2.10.10
[info]   | | | +-org.joda:joda-convert:2.2.1
[info]   | | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | | |   
[info]   | | +-org.json4s:json4s-jackson_2.13:4.0.3 [S]
[info]   | |   +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | |   | +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | |   | +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |   | +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | |   | 
[info]   | |   +-org.json4s:json4s-jackson-core_2.13:4.0.3 [S]
[info]   | |     +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | |     | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | |     | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |     | 
[info]   | |     +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |     
[info]   | +-it.pagopa:pdnd-interop-uservice-catalog-management-client_2.13:0.1.0-SNA..
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.4 (evicted by: 10.2.6)
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | |   
[info]   | | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.15 (evicted by: 2.6.16)
[info]   | | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe.akka:akka-actor-typed_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | | +-com.typesafe:config:1.4.0
[info]   | | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | | 
[info]   | | | | +-com.typesafe.akka:akka-slf4j_2.13:2.6.16 [S]
[info]   | | | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | | | +-com.typesafe:config:1.4.0
[info]   | | | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | | | 
[info]   | | | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | | | 
[info]   | | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | | 
[info]   | | | +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | | |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | |   | +-com.typesafe:config:1.4.0
[info]   | | |   | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | |   | 
[info]   | | |   +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | | |   +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | | |   | +-com.typesafe:config:1.4.0
[info]   | | |   | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | | |   | 
[info]   | | |   +-org.reactivestreams:reactive-streams:1.0.3
[info]   | | |   
[info]   | | +-de.heikoseeberger:akka-http-json4s_2.13:1.37.0 (evicted by: 1.38.2)
[info]   | | +-de.heikoseeberger:akka-http-json4s_2.13:1.38.2 [S]
[info]   | | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | | |   
[info]   | | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | | |   
[info]   | | +-org.json4s:json4s-ext_2.13:4.0.3 [S]
[info]   | | | +-joda-time:joda-time:2.10.10
[info]   | | | +-org.joda:joda-convert:2.2.1
[info]   | | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | | |   
[info]   | | +-org.json4s:json4s-jackson_2.13:4.0.3 [S]
[info]   | |   +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | |   | +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | |   | +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |   | +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | |   | 
[info]   | |   +-org.json4s:json4s-jackson-core_2.13:4.0.3 [S]
[info]   | |     +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | |     | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | |     | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |     | 
[info]   | |     +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |     
[info]   | +-it.pagopa:pdnd-interop-uservice-key-management-client_2.13:0.0.0
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | |   
[info]   | | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe.akka:akka-actor-typed_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | | +-com.typesafe:config:1.4.0
[info]   | | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | | 
[info]   | | | | +-com.typesafe.akka:akka-slf4j_2.13:2.6.16 [S]
[info]   | | | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | | | +-com.typesafe:config:1.4.0
[info]   | | | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | | | 
[info]   | | | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | | | 
[info]   | | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | | 
[info]   | | | +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | | |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | |   | +-com.typesafe:config:1.4.0
[info]   | | |   | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | |   | 
[info]   | | |   +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | | |   +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | | |   | +-com.typesafe:config:1.4.0
[info]   | | |   | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | | |   | 
[info]   | | |   +-org.reactivestreams:reactive-streams:1.0.3
[info]   | | |   
[info]   | | +-de.heikoseeberger:akka-http-json4s_2.13:1.38.2 [S]
[info]   | | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | | |   
[info]   | | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | | |   
[info]   | | +-org.json4s:json4s-ext_2.13:4.0.2 (evicted by: 4.0.3)
[info]   | | +-org.json4s:json4s-ext_2.13:4.0.3 [S]
[info]   | | | +-joda-time:joda-time:2.10.10
[info]   | | | +-org.joda:joda-convert:2.2.1
[info]   | | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | | |   
[info]   | | +-org.json4s:json4s-jackson_2.13:4.0.2 (evicted by: 4.0.3)
[info]   | | +-org.json4s:json4s-jackson_2.13:4.0.3 [S]
[info]   | |   +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | |   | +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | |   | +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |   | +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | |   | 
[info]   | |   +-org.json4s:json4s-jackson-core_2.13:4.0.3 [S]
[info]   | |     +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | |     | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | |     | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |     | 
[info]   | |     +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |     
[info]   | +-it.pagopa:pdnd-interop-uservice-party-management-client_2.13:0.1.0-SNAPS..
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.4 (evicted by: 10.2.6)
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | |   
[info]   | | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.15 (evicted by: 2.6.16)
[info]   | | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe.akka:akka-actor-typed_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | | +-com.typesafe:config:1.4.0
[info]   | | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | | 
[info]   | | | | +-com.typesafe.akka:akka-slf4j_2.13:2.6.16 [S]
[info]   | | | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | | | +-com.typesafe:config:1.4.0
[info]   | | | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | | | 
[info]   | | | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | | | 
[info]   | | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | | 
[info]   | | | +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | | |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | |   | +-com.typesafe:config:1.4.0
[info]   | | |   | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | |   | 
[info]   | | |   +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | | |   +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | | |   | +-com.typesafe:config:1.4.0
[info]   | | |   | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | | |   | 
[info]   | | |   +-org.reactivestreams:reactive-streams:1.0.3
[info]   | | |   
[info]   | | +-de.heikoseeberger:akka-http-json4s_2.13:1.37.0 (evicted by: 1.38.2)
[info]   | | +-de.heikoseeberger:akka-http-json4s_2.13:1.38.2 [S]
[info]   | | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | | |   
[info]   | | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | | |   
[info]   | | +-org.json4s:json4s-ext_2.13:4.0.1 (evicted by: 4.0.3)
[info]   | | +-org.json4s:json4s-ext_2.13:4.0.3 [S]
[info]   | | | +-joda-time:joda-time:2.10.10
[info]   | | | +-org.joda:joda-convert:2.2.1
[info]   | | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | | |   
[info]   | | +-org.json4s:json4s-jackson_2.13:4.0.1 (evicted by: 4.0.3)
[info]   | | +-org.json4s:json4s-jackson_2.13:4.0.3 [S]
[info]   | |   +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | |   | +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | |   | +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |   | +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | |   | 
[info]   | |   +-org.json4s:json4s-jackson-core_2.13:4.0.3 [S]
[info]   | |     +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | |     | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | |     | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |     | 
[info]   | |     +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |     
[info]   | +-javax.annotation:javax.annotation-api:1.3.2
[info]   | +-org.bouncycastle:bcpkix-jdk15on:1.69
[info]   | | +-org.bouncycastle:bcprov-jdk15on:1.69
[info]   | | +-org.bouncycastle:bcutil-jdk15on:1.69
[info]   | |   +-org.bouncycastle:bcprov-jdk15on:1.69
[info]   | |   
[info]   | +-org.bouncycastle:bcprov-jdk15on:1.69
[info]   | +-org.openapi4j:openapi-operation-validator:1.0.7
[info]   | | +-org.openapi4j:openapi-parser:1.0.7
[info]   | | | +-org.openapi4j:openapi-core:1.0.7
[info]   | | |   +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | | |   | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | |   | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | |   | 
[info]   | | |   +-com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.1
[info]   | | |     +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | |     +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | | |     | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | | |     | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | | |     | 
[info]   | | |     +-org.yaml:snakeyaml:1.27
[info]   | | |     
[info]   | | +-org.openapi4j:openapi-schema-validator:1.0.7
[info]   | |   +-org.openapi4j:openapi-core:1.0.7
[info]   | |     +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | |     | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | |     | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |     | 
[info]   | |     +-com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.1
[info]   | |       +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |       +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | |       | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | |       | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |       | 
[info]   | |       +-org.yaml:snakeyaml:1.27
[info]   | |       
[info]   | +-org.typelevel:cats-core_2.13:2.6.1 [S]
[info]   |   +-org.typelevel:cats-kernel_2.13:2.6.1 [S]
[info]   |   +-org.typelevel:simulacrum-scalafix-annotations_2.13:0.5.4 [S]
[info]   |   
[info]   +-it.pagopa:pdnd-interop-uservice-agreement-management-client_2.13:0.1.0-SNA..
[info]   | +-com.typesafe.akka:akka-http_2.13:10.2.4 (evicted by: 10.2.6)
[info]   | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | |   
[info]   | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.15 (evicted by: 2.6.16)
[info]   | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.16 [S]
[info]   | | +-com.typesafe.akka:akka-actor-typed_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe:config:1.4.0
[info]   | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | 
[info]   | | | +-com.typesafe.akka:akka-slf4j_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | | +-com.typesafe:config:1.4.0
[info]   | | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | | 
[info]   | | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | | 
[info]   | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | 
[info]   | | +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | |   | +-com.typesafe:config:1.4.0
[info]   | |   | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | |   | 
[info]   | |   +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | |   +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | |   | +-com.typesafe:config:1.4.0
[info]   | |   | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | |   | 
[info]   | |   +-org.reactivestreams:reactive-streams:1.0.3
[info]   | |   
[info]   | +-de.heikoseeberger:akka-http-json4s_2.13:1.37.0 (evicted by: 1.38.2)
[info]   | +-de.heikoseeberger:akka-http-json4s_2.13:1.38.2 [S]
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | |   
[info]   | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | |   
[info]   | +-org.json4s:json4s-ext_2.13:4.0.3 [S]
[info]   | | +-joda-time:joda-time:2.10.10
[info]   | | +-org.joda:joda-convert:2.2.1
[info]   | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | |   
[info]   | +-org.json4s:json4s-jackson_2.13:4.0.3 [S]
[info]   |   +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   |   | +-com.thoughtworks.paranamer:paranamer:2.8
[info]   |   | +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   |   | +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   |   | 
[info]   |   +-org.json4s:json4s-jackson-core_2.13:4.0.3 [S]
[info]   |     +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   |     | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   |     | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   |     | 
[info]   |     +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   |     
[info]   +-it.pagopa:pdnd-interop-uservice-catalog-management-client_2.13:0.1.0-SNAPS..
[info]   | +-com.typesafe.akka:akka-http_2.13:10.2.4 (evicted by: 10.2.6)
[info]   | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | |   
[info]   | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.15 (evicted by: 2.6.16)
[info]   | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.16 [S]
[info]   | | +-com.typesafe.akka:akka-actor-typed_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe:config:1.4.0
[info]   | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | 
[info]   | | | +-com.typesafe.akka:akka-slf4j_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | | +-com.typesafe:config:1.4.0
[info]   | | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | | 
[info]   | | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | | 
[info]   | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | 
[info]   | | +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | |   | +-com.typesafe:config:1.4.0
[info]   | |   | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | |   | 
[info]   | |   +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | |   +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | |   | +-com.typesafe:config:1.4.0
[info]   | |   | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | |   | 
[info]   | |   +-org.reactivestreams:reactive-streams:1.0.3
[info]   | |   
[info]   | +-de.heikoseeberger:akka-http-json4s_2.13:1.37.0 (evicted by: 1.38.2)
[info]   | +-de.heikoseeberger:akka-http-json4s_2.13:1.38.2 [S]
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | |   
[info]   | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | |   
[info]   | +-org.json4s:json4s-ext_2.13:4.0.3 [S]
[info]   | | +-joda-time:joda-time:2.10.10
[info]   | | +-org.joda:joda-convert:2.2.1
[info]   | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | |   
[info]   | +-org.json4s:json4s-jackson_2.13:4.0.3 [S]
[info]   |   +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   |   | +-com.thoughtworks.paranamer:paranamer:2.8
[info]   |   | +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   |   | +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   |   | 
[info]   |   +-org.json4s:json4s-jackson-core_2.13:4.0.3 [S]
[info]   |     +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   |     | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   |     | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   |     | 
[info]   |     +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   |     
[info]   +-it.pagopa:pdnd-interop-uservice-key-management-client_2.13:0.0.0
[info]   | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | |   
[info]   | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.16 [S]
[info]   | | +-com.typesafe.akka:akka-actor-typed_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe:config:1.4.0
[info]   | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | 
[info]   | | | +-com.typesafe.akka:akka-slf4j_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | | +-com.typesafe:config:1.4.0
[info]   | | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | | 
[info]   | | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | | 
[info]   | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | 
[info]   | | +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | |   | +-com.typesafe:config:1.4.0
[info]   | |   | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | |   | 
[info]   | |   +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | |   +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | |   | +-com.typesafe:config:1.4.0
[info]   | |   | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | |   | 
[info]   | |   +-org.reactivestreams:reactive-streams:1.0.3
[info]   | |   
[info]   | +-de.heikoseeberger:akka-http-json4s_2.13:1.38.2 [S]
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | |   
[info]   | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | |   
[info]   | +-org.json4s:json4s-ext_2.13:4.0.2 (evicted by: 4.0.3)
[info]   | +-org.json4s:json4s-ext_2.13:4.0.3 [S]
[info]   | | +-joda-time:joda-time:2.10.10
[info]   | | +-org.joda:joda-convert:2.2.1
[info]   | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | |   
[info]   | +-org.json4s:json4s-jackson_2.13:4.0.2 (evicted by: 4.0.3)
[info]   | +-org.json4s:json4s-jackson_2.13:4.0.3 [S]
[info]   |   +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   |   | +-com.thoughtworks.paranamer:paranamer:2.8
[info]   |   | +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   |   | +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   |   | 
[info]   |   +-org.json4s:json4s-jackson-core_2.13:4.0.3 [S]
[info]   |     +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   |     | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   |     | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   |     | 
[info]   |     +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   |     
[info]   +-it.pagopa:pdnd-interop-uservice-party-management-client_2.13:0.1.0-SNAPSHOT
[info]   | +-com.typesafe.akka:akka-http_2.13:10.2.4 (evicted by: 10.2.6)
[info]   | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | |   
[info]   | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.15 (evicted by: 2.6.16)
[info]   | +-com.typesafe.akka:akka-stream-typed_2.13:2.6.16 [S]
[info]   | | +-com.typesafe.akka:akka-actor-typed_2.13:2.6.16 [S]
[info]   | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe:config:1.4.0
[info]   | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | 
[info]   | | | +-com.typesafe.akka:akka-slf4j_2.13:2.6.16 [S]
[info]   | | | | +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | | | | | +-com.typesafe:config:1.4.0
[info]   | | | | | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | | | | | 
[info]   | | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | | 
[info]   | | | +-org.slf4j:slf4j-api:1.7.31 (evicted by: 1.7.32)
[info]   | | | +-org.slf4j:slf4j-api:1.7.32
[info]   | | | 
[info]   | | +-com.typesafe.akka:akka-stream_2.13:2.6.16 [S]
[info]   | |   +-com.typesafe.akka:akka-actor_2.13:2.6.16 [S]
[info]   | |   | +-com.typesafe:config:1.4.0
[info]   | |   | +-org.scala-lang.modules:scala-java8-compat_2.13:1.0.0 [S]
[info]   | |   | 
[info]   | |   +-com.typesafe.akka:akka-protobuf-v3_2.13:2.6.16
[info]   | |   +-com.typesafe:ssl-config-core_2.13:0.4.2 [S]
[info]   | |   | +-com.typesafe:config:1.4.0
[info]   | |   | +-org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2 [S]
[info]   | |   | 
[info]   | |   +-org.reactivestreams:reactive-streams:1.0.3
[info]   | |   
[info]   | +-de.heikoseeberger:akka-http-json4s_2.13:1.37.0 (evicted by: 1.38.2)
[info]   | +-de.heikoseeberger:akka-http-json4s_2.13:1.38.2 [S]
[info]   | | +-com.typesafe.akka:akka-http_2.13:10.2.6 [S]
[info]   | | | +-com.typesafe.akka:akka-http-core_2.13:10.2.6 [S]
[info]   | | |   +-com.typesafe.akka:akka-parsing_2.13:10.2.6 [S]
[info]   | | |   
[info]   | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | |   
[info]   | +-org.json4s:json4s-ext_2.13:4.0.1 (evicted by: 4.0.3)
[info]   | +-org.json4s:json4s-ext_2.13:4.0.3 [S]
[info]   | | +-joda-time:joda-time:2.10.10
[info]   | | +-org.joda:joda-convert:2.2.1
[info]   | | +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   | |   +-com.thoughtworks.paranamer:paranamer:2.8
[info]   | |   +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   | |   +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   | |   
[info]   | +-org.json4s:json4s-jackson_2.13:4.0.1 (evicted by: 4.0.3)
[info]   | +-org.json4s:json4s-jackson_2.13:4.0.3 [S]
[info]   |   +-org.json4s:json4s-core_2.13:4.0.3 [S]
[info]   |   | +-com.thoughtworks.paranamer:paranamer:2.8
[info]   |   | +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   |   | +-org.json4s:json4s-scalap_2.13:4.0.3 [S]
[info]   |   | 
[info]   |   +-org.json4s:json4s-jackson-core_2.13:4.0.3 [S]
[info]   |     +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   |     | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   |     | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   |     | 
[info]   |     +-org.json4s:json4s-ast_2.13:4.0.3 [S]
[info]   |     
[info]   +-javax.annotation:javax.annotation-api:1.3.2
[info]   +-org.bouncycastle:bcpkix-jdk15on:1.69
[info]   | +-org.bouncycastle:bcprov-jdk15on:1.69
[info]   | +-org.bouncycastle:bcutil-jdk15on:1.69
[info]   |   +-org.bouncycastle:bcprov-jdk15on:1.69
[info]   |   
[info]   +-org.bouncycastle:bcprov-jdk15on:1.69
[info]   +-org.openapi4j:openapi-operation-validator:1.0.7
[info]   | +-org.openapi4j:openapi-parser:1.0.7
[info]   | | +-org.openapi4j:openapi-core:1.0.7
[info]   | |   +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | |   | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | |   | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |   | 
[info]   | |   +-com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.1
[info]   | |     +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |     +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   | |     | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   | |     | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   | |     | 
[info]   | |     +-org.yaml:snakeyaml:1.27
[info]   | |     
[info]   | +-org.openapi4j:openapi-schema-validator:1.0.7
[info]   |   +-org.openapi4j:openapi-core:1.0.7
[info]   |     +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   |     | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   |     | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   |     | 
[info]   |     +-com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.1
[info]   |       +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   |       +-com.fasterxml.jackson.core:jackson-databind:2.13.0
[info]   |       | +-com.fasterxml.jackson.core:jackson-annotations:2.13.0
[info]   |       | +-com.fasterxml.jackson.core:jackson-core:2.13.0
[info]   |       | 
[info]   |       +-org.yaml:snakeyaml:1.27
[info]   |       
[info]   +-org.typelevel:cats-core_2.13:2.6.1 [S]
[info]     +-org.typelevel:cats-kernel_2.13:2.6.1 [S]
[info]     +-org.typelevel:simulacrum-scalafix-annotations_2.13:0.5.4 [S]
[info]     
[success] Total time: 2 s, completed Oct 12, 2021, 6:51:48 PM
