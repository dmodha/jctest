package test.model


import spray.json.DefaultJsonProtocol

case class Balance(address:String, balance:Double)
case class TransactionInfo(fromAddress:Option[String], toAddress:String, amount:String, timestamp:String)
case class Deposit(address:String, amount:Option[Double]=None)
case class Withdraw(address:String, amount:Option[Double]=None)
case class Transfer(fromAddress:String, toAddress:String, amount:String)
case class TransactionException(msg:String,cause:Throwable=null)extends Exception(msg,cause)
case class BalanceAndTransactions(balance:String, transactions:List[TransactionInfo])
case class Error(error:String)
case class JobCoinResponse(status:Option[String]=None, error:Option[String]=None)

object JobCoinModel extends DefaultJsonProtocol{
  implicit val BalanceFormat = jsonFormat2(Balance.apply)
  implicit val TransactionInfoFormat = jsonFormat4(TransactionInfo.apply)
  implicit val DepositFormat = jsonFormat2(Deposit.apply)
  implicit val TransferFormat = jsonFormat3(Transfer.apply)
  implicit val BalanceAndTransactionFormat = jsonFormat2(BalanceAndTransactions.apply)
  implicit val ErrorFormat = jsonFormat1(Error.apply)
  implicit val JobCoinResponseFormat = jsonFormat2(JobCoinResponse.apply)
}
