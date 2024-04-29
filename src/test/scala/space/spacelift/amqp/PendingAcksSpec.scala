package space.spacelift.amqp

import org.apache.pekko.actor.{Actor, ActorLogging, PoisonPill, Props}
import org.apache.pekko.testkit.TestProbe
import org.scalatest.wordspec.AnyWordSpecLike
import space.spacelift.amqp.Amqp._
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

object PendingAcksSpec {
  case object GetCounter
  case class Counter(value: Int)
  class BadListener(probe: TestProbe) extends Actor with ActorLogging {
    var counter = 0
    override def receive = {
      case Delivery(consumerTag, envelope, properties, body) => {
        log.info(s"received ${new String(body, "UTF-8")} tag = ${envelope.getDeliveryTag} redeliver = ${envelope.isRedeliver}")
        counter = counter + 1
        if (counter == 10) probe.ref ! 'done
      }
      case GetCounter => probe.ref ! Counter(counter)
    }
  }

  class GoodListener(probe: TestProbe) extends Actor with ActorLogging {
    var counter = 0
    override def receive = {
      case Delivery(consumerTag, envelope, properties, body) => {
        log.info(s"received ${new String(body, "UTF-8")} tag = ${envelope.getDeliveryTag} redeliver = ${envelope.isRedeliver}")
        counter = counter + 1
        sender ! Ack(envelope.getDeliveryTag)
        if (counter == 10) probe.ref ! 'done
      }
      case GetCounter => probe.ref ! Counter(counter)
    }
  }
}

class PendingAcksSpec extends ChannelSpec with AnyWordSpecLike {
  "consumers" should {
    "receive messages that were delivered to another consumer that crashed before it acked them" in {
      val exchange = ExchangeParameters(name = "amq.direct", exchangeType = "", passive = true)
      val queue = randomQueue
      val routingKey = randomKey

      ignoreMsg {
        case Amqp.Ok(p:Publish, _) => true
      }

      val probe = TestProbe()

      // create a consumer that does not ack messages
      val badListener = system.actorOf(Props(classOf[PendingAcksSpec.BadListener], probe))
      
      val consumer = ConnectionOwner.createChildActor(conn, Consumer.props(badListener, autoack = false, channelParams = None), name = Some("badConsumer"))
      val producer = ConnectionOwner.createChildActor(conn, ChannelOwner.props())
      Amqp.waitForConnection(system, consumer, producer).await(1, TimeUnit.SECONDS)

      consumer ! AddBinding(Binding(exchange, queue, routingKey))
      val Amqp.Ok(AddBinding(Binding(_, _, _)), _) = receiveOne(1 second)

      val message = "yo!".getBytes

      for (i <- 1 to 10) producer ! Publish(exchange.name, routingKey, message)

      probe.expectMsg(1 second, 'done)
      badListener ! PendingAcksSpec.GetCounter
      val counter = probe.expectMsgType[PendingAcksSpec.Counter].value
      assert(counter == 10)

      // now we should see 10 pending acks in the broker

      // create a consumer that does ack messages
      val goodListener = system.actorOf(Props(classOf[PendingAcksSpec.GoodListener], probe))
      val consumer1 = ConnectionOwner.createChildActor(conn, Consumer.props(goodListener, autoack = false, channelParams = None), name = Some("goodConsumer"))
      Amqp.waitForConnection(system, consumer1).await(1, TimeUnit.SECONDS)


      consumer1 ! AddBinding(Binding(exchange, queue, routingKey))
      val Amqp.Ok(AddBinding(Binding(_, _, _)), _) = receiveOne(1 second)

      // kill first consumer
      consumer ! PoisonPill

      probe.expectMsg(1 second, 'done)
      goodListener ! PendingAcksSpec.GetCounter
      val counter1 = probe.expectMsgType[PendingAcksSpec.Counter].value
      assert(counter1 == 10)
    }
  }
}
