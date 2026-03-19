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
};

export type DataProduct = {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  price: number;
  currency: string;
  accessType: "ONE_TIME" | "SUBSCRIPTION";
  billingInterval: "ONE_TIME" | "MONTHLY" | "YEARLY";
  batchDownloadLimitMb?: number | null;
  realtimeSubscriptionLimit?: number | null;
  maxRealtimePayloadKb?: number | null;
  active?: boolean;
};

export type MarketData = {
  id: number;
  symbol: string;
  price: number;
  volume: number;
  timestamp: string;
  marketDate: string;
  dataType: "TICK" | "NEWS" | "FUNDAMENTALS" | "CRYPTO" | "OTHER";
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
