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
import { useHasDelegation } from "../features/authorizations/queries";
import { useAuthStore } from "../features/auth/authStore";
import { useCommunityStore } from "../features/communities/communityStore";
import { useCreateIncident, useIncidents } from "../features/incidents/queries";
import { useUnits } from "../features/structure/queries";
import type { IncidentPriority, IncidentStatus, Role } from "../types/api";
import { formatDate } from "../utils/format";
import { normalizeLanguage } from "../utils/language";

const CREATE_ROLES: Role[] = [
  "COMMUNITY_ADMIN",
  "PRESIDENT",
  "SECRETARY",
  "TREASURER",
  "OWNER",
  "TENANT",
  "VECINO",
];

export function IncidentsPage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const membership = user?.memberships.find((item) => item.id === communityId);
  const roles = membership?.roles ?? [];
  const delegated = useHasDelegation(communityId, "INCIDENT");
  const canCreate = delegated || roles.some((role) => CREATE_ROLES.includes(role));
  const language = normalizeLanguage(i18n.language);
  const [status, setStatus] = useState<IncidentStatus | "">("");
  const [priority, setPriority] = useState<IncidentPriority | "">("");
  const [assignedToMe, setAssignedToMe] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const incidents = useIncidents(
    communityId,
    status || undefined,
    priority || undefined,
    assignedToMe,
  );
  const summary = useIncidents(communityId);
  const [search, setSearch] = useState("");
  const units = useUnits(communityId);

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  if (incidents.isLoading || summary.isLoading) {
    return <FullPageLoader />;
  }

  if (incidents.isError || summary.isError) {
    return (
      <div className="page">
        <Alert variant="error">{t("incidents.error")}</Alert>
      </div>
    );
  }

  const allIncidents = summary.data ?? [];
  const openCount = allIncidents.filter((incident) => incident.status === "OPEN").length;
  const progressCount = allIncidents.filter((incident) =>
    incident.status === "IN_PROGRESS" || incident.status === "WAITING_PROVIDER").length;
  const resolvedCount = allIncidents.filter((incident) =>
    incident.status === "RESOLVED" || incident.status === "CLOSED").length;
  const urgentCount = allIncidents.filter((incident) =>
    incident.priority === "URGENT" && incident.status !== "CLOSED" && incident.status !== "CANCELLED").length;
  const visibleIncidents = (incidents.data ?? []).filter((incident) => {
    const term = search.trim().toLowerCase();
    return !term
      || incident.title.toLowerCase().includes(term)
      || incident.category.toLowerCase().includes(term)
      || (incident.unitCode ?? "").toLowerCase().includes(term);
  });

  return (
    <div className="page incidents-page">
      <header className="page-header">
        <div>
          <h1>{t("incidents.title")}</h1>
          <p className="page-subtitle">{t("incidents.subtitle")}</p>
        </div>
        {canCreate ? (
          <Button onClick={() => setShowForm((value) => !value)}>
            {t("incidents.newIncident")}
          </Button>
        ) : null}
      </header>

      <div className="incident-kpis">
        <button type="button" className="incident-kpi incident-kpi--open" onClick={() => { setStatus("OPEN"); setPriority(""); }}>
          <span>{t("incidents.kpiOpen")}</span>
          <strong>{openCount}</strong>
        </button>
        <button type="button" className="incident-kpi incident-kpi--progress" onClick={() => { setStatus("IN_PROGRESS"); setPriority(""); }}>
          <span>{t("incidents.kpiInProgress")}</span>
          <strong>{progressCount}</strong>
        </button>
        <button type="button" className="incident-kpi incident-kpi--resolved" onClick={() => { setStatus("RESOLVED"); setPriority(""); }}>
          <span>{t("incidents.kpiResolved")}</span>
          <strong>{resolvedCount}</strong>
        </button>
        <button type="button" className="incident-kpi incident-kpi--urgent" onClick={() => { setStatus(""); setPriority("URGENT"); }}>
          <span>{t("incidents.kpiUrgent")}</span>
          <strong>{urgentCount}</strong>
        </button>
      </div>

      {showForm && communityId ? (
        <IncidentForm
          communityId={communityId}
          units={units.data ?? []}
          onDone={() => setShowForm(false)}
        />
      ) : null}

      <div className="filter-bar">
        <Input
          label={t("incidents.search")}
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder={t("incidents.searchPlaceholder")}
        />
        <Select label={t("incidents.status")} value={status} onChange={(e) => setStatus(e.target.value as IncidentStatus | "")}>
          <option value="">{t("incidents.all")}</option>
          {(["OPEN", "IN_PROGRESS", "WAITING_PROVIDER", "RESOLVED", "CLOSED", "CANCELLED"] as IncidentStatus[]).map((value) => (
            <option value={value} key={value}>{t(`incidents.statuses.${value}`)}</option>
          ))}
        </Select>
        <Select label={t("incidents.priority")} value={priority} onChange={(e) => setPriority(e.target.value as IncidentPriority | "")}>
          <option value="">{t("incidents.all")}</option>
          {(["LOW", "MEDIUM", "HIGH", "URGENT"] as IncidentPriority[]).map((value) => (
            <option value={value} key={value}>{t(`incidents.priorities.${value}`)}</option>
          ))}
        </Select>
        <label className="inline-check">
          <input type="checkbox" checked={assignedToMe} onChange={(e) => setAssignedToMe(e.target.checked)} />
          <span>{t("incidents.assignedToMe")}</span>
        </label>
      </div>

      {visibleIncidents.length === 0 ? (
        <div className="empty-state">
          <span>{t("incidents.noIncidents")}</span>
          {canCreate ? (
            <Button variant="secondary" onClick={() => setShowForm(true)}>{t("incidents.newIncident")}</Button>
          ) : null}
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>{t("incidents.incidentTitle")}</th>
                <th>{t("incidents.category")}</th>
                <th>{t("incidents.unit")}</th>
                <th>{t("incidents.priority")}</th>
                <th>{t("incidents.status")}</th>
                <th>{t("incidents.assignedTo")}</th>
                <th>{t("incidents.created")}</th>
              </tr>
            </thead>
            <tbody>
              {visibleIncidents.map((incident) => (
                <tr key={incident.id}>
                  <td data-label={t("incidents.incidentTitle")}>
                    <Link to={`/communities/${communityId}/incidents/${incident.id}`}>{incident.title}</Link>
                  </td>
                  <td data-label={t("incidents.category")}>{incident.category}</td>
                  <td data-label={t("incidents.unit")}>{incident.unitCode ?? t("incidents.noUnit")}</td>
                  <td data-label={t("incidents.priority")}>
                    <span className={`badge incident-priority incident-priority--${incident.priority.toLowerCase()}`}>
                      {t(`incidents.priorities.${incident.priority}`)}
                    </span>
                  </td>
                  <td data-label={t("incidents.status")}>
                    <span className={`badge incident-status incident-status--${incident.status.toLowerCase()}`}>
                      {t(`incidents.statuses.${incident.status}`)}
                    </span>
                  </td>
                  <td data-label={t("incidents.assignedTo")}>{incident.assignedToName ?? t("incidents.unassigned")}</td>
                  <td data-label={t("incidents.created")}>{formatDate(incident.createdAt, language)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function IncidentForm({
  communityId,
  units,
  onDone,
}: {
  communityId: string;
  units: { id: string; code: string }[];
  onDone: () => void;
}) {
  const { t } = useTranslation();
  const mutation = useCreateIncident(communityId);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("");
  const [priority, setPriority] = useState<IncidentPriority>("MEDIUM");
  const [unitId, setUnitId] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await mutation.mutateAsync({
        title,
        description,
        category,
        priority,
        unitId: unitId || undefined,
      });
      onDone();
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      <h3>{t("incidents.newIncident")}</h3>
      {mutation.isError ? <Alert variant="error">{t("incidents.error")}</Alert> : null}
      <div className="form-grid">
        <Input label={t("incidents.incidentTitle")} value={title} onChange={(e) => setTitle(e.target.value)} required />
        <Input label={t("incidents.category")} value={category} onChange={(e) => setCategory(e.target.value)} required />
        <Select label={t("incidents.priority")} value={priority} onChange={(e) => setPriority(e.target.value as IncidentPriority)}>
          {(["LOW", "MEDIUM", "HIGH", "URGENT"] as IncidentPriority[]).map((value) => (
            <option value={value} key={value}>{t(`incidents.priorities.${value}`)}</option>
          ))}
        </Select>
        <Select label={t("incidents.unit")} value={unitId} onChange={(e) => setUnitId(e.target.value)}>
          <option value="">{t("incidents.noUnit")}</option>
          {units.map((unit) => (
            <option value={unit.id} key={unit.id}>{unit.code}</option>
          ))}
        </Select>
        <Textarea
          label={t("incidents.description")}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          required
        />
      </div>
      <div className="form-actions">
        <Button type="submit" loading={mutation.isPending}>{t("incidents.save")}</Button>
      </div>
    </form>
  );
}
