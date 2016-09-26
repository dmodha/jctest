package test.actor


import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.LazyLogging
import test.model._
import test.persistence.JCStore

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import spray.http._
import spray.json.DefaultJsonProtocol
import spray.httpx.SprayJsonSupport._
import spray.client.pipelining._
import test.model.JobCoinModel._


object Poller {
  def props(jCStore: JCStore, houseAddress: String) = Props(classOf[Poller], jCStore, houseAddress)
}

class Poller(jCStore: JCStore, houseAddress: String) extends Actor with LazyLogging {

  implicit val ec: ExecutionContext = context.system.dispatchers.lookup("akka.default-dispatcher")
  val getBalancePipeline: HttpRequest => Future[BalanceAndTransactions] = (
    //    addHeader("accept","application/json") ~>
    sendReceive
      ~> unmarshal[BalanceAndTransactions]
    )

  val transferPipeline: HttpRequest => Future[JobCoinResponse] = (
    //    addHeader("Content-Type","application/json") ~>
    sendReceive
      ~> unmarshal[JobCoinResponse]
    )

  def pollBalanceAndTransaction(tuple: (String, String)): Unit = {
    val g = getBalancePipeline(Get(s"http://jobcoin.projecticeland.net/ultrasubtle/api/addresses/${tuple._2}"))
    g onSuccess {
      case transaction => if (transaction.balance.toDouble > 0) {
        val t = transferPipeline(Post(s"http://jobcoin.projecticeland.net/ultrasubtle/api/transactions", Transfer(tuple._2, houseAddress, transaction.balance)))
        t onFailure {
          case m =>
            logger.error(s"unable to transfer to house: ${m}")
        }
        t onSuccess {
          case m =>
            val fee = jCStore.deposit(Deposit(tuple._2, Some(transaction.balance.toDouble)), true)
            logger.info(s"transferred to house ${m} from ${tuple._2} total fee = ${fee}")
        }
      }
    }
    g onFailure {
      case f =>
        logger.warn(s"$f")
    }
  }

  override def receive: Receive = {
    case "Tick" => {
      jCStore.getMappedAddresses.foreach(tuple => {
        pollBalanceAndTransaction(tuple)
        val balance = jCStore.balanceForAnAddress(tuple._2).balance
        if (balance > 0) {
          val random = ((10*Math.random()).toInt/10.0)
          val amountToTransfer = if( balance < 0.5) balance else (balance * random)
          logger.info(s"transferring ${amountToTransfer} from house to ${tuple._1}")
          val t = transferPipeline(Post(s"http://jobcoin.projecticeland.net/ultrasubtle/api/transactions", Transfer(houseAddress, tuple._1, amountToTransfer.toString)))
          t onFailure {
            case m => logger.error(s"unable to transfer from house: ${m}")
          }
          t onSuccess {
            case m =>
              logger.info(s"transferred from house ${m} to ${tuple._1}")
              jCStore.withdraw(Withdraw(tuple._2, Some(amountToTransfer)))
          }
        } else {
          logger.info("{} has zero balance", tuple._2)
        }
      }
      )
    }
  }

  override def preStart(): Unit = {
    super.preStart()
    this.context.system.scheduler.schedule(0 milliseconds, 60 seconds, self, "Tick")
  }
}
