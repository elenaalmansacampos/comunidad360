import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Select } from "../components/Select";
import { Textarea } from "../components/Textarea";
import { useCommunityStore } from "../features/communities/communityStore";
import { StructureSummaryCard } from "../features/structure/StructureSummaryCard";
import {
  useBuildings,
  useCommonElements,
  useCreateBuilding,
  useCreateCommonElement,
  useCreateUnit,
  useStructureSummary,
  useUnits,
} from "../features/structure/queries";
import { ApiError } from "../services/apiClient";
import type { Building, Language, UnitType } from "../types/api";
import { formatPercent } from "../utils/format";

type ActiveForm = "building" | "unit" | "common" | null;

export function StructurePage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const buildings = useBuildings(communityId);
  const units = useUnits(communityId);
  const commonElements = useCommonElements(communityId);
  const summary = useStructureSummary(communityId);
  const [activeForm, setActiveForm] = useState<ActiveForm>(null);

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  const language: Language = i18n.language.startsWith("ca") ? "ca" : "es";
  const loading = buildings.isLoading || units.isLoading || commonElements.isLoading || summary.isLoading;
  const error = buildings.error ?? units.error ?? commonElements.error ?? summary.error;

  if (loading) {
    return <FullPageLoader />;
  }

  if (error) {
    const forbidden = error instanceof ApiError && error.status === 403;
    return (
      <div className="page">
        <Alert variant="error">
          {forbidden ? t("common.accessDenied") : t("common.unknownError")}
        </Alert>
        {!forbidden ? (
          <Button
            variant="secondary"
            onClick={() => {
              void buildings.refetch();
              void units.refetch();
              void commonElements.refetch();
              void summary.refetch();
            }}
          >
            {t("common.retry")}
          </Button>
        ) : null}
      </div>
    );
  }

  if (!communityId) {
    return <div className="empty-state">{t("errors.notFound")}</div>;
  }

  const buildingList = buildings.data ?? [];
  const unitList = units.data ?? [];
  const commonElementList = commonElements.data ?? [];

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>{t("structure.title")}</h1>
          <p className="page-subtitle">{t("structure.subtitle")}</p>
        </div>
        <div className="toolbar">
          <Button variant="secondary" onClick={() => setActiveForm("building")}>
            {t("structure.newBuilding")}
          </Button>
          <Button variant="secondary" onClick={() => setActiveForm("unit")}>
            {t("structure.newUnit")}
          </Button>
          <Button variant="secondary" onClick={() => setActiveForm("common")}>
            {t("structure.newCommonElement")}
          </Button>
        </div>
      </header>

      {summary.data ? <StructureSummaryCard summary={summary.data} /> : null}

      {activeForm === "building" ? (
        <BuildingForm communityId={communityId} onDone={() => setActiveForm(null)} />
      ) : null}
      {activeForm === "unit" ? (
        <UnitForm communityId={communityId} buildings={buildingList} onDone={() => setActiveForm(null)} />
      ) : null}
      {activeForm === "common" ? (
        <CommonElementForm
          communityId={communityId}
          buildings={buildingList}
          onDone={() => setActiveForm(null)}
        />
      ) : null}

      <section className="structure-section">
        <h2>{t("structure.buildings")}</h2>
        {buildingList.length === 0 ? (
          <div className="empty-state">{t("structure.noBuildings")}</div>
        ) : (
          <div className="list-grid">
            {buildingList.map((building) => (
              <article className="list-card" key={building.id}>
                <h3>{building.name}</h3>
                <p>
                  {[building.address, building.city, building.postalCode].filter(Boolean).join(" · ") ||
                    t("structure.none")}
                </p>
                {building.floors != null ? (
                  <span className="badge badge--muted">
                    {t("structure.fields.floors")}: {building.floors}
                  </span>
                ) : null}
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="structure-section">
        <h2>{t("structure.units")}</h2>
        {unitList.length === 0 ? (
          <div className="empty-state">{t("structure.noUnits")}</div>
        ) : (
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>{t("structure.fields.building")}</th>
                  <th>{t("structure.fields.code")}</th>
                  <th>{t("structure.fields.type")}</th>
                  <th>{t("structure.fields.coefficient")}</th>
                  <th>{t("structure.fields.status")}</th>
                </tr>
              </thead>
              <tbody>
                {unitList.map((unit) => (
                  <tr key={unit.id}>
                    <td data-label={t("structure.fields.building")}>{unit.buildingName ?? t("structure.none")}</td>
                    <td data-label={t("structure.fields.code")}>
                      <Link to={`/communities/${communityId}/units/${unit.id}`}>{unit.code}</Link>
                    </td>
                    <td data-label={t("structure.fields.type")}>
                      {t(`structure.unitType.${unit.type}`, { defaultValue: unit.type })}
                    </td>
                    <td data-label={t("structure.fields.coefficient")}>
                      {formatPercent(Number(unit.coefficient), language)}
                    </td>
                    <td data-label={t("structure.fields.status")}>
                      <span className="badge badge--muted">
                        {t(`structure.unitStatus.${unit.status}`, { defaultValue: unit.status })}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="structure-section">
        <h2>{t("structure.commonElements")}</h2>
        {commonElementList.length === 0 ? (
          <div className="empty-state">{t("structure.noCommonElements")}</div>
        ) : (
          <div className="list-grid">
            {commonElementList.map((element) => (
              <article className="list-card" key={element.id}>
                <h3>{element.name}</h3>
                <p>{element.description ?? element.type ?? t("structure.none")}</p>
                {element.buildingName ? (
                  <span className="badge badge--muted">{element.buildingName}</span>
                ) : null}
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function BuildingForm({ communityId, onDone }: { communityId: string; onDone: () => void }) {
  const { t } = useTranslation();
  const mutation = useCreateBuilding(communityId);
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [city, setCity] = useState("");
  const [postalCode, setPostalCode] = useState("");
  const [floors, setFloors] = useState("");
  const [notes, setNotes] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await mutation.mutateAsync({
        name,
        address: address || undefined,
        city: city || undefined,
        postalCode: postalCode || undefined,
        floors: floors ? Number(floors) : undefined,
        notes: notes || undefined,
      });
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h2>{t("structure.newBuilding")}</h2>
      {mutation.isError ? <Alert variant="error">{t("structure.error")}</Alert> : null}
      <div className="form-grid">
        <Input label={t("structure.fields.name")} value={name} onChange={(e) => setName(e.target.value)} required />
        <Input label={t("structure.fields.address")} value={address} onChange={(e) => setAddress(e.target.value)} />
        <Input label={t("structure.fields.city")} value={city} onChange={(e) => setCity(e.target.value)} />
        <Input
          label={t("structure.fields.postalCode")}
          value={postalCode}
          onChange={(e) => setPostalCode(e.target.value)}
        />
        <Input
          label={t("structure.fields.floors")}
          type="number"
          min="0"
          value={floors}
          onChange={(e) => setFloors(e.target.value)}
        />
        <Textarea label={t("structure.fields.notes")} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </div>
      <FormActions onCancel={onDone} loading={mutation.isPending} />
    </form>
  );
}

function UnitForm({
  communityId,
  buildings,
  onDone,
}: {
  communityId: string;
  buildings: Building[];
  onDone: () => void;
}) {
  const { t } = useTranslation();
  const mutation = useCreateUnit(communityId);
  const [buildingId, setBuildingId] = useState("");
  const [code, setCode] = useState("");
  const [type, setType] = useState<UnitType>("VIVIENDA");
  const [floor, setFloor] = useState("");
  const [door, setDoor] = useState("");
  const [builtArea, setBuiltArea] = useState("");
  const [coefficient, setCoefficient] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await mutation.mutateAsync({
        buildingId: buildingId || undefined,
        code,
        type,
        floor: floor || undefined,
        door: door || undefined,
        builtArea: builtArea ? Number(builtArea) : undefined,
        coefficient,
      });
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  const errorMessage =
    mutation.error instanceof ApiError && mutation.error.code === "UNIT_CODE_EXISTS"
      ? t("structure.codeExists")
      : t("structure.error");

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h2>{t("structure.newUnit")}</h2>
      {mutation.isError ? <Alert variant="error">{errorMessage}</Alert> : null}
      <div className="form-grid">
        <Select
          label={t("structure.fields.building")}
          value={buildingId}
          onChange={(e) => setBuildingId(e.target.value)}
        >
          <option value="">{t("structure.none")}</option>
          {buildings.map((building) => (
            <option value={building.id} key={building.id}>
              {building.name}
            </option>
          ))}
        </Select>
        <Input label={t("structure.fields.code")} value={code} onChange={(e) => setCode(e.target.value)} required />
        <Select
          label={t("structure.fields.type")}
          value={type}
          onChange={(e) => setType(e.target.value as UnitType)}
        >
          {(["VIVIENDA", "LOCAL", "GARAJE", "TRASTERO", "ELEMENTO_COMUN"] as UnitType[]).map((unitType) => (
            <option value={unitType} key={unitType}>
              {t(`structure.unitType.${unitType}`)}
            </option>
          ))}
        </Select>
        <Input label={t("structure.fields.floor")} value={floor} onChange={(e) => setFloor(e.target.value)} />
        <Input label={t("structure.fields.door")} value={door} onChange={(e) => setDoor(e.target.value)} />
        <Input
          label={t("structure.fields.builtArea")}
          type="number"
          min="0"
          step="0.01"
          value={builtArea}
          onChange={(e) => setBuiltArea(e.target.value)}
        />
        <Input
          label={t("structure.fields.coefficient")}
          type="number"
          min="0"
          max="100"
          step="0.000001"
          value={coefficient}
          onChange={(e) => setCoefficient(e.target.value)}
          required
        />
      </div>
      <FormActions onCancel={onDone} loading={mutation.isPending} />
    </form>
  );
}

function CommonElementForm({
  communityId,
  buildings,
  onDone,
}: {
  communityId: string;
  buildings: Building[];
  onDone: () => void;
}) {
  const { t } = useTranslation();
  const mutation = useCreateCommonElement(communityId);
  const [buildingId, setBuildingId] = useState("");
  const [name, setName] = useState("");
  const [type, setType] = useState("");
  const [description, setDescription] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await mutation.mutateAsync({
        buildingId: buildingId || undefined,
        name,
        type: type || undefined,
        description: description || undefined,
      });
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h2>{t("structure.newCommonElement")}</h2>
      {mutation.isError ? <Alert variant="error">{t("structure.error")}</Alert> : null}
      <div className="form-grid">
        <Select
          label={t("structure.fields.building")}
          value={buildingId}
          onChange={(e) => setBuildingId(e.target.value)}
        >
          <option value="">{t("structure.none")}</option>
          {buildings.map((building) => (
            <option value={building.id} key={building.id}>
              {building.name}
            </option>
          ))}
        </Select>
        <Input label={t("structure.fields.name")} value={name} onChange={(e) => setName(e.target.value)} required />
        <Input label={t("structure.fields.type")} value={type} onChange={(e) => setType(e.target.value)} />
        <Textarea
          label={t("structure.fields.description")}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>
      <FormActions onCancel={onDone} loading={mutation.isPending} />
    </form>
  );
}

function FormActions({ onCancel, loading }: { onCancel: () => void; loading: boolean }) {
  const { t } = useTranslation();
  return (
    <div className="form-actions">
      <Button type="button" variant="secondary" onClick={onCancel}>
        {t("common.cancel")}
      </Button>
      <Button type="submit" loading={loading}>
        {t("common.save")}
      </Button>
    </div>
  );
}
