// ─── Enums ────────────────────────────────────────────────────────────────────

export type FileStatus = 'RECEIVED' | 'PENDING' | 'PROCESSING' | 'PROCESSED' | 'ERROR' | 'DELETED';
export type ProcessingMode = 'AUTO' | 'MANUAL' | 'SCHEDULED';
export type FileType = 'XML' | 'JSON' | 'CSV' | 'TXT' | 'EDIFACT' | 'X12' | 'EXCEL';
export type ErrorType =
  | 'MISSING_MANDATORY_FIELD'
  | 'TRANSFORMATION_FAILURE'
  | 'INVALID_DATA_FORMAT'
  | 'LOOKUP_VALUE_NOT_FOUND'
  | 'DUPLICATE_ORDER_ID'
  | 'SCHEMA_PARSE_ERROR'
  | 'DB_INSERT_FAILURE'
  | 'VALIDATION_RULE_FAIL'
  | 'TIMEOUT'
  | 'CHECKSUM_MISMATCH';

// ─── Auth ─────────────────────────────────────────────────────────────────────

export interface LoginRequest {
  username: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface AuthUser {
  username: string;
  roles: string[];
}

// ─── Files ────────────────────────────────────────────────────────────────────

export interface TmsFile {
  entryNo: number;
  fileName: string;
  fileType: FileType;
  partnerId: number;
  partnerCode: string;
  partnerName: string;
  status: FileStatus;
  processingMode: ProcessingMode;
  fileSize: number;
  checksum: string;
  storagePath: string;
  errorMessage: string | null;
  retryCount: number;
  orderCount: number | null;
  receivedTimestamp: string;
  processedTimestamp: string | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// ─── Schema Tree ──────────────────────────────────────────────────────────────

export interface SchemaNode {
  path: string;
  name: string;
  type: 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN' | 'OBJECT' | 'ARRAY';
  sampleValue?: string;
  isArray?: boolean;
  arrayCount?: number;
  children: SchemaNode[];
}

// ─── Mappings ─────────────────────────────────────────────────────────────────

export interface MappingLine {
  mappingLineId?: number;
  sourceFieldPath: string;
  targetField: string;
  transformationRule?: string;
  transformationParams?: Record<string, unknown>;
  defaultValue?: string;
  isRequired?: boolean;
  sequence: number;
  conditionRule?: string;
  lookupTableName?: string;
}

export interface Mapping {
  mappingId: number;
  partnerId: number;
  partnerCode: string;
  fileType: FileType;
  mappingName: string;
  version: number;
  activeFlag: boolean;
  description?: string;
  createdBy?: string;
  createdDate: string;
  updatedDate: string;
  lines: MappingLine[];
}

export interface AiSuggestion {
  targetField: string;
  sourcePath: string;
  confidence: number;
  reason: string;
  suggestedTransform?: string;
}

export interface AiSuggestionResponse {
  suggestions: AiSuggestion[];
  overallConfidence: number;
  unmappedRequired: string[];
}

// ─── Partners ─────────────────────────────────────────────────────────────────

export interface Partner {
  partnerId: number;
  partnerCode: string;
  partnerName: string;
  defaultFormat: FileType;
  processingMode: ProcessingMode;
  active: boolean;
  slaHours: number;
  contactEmail?: string;
  sftpConfig?: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

// ─── Errors ───────────────────────────────────────────────────────────────────

export interface EdiError {
  errorId: number;
  entryNo: number;
  fileName: string;
  mappingLineId?: number;
  errorType: ErrorType;
  errorCode?: string;
  errorMessage: string;
  fieldPath?: string;
  resolvedFlag: boolean;
  resolvedBy?: string;
  resolvedAt?: string;
  resolutionNote?: string;
  timestamp: string;
}

// ─── Monitoring ───────────────────────────────────────────────────────────────

export interface PartnerVolume {
  partnerCode: string;
  partnerName: string;
  fileCount: number;
  percentage: number;
}

export interface ErrorTypeCount {
  errorType: string;
  errorCode?: string;
  count: number;
  percentage: number;
}

export interface MonitoringStats {
  totalFilesReceived: number;
  totalFilesProcessed: number;
  totalFilesFailed: number;
  totalFilesPending: number;
  successRatePercent: number;
  avgProcessingSeconds: number;
  openErrors: number;
  partnerVolumes: PartnerVolume[];
  errorTypeCounts: ErrorTypeCount[];
  hourlyThroughput?: Record<string, number>;
}

// ─── Canonical / Staging ──────────────────────────────────────────────────────

export interface OrderHeader {
  id: number;
  entryNo: number;
  externalOrderId: string;
  customerCode: string;
  orderDate: string;
  requestedDeliveryDate?: string;
  originAddress?: string;
  destinationAddress?: string;
  incoterm?: string;
  priority: string;
  status: string;
  createdAt: string;
}

// ─── UI Helpers ───────────────────────────────────────────────────────────────

export interface SelectOption {
  value: string;
  label: string;
}

export interface SortConfig {
  field: string;
  dir: 'asc' | 'desc';
}
