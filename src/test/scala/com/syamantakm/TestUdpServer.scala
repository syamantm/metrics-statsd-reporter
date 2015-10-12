package com.syamantakm

import java.nio.charset.Charset
import java.util.concurrent.Executors

import scala.collection.mutable.{Set => MutableSet}
import io.netty.channel.socket.DatagramPacket
import io.reactivex.netty.RxNetty
import io.reactivex.netty.channel.{ConnectionHandler, ObservableConnection}
import io.reactivex.netty.protocol.udp.server.UdpServer
import org.slf4j.LoggerFactory
import rx.Observable
import rx.functions.Func1

/**
 * @author syamantak.
 */
class TestUdpServer(port: Int = 8098, resultCapture: MutableSet[String]) {

  private val logger = LoggerFactory.getLogger(getClass)

  private val WELCOME_MSG = "Welcome to the broadcast world!"
  private val WELCOME_MSG_BYTES = WELCOME_MSG.getBytes(Charset.defaultCharset())

  def createServer(): UdpServer[DatagramPacket, DatagramPacket] = {
    val server = RxNetty.createUdpServer(port, new ConnectionHandler[DatagramPacket, DatagramPacket]() {
      override def handle(newConnection: ObservableConnection[DatagramPacket, DatagramPacket]): Observable[Void] = {
        newConnection.getInput().flatMap(new Func1[DatagramPacket, Observable[Void]]() {
          override def call(received: DatagramPacket): Observable[Void] = {
            val sender = received.sender()
            val req = received.content().toString(Charset.defaultCharset())
            logger.info(s"Received datagram. Sender: $sender, data: $req")
            resultCapture += req
            val data = newConnection.getChannel.alloc().buffer(WELCOME_MSG_BYTES.length)
            data.writeBytes(WELCOME_MSG_BYTES)
            newConnection.writeAndFlush(new DatagramPacket(data, sender))
          }
        })
      }
    })
    logger.info("UDP hello server started...")
    server
  }
}

object TestUdpServer {
  def apply(port: Int = 8098, resultCapture: MutableSet[String]) = {
    val server = new TestUdpServer(port, resultCapture).createServer()
    val service = Executors.newSingleThreadExecutor()
    service.submit(new Runnable {
      override def run(): Unit = server.startAndWait()
    })
    server
  }
}