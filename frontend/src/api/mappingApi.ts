import api from './axios';
import type { Mapping, MappingLine, AiSuggestionResponse } from '../types';

/** Normalize lines for PUT so Jackson always receives compatible types (boolean, string params). */
function wireMappingLine(l: MappingLine) {
  const p = l.transformationParams;
  let transformationParams: string | undefined;
  if (p == null) transformationParams = undefined;
  else if (typeof p === 'string') transformationParams = p;
  else transformationParams = JSON.stringify(p);

  return {
    mappingLineId: l.mappingLineId,
    sourceFieldPath: l.sourceFieldPath ?? '',
    targetField: l.targetField,
    transformationRule: l.transformationRule ?? 'DIRECT',
    transformationParams,
    defaultValue: l.defaultValue,
    isRequired: l.isRequired === true,
    sequence: typeof l.sequence === 'number' ? l.sequence : 0,
    conditionRule: l.conditionRule,
    lookupTableName: l.lookupTableName,
  };
}

export interface MappingVersionHistoryItem {
  id: number;
  mappingId: number;
  version: number;
  savedBy: string;
  savedAt: string;
  changeSummary: string;
  lineCount: number;
}

export const mappingApi = {
  list: (partnerId?: number) =>
    api.get<Mapping[]>('/mappings', { params: partnerId ? { partnerId } : {} }).then((r) => r.data),

  get: (mappingId: number) =>
    api.get<Mapping>(`/mappings/${mappingId}`).then((r) => r.data),

  create: (data: Partial<Mapping>) =>
    api
      .post<Mapping>('/mappings', {
        ...data,
        lines: data.lines?.map(wireMappingLine),
      })
      .then((r) => r.data),

  update: (mappingId: number, data: Partial<Mapping>) =>
    api
      .put<Mapping>(`/mappings/${mappingId}`, {
        ...data,
        lines: data.lines?.map(wireMappingLine),
      })
      .then((r) => r.data),

  activate: (mappingId: number) =>
    api.post(`/mappings/${mappingId}/activate`).then((r) => r.data),

  getVersionHistory: (mappingId: number) =>
    api.get<MappingVersionHistoryItem[]>(`/mappings/${mappingId}/versions`).then((r) => r.data),

  aiSuggest: (entryNo: number) =>
    api.post<AiSuggestionResponse>('/mappings/ai-suggest', { entryNo }).then((r) => r.data),

  testMapping: (mappingId: number, entryNo: number) =>
    api.post(`/mappings/${mappingId}/test`, { entryNo }).then((r) => r.data),
};
