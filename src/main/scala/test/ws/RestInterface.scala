package test.ws

import java.net.InetAddress

import akka.actor.{Actor, ActorLogging}
import akka.actor._
import akka.util.Timeout
import com.fasterxml.jackson.databind.ObjectMapper
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing._
import test.model.{BalanceAndTransactions, Error, TransactionException, Transfer}
import test.model.JobCoinModel._
import test.persistence.JCStore

import scala.concurrent.duration._
import spray.routing.{HttpService, HttpServiceActor}

object RestInterface {
  def props(jCStore: JCStore, port:Int) = Props(classOf[RestInterface], jCStore, port)

  val (ip, hostname) = gethostInfo()

  private def gethostInfo() = {
    try {
      val ip = InetAddress.getLocalHost();
      val hostname = ip.getHostName();
      (ip.getHostAddress, hostname)
    } catch {
      case ex: Exception => ex.printStackTrace()
        ("127.0.0.1","localhost")
    }
  }
}

class RestInterface(jcStore:JCStore, port:Int) extends HttpServiceActor
    with RestApi {
  def getHostPrefix:String = s"http://${RestInterface.ip}:${port}"

  val exceptionHandler = ExceptionHandler {
    case ex: TransactionException => complete(StatusCodes.BadRequest, Error(ex.msg))
    case ex: Exception => complete(StatusCodes.InternalServerError, Error(ex.getMessage))
  }
  val totallyMissingHandler = RejectionHandler {
    case Nil /* secret code for path not found */ =>
     complete(StatusCodes.NotFound, "page not found")
  }
  val objectMapper = new ObjectMapper()
  val completedUsage = objectMapper.writeValueAsString(usage)
  def receive = runRoute(
    handleExceptions(exceptionHandler) {
      handleRejections(totallyMissingHandler) {
        pathSingleSlash {
          redirect("/usage", StatusCodes.TemporaryRedirect)
        } ~
        path("usage") {
          complete(completedUsage)
        } ~
          routes(jcStore)
      }
    }
  )
}

trait RestApi extends HttpService with ActorLogging { actor: Actor =>
  implicit val timeout = Timeout(10 seconds)
  def getHostPrefix:String
  val usage = Array(
   Array(s"${getHostPrefix}/jobCoinMixer/api/map/{ORIGINAL_ADDRESS}", "GET", "returns mapped addresses") ,
   Array(s"${getHostPrefix}/jobCoinMixer/api/map/", "GET", "returns mapped addresses") ,
   Array(s"${getHostPrefix}/jobCoinMixer/api/balance/{ADDRESS}", "GET", "returns balance"),
   Array(s"${getHostPrefix}/jobCoinMixer/api/map/{ORIGINAL_ADDRESS}", "PUT", "returns newly created mapped addresses")
  )
  def routes(store:JCStore):Route = pathPrefix("jobCoinMixer"/"api") {
    get {
      path("map" / Segment) {
        address => {
          complete {
            store.getMappedAddresses.filter(t=>{t._1 == address || t._2 == address})
          }
        }
      } ~
      path("map") {
        complete{
          store.getMappedAddresses
        }
      } ~ path("balance"/Segment) {
        address => {
          complete {
            store.balanceForAnAddress(address)
          }
        }
      }
    } ~
    put {
      path("map"/Segment) {
        address => {
          complete {
            store.createMappedAddresss(address)
          }
        }
      }
    }
  }
}
