import { timestampDate } from "@bufbuild/protobuf/wkt";
import { FindUserByIdService } from "@/grpc/gen/momiji/user/findbyid/v1/findbyid_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

// プロフィールは email と name のみ（Amazon 式）。 住所・電話は配送先（shipping-addresses）が持つ。
export type UserProfile = {
  id: string;
  email: string;
  name: string;
  createdAt: string;
  updatedAt: string;
};

export async function fetchProfile(): Promise<UserProfile> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(FindUserByIdService, session.accessToken);
    const response = await client.findUserById({});
    return {
      id: response.id,
      email: response.email,
      name: response.name,
      createdAt: response.createdAt
        ? timestampDate(response.createdAt).toISOString()
        : "",
      updatedAt: response.updatedAt
        ? timestampDate(response.updatedAt).toISOString()
        : "",
    };
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}
