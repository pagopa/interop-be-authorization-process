package it.pagopa.interop.authorizationprocess.server.impl

import com.typesafe.scalalogging.Logger
import akka.http.scaladsl.Http

import akka.management.scaladsl.AkkaManagement

import cats.syntax.all._
import it.pagopa.interop.commons.utils.{CORSSupport}
import it.pagopa.interop.authorizationprocess.common.ApplicationConfiguration
import it.pagopa.interop.authorizationprocess.server.Controller
import kamon.Kamon

import akka.actor.typed.ActorSystem
import scala.concurrent.ExecutionContext
import buildinfo.BuildInfo
import it.pagopa.interop.commons.logging.renderBuildInfo
import akka.actor.typed.scaladsl.Behaviors
import scala.util.{Success, Failure}

object Main extends App with CORSSupport with Dependencies {

  val logger: Logger = Logger(this.getClass)

  System.setProperty("kanela.show-banner", "false")

  val system = ActorSystem[Nothing](
    Behaviors.setup[Nothing] { context =>
      implicit val actorSystem: ActorSystem[_]        = context.system
      implicit val executionContext: ExecutionContext = actorSystem.executionContext

      Kamon.init()
      AkkaManagement.get(actorSystem.classicSystem).start()

      logger.info(renderBuildInfo(BuildInfo))

      val serverBinding = for {
        jwtReader <- getJwtValidator()
        controller = new Controller(clientApi(jwtReader), operatorApi(jwtReader), validationExceptionToRoute.some)(
          actorSystem.classicSystem
        )
        binding <- Http()(actorSystem.classicSystem)
          .newServerAt("0.0.0.0", ApplicationConfiguration.serverPort)
          .bind(corsHandler(controller.routes))
      } yield binding

      serverBinding.onComplete {
        case Success(b) =>
          logger.info(s"Started server at ${b.localAddress.getHostString()}:${b.localAddress.getPort()}")
        case Failure(e) =>
          actorSystem.terminate()
          logger.error("Startup error: ", e)
      }

      Behaviors.empty
    },
    BuildInfo.name
  )

  system.whenTerminated.onComplete { case _ => Kamon.stop() }(scala.concurrent.ExecutionContext.global)

}
