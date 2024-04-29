package space.spacelift.amqp

import com.rabbitmq.client.ConnectionFactory
import org.apache.pekko.actor.{ActorRef, ActorSystem}
import org.apache.pekko.pattern.gracefulStop
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import org.apache.pekko.util.Timeout
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import space.spacelift.amqp.Amqp._
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

class ChannelSpec extends TestKit(ActorSystem("TestSystem")) with AnyWordSpecLike with BeforeAndAfter with ImplicitSender {
  implicit val timeout = Timeout(5 seconds)
  val connFactory = new ConnectionFactory()
  val uri = system.settings.config.getString("amqp-scala-client-test.rabbitmq.uri")
  connFactory.setUri(uri)
  var conn: ActorRef = _
  var channelOwner: ActorRef = _
  val random = new Random()

  def randomQueueName = "queue" + random.nextInt()

  def randomExchangeName = "exchange" + random.nextInt()

  def randomQueue = QueueParameters(name = randomQueueName, passive = false, exclusive = false)

  def randomKey = "key" + random.nextInt()

  before {
    println("before")
    conn = system.actorOf(ConnectionOwner.props(connFactory, 1 second))
    channelOwner = ConnectionOwner.createChildActor(conn, ChannelOwner.props())
    waitForConnection(system, conn, channelOwner).await(5, TimeUnit.SECONDS)
  }

  after {
    println("after")
    Await.result(gracefulStop(conn, 5 seconds), 6 seconds)
  }
}
