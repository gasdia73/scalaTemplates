package org.guz.auth

import cats.effect.*
import org.guz.protos.orders.*
import io.grpc.*
import fs2.Stream
import fs2.grpc.syntax.all.*
import io.grpc.netty.shaded.io.grpc.netty.*
import scala.util.chaining

class OrderService extends OrderFs2Grpc[IO, Metadata] {

  override def sendOrderStream(
      request: Stream[IO, OrderRequest],
      ctx: Metadata
  ): Stream[IO, OrderReply] =
    request.map { orderReq =>
      OrderReply(
        orderReq.orderId,
        orderReq.items,
        orderReq.items.map(_.amount).sum
      )

    }

}

object OrderServer extends IOApp.Simple {

  val grpcServerResource = OrderFs2Grpc
    .bindServiceResource(new OrderService)
    .flatMap { service =>
      NettyServerBuilder
        .forPort(9999)
        .addService(service)
        .resource[IO]
    }

  override def run = grpcServerResource.use { service =>
    IO(println("Order microservice started!")) *> 
    IO(service.start()) *>
    IO.never
  }

}

object Client {

  val resource = NettyChannelBuilder
    .forAddress("localhost", 9999)
    .usePlaintext()
    .resource[IO]
    .flatMap(channel => OrderFs2Grpc.stubResource(channel))

}

object GrpcApp extends IOApp.Simple {

  override def run = Client.resource.use { orderService =>
    val rpcCall = orderService.sendOrderStream(
      Stream.eval(
        IO(
          OrderRequest(
            0,
            List(Item("poco", 3, 2000.0))
          )
        )
      ), Metadata()
    ).compile.toList.flatMap(replies => IO.println(replies))

    rpcCall
  }

}
