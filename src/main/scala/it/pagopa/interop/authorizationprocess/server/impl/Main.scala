package it.pagopa.interop.authorizationprocess.server.impl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.management.scaladsl.AkkaManagement
import buildinfo.BuildInfo
import cats.syntax.all._
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.authorizationprocess.common.ApplicationConfiguration
import it.pagopa.interop.authorizationprocess.server.Controller
import it.pagopa.interop.commons.logging.renderBuildInfo
import it.pagopa.interop.commons.utils.CORSSupport
import kamon.Kamon

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}
import akka.actor.typed.DispatcherSelector

object Main extends App with CORSSupport with Dependencies {

  Kamon.init()

  val logger: Logger = Logger(this.getClass)

  System.setProperty("kanela.show-banner", "false")

  val system: ActorSystem[Nothing] = ActorSystem[Nothing](
    Behaviors.setup[Nothing] { context =>
      implicit val actorSystem: ActorSystem[_]        = context.system
      implicit val executionContext: ExecutionContext = actorSystem.executionContext
      val selector: DispatcherSelector                = DispatcherSelector.fromConfig("futures-dispatcher")
      val blockingEc: ExecutionContextExecutor        = actorSystem.dispatchers.lookup(selector)

      AkkaManagement.get(actorSystem.classicSystem).start()

      logger.info(renderBuildInfo(BuildInfo))

      val serverBinding = for {
        jwtReader <- getJwtValidator()
        controller = new Controller(
          clientApi(jwtReader, blockingEc),
          healthApi,
          operatorApi(jwtReader, blockingEc),
          validationExceptionToRoute.some
        )(actorSystem.classicSystem)
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

      Behaviors.empty[Nothing]
    },
    BuildInfo.name
  )

  system.whenTerminated.onComplete { case _ => Kamon.stop() }(scala.concurrent.ExecutionContext.global)

}
