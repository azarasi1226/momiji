import { createClient, type Client } from "@connectrpc/connect"
import { createGrpcTransport } from "@connectrpc/connect-node"
import type { DescService } from "@bufbuild/protobuf"

export const GRPC_URL = process.env.GRPC_URL ?? "http://localhost:9091"

export function createGrpcClient<T extends DescService>(
  service: T,
  accessToken: string,
): Client<T> {
  const transport = createGrpcTransport({
    baseUrl: GRPC_URL,
    interceptors: [
      (next) => async (req) => {
        req.header.set("authorization", `Bearer ${accessToken}`)
        return next(req)
      },
    ],
  })
  return createClient(service, transport)
}
