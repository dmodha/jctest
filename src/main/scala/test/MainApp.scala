package test

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import test.actor.Poller
import test.fees.FeeCalculator
import test.persistence.{JCStore, JCStoreImpl}
import test.ws.RestInterface

import scala.concurrent.duration._

object MainApp extends App{
  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  implicit val system = ActorSystem("job-coin-service")

  val feeCalculator = new FeeCalculator(){
    override def fee(from: String, amount: Double): Double = amount * 0.0001
  }

  val houseAddress = if( config.hasPath("test.houseAddress")) config.getString("config.houseAddress") else "test_house_address"

  val jcStore = new JCStoreImpl(feeCalculator,houseAddress)

  val poller = system.actorOf(Poller.props(jcStore,houseAddress), "poller")
  val api = system.actorOf(RestInterface.props(jcStore, port ), "httpInterface")


  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)

  IO(Http).ask(Http.Bind(listener = api, interface = host, port = port))
    .mapTo[Http.Event]
    .map {
      case Http.Bound(address) =>
        println(s"REST interface bound to $address")
      case Http.CommandFailed(cmd) =>
        println("REST interface could not bind to " +
          s"$host:$port, ${cmd.failureMessage}")
        system.terminate()
    }
//  jcStore.initializeMappedAdded("abcd","39df172d-c49f-4309-b7ea-e019eb5916c4")
}
