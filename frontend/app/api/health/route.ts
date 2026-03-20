import { NextResponse } from "next/server";

export async function GET() {
  return NextResponse.json({
    status: "UP",
    service: "market-data-lake-ui",
    timestamp: new Date().toISOString()
  });
}
