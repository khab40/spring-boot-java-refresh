export type UserRole = "USER" | "ADMIN";
export type AuthProvider = "LOCAL" | "GOOGLE";

export type UserProfile = {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  company?: string | null;
  country?: string | null;
  phoneNumber?: string | null;
  role: UserRole;
  authProvider: AuthProvider;
  emailVerified: boolean;
  createdAt?: string;
  emailVerifiedAt?: string | null;
};

export type AuthResponse = {
  userId: number;
  email: string;
  accessToken?: string | null;
  tokenType?: string | null;
  apiKey?: string | null;
  emailVerified: boolean;
  message: string;
  profile?: UserProfile | null;
};

export type DataProduct = {
  id: number;
  catalogItemId?: number | null;
  code: string;
  name: string;
  description?: string | null;
  price: number;
  minimumPrice?: number | null;
  includedSymbols?: number | null;
  includedDays?: number | null;
  pricePerAdditionalSymbol?: number | null;
  pricePerAdditionalDay?: number | null;
  fullUniverseSymbolCount?: number | null;
  currency: string;
  accessType: "ONE_TIME_PURCHASE" | "SUBSCRIPTION";
  billingInterval: "ONE_TIME" | "MONTHLY" | "YEARLY";
  batchDownloadLimitMb?: number | null;
  realtimeSubscriptionLimit?: number | null;
  maxRealtimePayloadKb?: number | null;
  quotedPrice?: number | null;
  quotedSymbolCount?: number | null;
  quotedDayCount?: number | null;
  quotedStartDate?: string | null;
  quotedEndDate?: string | null;
  quotedSelectionSummary?: string | null;
  quotedPricingSummary?: string | null;
  active?: boolean;
};

export type CatalogItem = {
  id: number;
  code: string;
  name: string;
  summary?: string | null;
  description?: string | null;
  marketDataType: "QUOTE" | "TICK" | "NEWS" | "FUNDAMENTALS" | "CRYPTO" | "OTHER";
  storageSystem: "DELTA_LAKE" | "ICEBERG" | "STUB" | "OTHER";
  deliveryApiPath?: string | null;
  lakeQueryReference?: string | null;
  sampleSymbols?: string | null;
  coverageStartDate?: string | null;
  coverageEndDate?: string | null;
  selectionSummary?: string | null;
  active?: boolean;
  createdAt?: string;
  offers: DataProduct[];
};

export type CatalogFilters = {
  symbol: string;
  availableFrom: string;
  availableTo: string;
  marketDataType: "" | CatalogItem["marketDataType"];
  storageSystem: "" | CatalogItem["storageSystem"];
  accessType: "" | DataProduct["accessType"];
  billingInterval: "" | DataProduct["billingInterval"];
};

export type MarketData = {
  id: number;
  symbol: string;
  price: number;
  volume: number;
  timestamp: string;
  marketDate: string;
  dataType: "QUOTE" | "TICK" | "NEWS" | "FUNDAMENTALS" | "CRYPTO" | "OTHER";
};

export type MarketDataRuntimeStatus = {
  mode: string;
  stubbed: boolean;
  message: string;
};

export type Entitlement = {
  id: number;
  accessType: string;
  status: string;
  grantedAt: string;
  expiresAt?: string | null;
  purchasedUnits: number;
  batchDownloadUsedMb: number;
  realtimeSubscriptionsUsed: number;
  payloadKilobytesUsed: number;
  product: DataProduct;
};

export type PaymentTransaction = {
  id: number;
  amount: number;
  currency: string;
  status: string;
  checkoutUrl?: string | null;
  errorMessage?: string | null;
  createdAt?: string;
  updatedAt?: string;
  product: DataProduct;
  items?: {
    id?: number;
    quantity: number;
    unitPrice: number;
    lineAmount: number;
    currency: string;
    product: DataProduct;
  }[];
};

export type OtdDeliveryFile = {
  fileName: string;
  objectKey: string;
  signedUrl: string;
  sizeBytes: number;
  linkExpiresAt: string;
};

export type OtdDelivery = {
  deliveryId: number;
  productId: number;
  productCode: string;
  productName: string;
  status: string;
  sqlText: string;
  rowCount: number;
  fileCount: number;
  totalBytes: number;
  consumedMegabytes: number;
  remainingBatchMegabytes?: number | null;
  createdAt: string;
  files: OtdDeliveryFile[];
};

export type UsageSummary = {
  userId: number;
  productId: number;
  batchDownloadUsedMb: number;
  remainingBatchMb?: number | null;
  realtimeSubscriptionsUsed: number;
  remainingRealtimeSubscriptions?: number | null;
  payloadKilobytesUsed: number;
  remainingPayloadKb?: number | null;
};

export type AdminDashboard = {
  totalUsers: number;
  totalProducts: number;
  totalPayments: number;
  totalApiKeys: number;
  totalUsageRecords: number;
  activeEntitlements: number;
  recentUsers: UserProfile[];
  recentPayments: {
    id: number;
    userEmail: string;
    productCode: string;
    amount: number;
    currency: string;
    status: string;
    createdAt: string;
  }[];
  recentUsage: {
    id: number;
    userEmail: string;
    productCode: string;
    usageType: string;
    megabytesUsed: number;
    payloadKilobytesUsed: number;
    realtimeSubscriptionsUsed: number;
    requestCount: number;
    occurredAt: string;
  }[];
  recentEntitlements: {
    id: number;
    userEmail: string;
    productCode: string;
    accessType: string;
    status: string;
    grantedAt: string;
    expiresAt?: string | null;
  }[];
};

export type SessionState = {
  accessToken: string;
  apiKey: string;
  userId: number;
  email: string;
  profile?: UserProfile | null;
};

export type UpdateUserProfilePayload = {
  firstName: string;
  lastName: string;
  company?: string | null;
  country?: string | null;
  phoneNumber?: string | null;
};
