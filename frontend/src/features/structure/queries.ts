import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type {
  Building,
  CommonElement,
  StructureSummary,
  Unit,
  UnitType,
} from "../../types/api";

export interface CreateBuildingPayload {
  name: string;
  address?: string;
  city?: string;
  postalCode?: string;
  floors?: number;
  notes?: string;
}

export interface CreateUnitPayload {
  buildingId?: string;
  code: string;
  type: UnitType;
  floor?: string;
  door?: string;
  builtArea?: number;
  coefficient: number | string;
}

export interface CreateCommonElementPayload {
  buildingId?: string;
  name: string;
  type?: string;
  description?: string;
}

function structureKey(communityId: string | undefined) {
  return ["structure", communityId] as const;
}

export function useBuildings(communityId: string | undefined) {
  return useQuery({
    queryKey: [...structureKey(communityId), "buildings"],
    queryFn: () => api.get<Building[]>(`/communities/${communityId}/buildings`),
    enabled: Boolean(communityId),
  });
}

export function useUnit(communityId: string | undefined, unitId: string | undefined) {
  return useQuery({
    queryKey: [...structureKey(communityId), "units", unitId],
    queryFn: () => api.get<Unit>(`/communities/${communityId}/units/${unitId}`),
    enabled: Boolean(communityId) && Boolean(unitId),
  });
}

export function useUnits(communityId: string | undefined) {
  return useQuery({
    queryKey: [...structureKey(communityId), "units"],
    queryFn: () => api.get<Unit[]>(`/communities/${communityId}/units`),
    enabled: Boolean(communityId),
  });
}

export function useCommonElements(communityId: string | undefined) {
  return useQuery({
    queryKey: [...structureKey(communityId), "common-elements"],
    queryFn: () => api.get<CommonElement[]>(`/communities/${communityId}/common-elements`),
    enabled: Boolean(communityId),
  });
}

export function useStructureSummary(communityId: string | undefined) {
  return useQuery({
    queryKey: [...structureKey(communityId), "summary"],
    queryFn: () => api.get<StructureSummary>(`/communities/${communityId}/structure/summary`),
    enabled: Boolean(communityId),
  });
}

function useInvalidateStructure(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: structureKey(communityId) });
}

export function useCreateBuilding(communityId: string | undefined) {
  const invalidate = useInvalidateStructure(communityId);
  return useMutation({
    mutationFn: (payload: CreateBuildingPayload) =>
      api.post<Building>(`/communities/${communityId}/buildings`, payload),
    onSuccess: invalidate,
  });
}

export function useCreateUnit(communityId: string | undefined) {
  const invalidate = useInvalidateStructure(communityId);
  return useMutation({
    mutationFn: (payload: CreateUnitPayload) =>
      api.post<Unit>(`/communities/${communityId}/units`, payload),
    onSuccess: invalidate,
  });
}

export function useCreateCommonElement(communityId: string | undefined) {
  const invalidate = useInvalidateStructure(communityId);
  return useMutation({
    mutationFn: (payload: CreateCommonElementPayload) =>
      api.post<CommonElement>(`/communities/${communityId}/common-elements`, payload),
    onSuccess: invalidate,
  });
}
