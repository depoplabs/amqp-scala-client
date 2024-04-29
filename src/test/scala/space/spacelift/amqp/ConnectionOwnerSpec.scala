package space.spacelift.amqp

import Amqp._
import ConnectionOwner.{Connected, CreateChannel, Disconnected}
import com.rabbitmq.client.{ConnectionFactory, Address, Channel}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.gracefulStop
import org.apache.pekko.testkit.{ImplicitSender, TestKit, TestProbe}
import org.apache.pekko.util.Timeout
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike
import concurrent.duration._
import concurrent.Await
import java.util.concurrent.TimeUnit

class ConnectionOwnerSpec extends TestKit(ActorSystem("TestSystem")) with AnyWordSpecLike with ImplicitSender {
  implicit val timeout = Timeout(5 seconds)

  "ConnectionOwner" should {
    "provide channels for many child actors" in {
      val connFactory = new ConnectionFactory()
      val uri = system.settings.config.getString("amqp-scala-client-test.rabbitmq.uri")
      connFactory.setUri(uri)
      val conn = system.actorOf(ConnectionOwner.props(connFactory))
      Amqp.waitForConnection(system, conn).await(2, TimeUnit.SECONDS)
      val actors = 100
      for (i <- 0 until actors) {
        val p = TestProbe()
        p.send(conn, CreateChannel)
        p.expectMsgClass(2.second, classOf[Channel])
      }
      Await.result(gracefulStop(conn, 5 seconds), 6 seconds)
    }
    "connect even if the default host is unavailable" in {
      val connFactory = new ConnectionFactory()
      val uri = system.settings.config.getString("amqp-scala-client-test.rabbitmq.uri")
      connFactory.setUri(uri)
      val goodHost = connFactory.getHost
      connFactory.setHost("fake-host")
      val conn = system.actorOf(ConnectionOwner.props(connFactory, addresses = Some(Array(
          new Address("another.fake.host"),
          new Address(goodHost)
        ))))
      Amqp.waitForConnection(system, conn).await(50, TimeUnit.SECONDS)
      val actors = 100
      for (i <- 0 until actors) {
        val p = TestProbe()
        p.send(conn, CreateChannel)
        p.expectMsgClass(2.second, classOf[Channel])
      }
      Await.result(gracefulStop(conn, 5 seconds), 6 seconds)
    }
    "send Connected/Disconnected status messages" in {
      val connFactory = new ConnectionFactory()
      val uri = system.settings.config.getString("amqp-scala-client-test.rabbitmq.uri")
      connFactory.setUri(uri)
      val probe = TestProbe()
      val conn = system.actorOf(ConnectionOwner.props(connFactory))
      conn ! AddStatusListener(probe.ref)
      probe.expectMsg(2 seconds, Connected)
      conn ! Abort()
      probe.expectMsg(2 seconds, Disconnected)
    }
  }
}
