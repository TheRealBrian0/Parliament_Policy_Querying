const DEFAULT_URL = "/graphql";

export function getGraphqlUrl(): string {
  return import.meta.env.VITE_GRAPHQL_URL?.trim() || DEFAULT_URL;
}

export type GraphqlResponse<T> = { data?: T; errors?: { message: string }[] };

export async function graphqlRequest<T>(
  query: string,
  variables?: Record<string, unknown>
): Promise<T> {
  const res = await fetch(getGraphqlUrl(), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query, variables }),
  });
  if (!res.ok) {
    throw new Error(`GraphQL HTTP ${res.status}`);
  }
  const body = (await res.json()) as GraphqlResponse<T>;
  if (body.errors?.length) {
    throw new Error(body.errors.map((e) => e.message).join("; "));
  }
  if (body.data === undefined) {
    throw new Error("GraphQL response missing data");
  }
  return body.data;
}
