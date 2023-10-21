import PagopaVersions._
import Versions._
import sbt._

object Dependencies {

  private[this] object akka {
    lazy val namespace     = "com.typesafe.akka"
    lazy val actorTyped    = namespace                       %% "akka-actor-typed"           % akkaVersion
    lazy val actor         = namespace                       %% "akka-actor"                 % akkaVersion
    lazy val serialization = namespace                       %% "akka-serialization-jackson" % akkaVersion
    lazy val stream        = namespace                       %% "akka-stream"                % akkaVersion
    lazy val clusterTools  = namespace                       %% "akka-cluster-tools"         % akkaVersion
    lazy val http          = namespace                       %% "akka-http"                  % akkaHttpVersion
    lazy val httpJson      = namespace                       %% "akka-http-spray-json"       % akkaHttpVersion
    lazy val httpJson4s    = "de.heikoseeberger"             %% "akka-http-json4s"           % akkaHttpJson4sVersion
    lazy val management    = "com.lightbend.akka.management" %% "akka-management"            % akkaManagementVersion
    lazy val managementLogLevels =
      "com.lightbend.akka.management" %% "akka-management-loglevels-logback" % akkaManagementVersion
    lazy val slf4j         = namespace %% "akka-slf4j"          % akkaVersion
    lazy val httpTestkit   = namespace %% "akka-http-testkit"   % akkaHttpVersion
    lazy val streamTestkit = namespace %% "akka-stream-testkit" % akkaVersion
    lazy val testkit       = namespace %% "akka-testkit"        % akkaVersion

  }

  private[this] object pagopa {
    lazy val namespace               = "it.pagopa"
    lazy val authorizationManagementClient =
      namespace %% "interop-be-authorization-management-client" % authorizationManagementVersion
    lazy val authorizationManagementModels =
      namespace %% "interop-be-authorization-management-models" % authorizationManagementVersion
    lazy val authorizationManagementKeyConverter=
      namespace %% "interop-be-authorization-management-keyconverter" % authorizationManagementVersion
    lazy val agreementManagementModels =
      namespace %% "interop-be-agreement-management-models" % agreementManagementVersion
    lazy val catalogManagementModels =
      namespace %% "interop-be-catalog-management-models" % catalogManagementVersion
    lazy val tenantManagementModels =
      namespace %% "interop-be-tenant-management-models" % tenantManagementVersion
    lazy val purposeManagementModels =
      namespace %% "interop-be-purpose-management-models" % purposeManagementVersion
    lazy val selfcareV2Client =
      namespace %% "interop-selfcare-v2-client" % selfcareV2ClientVersion

    lazy val commonsUtils = namespace %% "interop-commons-utils" % commonsVersion
    lazy val commonsJwt   = namespace %% "interop-commons-jwt"   % commonsVersion
    lazy val commonsCqrs  = namespace %% "interop-commons-cqrs"  % commonsVersion
  }

  private[this] object nimbus {
    lazy val namespace = "com.nimbusds"
    lazy val joseJwt   = namespace % "nimbus-jose-jwt" % nimbusVersion
  }

  private[this] object bouncycastle {
    lazy val namespace = "org.bouncycastle"
    lazy val provider  = namespace % "bcprov-jdk15on" % bouncycastleVersion
    lazy val kix       = namespace % "bcpkix-jdk15on" % bouncycastleVersion
  }

  private[this] object scalpb {
    lazy val namespace = "com.thesamet.scalapb"
    lazy val core      = namespace %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion
  }

  private[this] object cats {
    lazy val namespace = "org.typelevel"
    lazy val core      = namespace %% "cats-core" % catsVersion
  }

  private[this] object json4s {
    lazy val namespace = "org.json4s"
    lazy val jackson   = namespace %% "json4s-jackson" % json4sVersion
    lazy val ext       = namespace %% "json4s-ext"     % json4sVersion
  }

  private[this] object logback {
    lazy val namespace = "ch.qos.logback"
    lazy val classic   = namespace % "logback-classic" % logbackVersion
  }

  private[this] object mustache {
    lazy val mustache = "com.github.spullara.mustache.java" % "compiler" % mustacheVersion
  }

  private[this] object scalatest {
    lazy val namespace = "org.scalatest"
    lazy val core      = namespace %% "scalatest" % scalatestVersion
  }

  private[this] object scalamock {
    lazy val namespace = "org.scalamock"
    lazy val core      = namespace %% "scalamock" % scalaMockVersion
  }

  private[this] object jackson {
    lazy val namespace   = "com.fasterxml.jackson.core"
    lazy val core        = namespace % "jackson-core"         % jacksonVersion
    lazy val annotations = namespace % "jackson-annotations"  % jacksonVersion
    lazy val databind    = namespace % "jackson-databind"     % jacksonVersion
    lazy val scalaModule = namespace % "jackson-module-scala" % jacksonVersion
  }

  object Jars {
    lazy val overrides: Seq[ModuleID] =
      Seq(
        jackson.annotations % Compile,
        jackson.core        % Compile,
        jackson.databind    % Compile,
        jackson.scalaModule % Compile
      )
    lazy val `server`: Seq[ModuleID]  = Seq(
      // For making Java 12 happy
      "javax.annotation"             % "javax.annotation-api" % "1.3.2" % "compile",
      //
      akka.actor                            % Compile,
      akka.actorTyped                       % Compile,
      akka.clusterTools                     % Compile,
      akka.http                             % Compile,
      akka.httpJson                         % Compile,
      akka.management                       % Compile,
      akka.managementLogLevels              % Compile,
      akka.serialization                    % Compile,
      akka.slf4j                            % Compile,
      akka.stream                           % Compile,
      bouncycastle.kix                      % Compile,
      bouncycastle.provider                 % Compile,
      cats.core                             % Compile,
      logback.classic                       % Compile,
      mustache.mustache                     % Compile,
      nimbus.joseJwt                        % Compile,
      pagopa.agreementManagementModels      % Compile,
      pagopa.commonsUtils                   % Compile,
      pagopa.catalogManagementModels        % Compile,
      pagopa.commonsJwt                     % Compile,
      pagopa.commonsCqrs                    % Compile,
      pagopa.authorizationManagementClient  % Compile,
      pagopa.authorizationManagementModels  % Compile,
      pagopa.authorizationManagementKeyConverter  % Compile,
      pagopa.selfcareV2Client               % Compile,
      pagopa.purposeManagementModels        % Compile,
      pagopa.tenantManagementModels         % Compile,
      scalpb.core                           % "protobuf",
      akka.httpTestkit                      % Test,
      akka.streamTestkit                    % Test,
      akka.testkit                          % Test,
      scalatest.core                        % Test,
      scalamock.core                        % Test
    )
    lazy val client: Seq[ModuleID]    =
      Seq(akka.stream, akka.http, akka.httpJson4s, akka.slf4j, json4s.jackson, json4s.ext, pagopa.commonsUtils).map(
        _ % Compile
      )
  }
}
