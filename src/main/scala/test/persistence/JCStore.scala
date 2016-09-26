package test.persistence

import java.util
import java.util.{Date, UUID}
import java.util.concurrent.atomic.AtomicReference

import com.typesafe.scalalogging.LazyLogging
import test.fees.FeeCalculator
import test.model._

import scala.annotation.tailrec
import scala.collection.JavaConversions._

trait JCStore {
  def createMappedAddresss(address:String):String
  def getMappedAddresses:List[(String,String)]

  //returns fee
  def deposit(deposit: Deposit, subtractFee:Boolean = false): Double
  def withdraw(withdraw: Withdraw):Unit


  def balanceForAnAddress(address: String): Balance

  def balance(): List[Balance]

  def calculateFee(address:String,amount:Double):Double
}


class AtomicBalance {
  private val value = new AtomicReference(java.lang.Double.valueOf(0.0))

  @tailrec
  final def getAndAdd(delta: Double): Double = {
    val currentValue = value.get
    val newValue = java.lang.Double.valueOf(currentValue.doubleValue + delta)
    if (newValue < 0)
      throw TransactionException(s"insufficient funds: current balance is ${currentValue}")
    if (value.compareAndSet(currentValue, newValue))
      currentValue.doubleValue
    else
      getAndAdd(delta) // try, try again
  }

  def get: Double = value.get()

}

// Replace with real store
class JCStoreImpl(feeCalculator: FeeCalculator, houseAccount: String = UUID.randomUUID().toString) extends JCStore with AutoCloseable with LazyLogging {

  val balanceMap = new util.concurrent.ConcurrentHashMap[String, AtomicBalance]()
  val mappedAddresses = new util.concurrent.ConcurrentHashMap[String,String]()


  initHouse()

  def initHouse() {
    initUser(houseAccount)
  }

  override def createMappedAddresss(address: String): String = {
    mappedAddresses.putIfAbsent(address, UUID.randomUUID().toString)
    val a = mappedAddresses.get(address)
    initUser(a)
    a
  }

  override def getMappedAddresses: List[(String, String)] = {
    mappedAddresses.entrySet().map(e=>(e.getKey,e.getValue)).toList
  }
  override def deposit(deposit: Deposit, subtractFee:Boolean = false): Double = {
    initUser(deposit.address)
    val fee = this.calculateFee(deposit.address, deposit.amount.getOrElse(0))
    updateBalance(deposit.address, deposit.amount.getOrElse(0.0) - fee)
    fee
  }

  override def withdraw(withdraw: Withdraw):Unit = {
    if( withdraw.amount.getOrElse(0.0) < 0.0 )
      throw new RuntimeException("invalid value")
    updateBalance(withdraw.address, -1.0 * withdraw.amount.getOrElse(0.0) )
  }
  private def initUser(user: String): Unit = {
    balanceMap.putIfAbsent(user, new AtomicBalance)
  }

  private def updateBalance(address: String, delta: Double): Unit = {
    if (delta != 0) {
      balanceMap.get(address).getAndAdd(delta)
    }
  }

  override def balanceForAnAddress(address: String): Balance = {
    if (!balanceMap.containsKey(address)) {
      logger.warn(s"invalid address ${address}")
      return Balance(address, 0.0)
    }
    Balance(address, balanceMap.get(address).get)
  }

  override def balance(): List[Balance] = {
    balanceMap.entrySet().map(kv => Balance(kv.getKey, kv.getValue.get)).toList
  }

  override def calculateFee(address:String, amount: Double): Double = feeCalculator.fee(address,amount)

  def initializeMappedAdded(s: String, s1: String) = {
    mappedAddresses.put(s, s1)
    initUser(s1)
  }
  override def close(): Unit = {
    balanceMap.clear()
  }
}