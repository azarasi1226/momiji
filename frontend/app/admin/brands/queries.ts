import { timestampDate } from "@bufbuild/protobuf/wkt";
import { FindBrandByIdService } from "@/grpc/gen/momiji/brand/findbyid/v1/findbyid_pb.js";
import { ListBrandsService } from "@/grpc/gen/momiji/brand/list/v1/list_pb.js";
import { BrandStatus } from "@/grpc/gen/momiji/brand/v1/status_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { redirectIfUnauthenticated } from "@/lib/grpc-error";
import { requireValidSession } from "@/lib/session";

export type Brand = {
  id: string;
  name: string;
  description: string;
  status: string;
  createdAt: string;
  updatedAt: string;
};

function brandStatusName(status: BrandStatus): string {
  switch (status) {
    case BrandStatus.ACTIVE:
      return "ACTIVE";
    case BrandStatus.ARCHIVED:
      return "ARCHIVED";
    default:
      return "UNKNOWN";
  }
}

export async function listBrands(): Promise<Brand[]> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(ListBrandsService, session.accessToken);
    const res = await client.listBrands({});
    return res.brands.map((b) => ({
      id: b.id,
      name: b.name,
      description: b.description,
      status: brandStatusName(b.status),
      createdAt: b.createdAt ? timestampDate(b.createdAt).toISOString() : "",
      updatedAt: b.updatedAt ? timestampDate(b.updatedAt).toISOString() : "",
    }));
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}

export async function fetchBrand(id: string): Promise<Brand> {
  const session = await requireValidSession();
  try {
    const client = createGrpcClient(FindBrandByIdService, session.accessToken);
    const res = await client.findBrandById({ id });
    return {
      id: res.id,
      name: res.name,
      description: res.description,
      status: brandStatusName(res.status),
      createdAt: res.createdAt
        ? timestampDate(res.createdAt).toISOString()
        : "",
      updatedAt: res.updatedAt
        ? timestampDate(res.updatedAt).toISOString()
        : "",
    };
  } catch (e) {
    redirectIfUnauthenticated(e);
    throw e;
  }
}

export async function listAllBrands(): Promise<{ id: string; name: string }[]> {
  const brands = await listBrands();
  return brands.map((b) => ({ id: b.id, name: b.name }));
}

export async function listActiveBrands(): Promise<
  { id: string; name: string }[]
> {
  const brands = await listBrands();
  return brands
    .filter((b) => b.status === "ACTIVE")
    .map((b) => ({ id: b.id, name: b.name }));
}
