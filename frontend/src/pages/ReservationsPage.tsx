import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { CalendarCheck, Check, Plus, X } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Select } from "../components/Select";
import { Textarea } from "../components/Textarea";
import { useAuthStore } from "../features/auth/authStore";
import { useCommunityStore } from "../features/communities/communityStore";
import {
  canCreateReservations,
  canManageAreas,
  canManageReservations,
} from "../features/mvp/utils";
import {
  useCancelReservation,
  useCommonAreas,
  useCreateCommonArea,
  useCreateReservation,
  useReservations,
  useReviewReservation,
  useUpdateCommonArea,
} from "../features/reservations/queries";
import type { CommonArea, Reservation, ReservationStatus } from "../types/api";
import { formatDate } from "../utils/format";
import { normalizeLanguage } from "../utils/language";

function timeLabel(value: string): string {
  return value.slice(0, 5);
}

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function ReservationsPage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const roles = user?.memberships.find((item) => item.id === communityId)?.roles ?? [];
  const canCreate = canCreateReservations(roles);
  const canManage = canManageReservations(roles);
  const canManageZones = canManageAreas(roles);
  const language = normalizeLanguage(i18n.language);
  const areas = useCommonAreas(communityId);
  const reservations = useReservations(communityId);
  const createReservation = useCreateReservation(communityId);
  const createArea = useCreateCommonArea(communityId);
  const updateArea = useUpdateCommonArea(communityId);
  const review = useReviewReservation(communityId);
  const cancel = useCancelReservation(communityId);
  const [tab, setTab] = useState<"UPCOMING" | "MINE" | "ALL">("UPCOMING");
  const [showBooking, setShowBooking] = useState(false);
  const [showAreaForm, setShowAreaForm] = useState(false);
  const [areaId, setAreaId] = useState("");
  const [date, setDate] = useState(today);
  const [startTime, setStartTime] = useState("10:00");
  const [endTime, setEndTime] = useState("12:00");
  const [notes, setNotes] = useState("");
  const [areaName, setAreaName] = useState("");
  const [areaDescription, setAreaDescription] = useState("");
  const [areaLocation, setAreaLocation] = useState("");
  const [areaCapacity, setAreaCapacity] = useState("");
  const [areaApproval, setAreaApproval] = useState(true);

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  const areaList = areas.data ?? [];
  const reservationList = reservations.data ?? [];
  const activeAreas = areaList.filter((area) => area.active);
  const todayValue = today();
  const upcoming = reservationList.filter((reservation) =>
    reservation.date >= todayValue
      && (reservation.status === "PENDING" || reservation.status === "APPROVED"));
  const mine = reservationList.filter((reservation) => reservation.mine);
  const filtered = useMemo(() => {
    if (tab === "MINE") return mine;
    if (tab === "ALL") return reservationList;
    return upcoming;
  }, [tab, mine, reservationList, upcoming]);
  const pending = reservationList.filter((reservation) => reservation.status === "PENDING");

  async function submitBooking(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await createReservation.mutateAsync({
        areaId,
        date,
        startTime: `${startTime}:00`,
        endTime: `${endTime}:00`,
        notes: notes || undefined,
      });
      setNotes("");
      setShowBooking(false);
    } catch {
      // error mostrado en el aviso
    }
  }

  async function submitArea(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await createArea.mutateAsync({
        name: areaName,
        description: areaDescription || undefined,
        location: areaLocation || undefined,
        capacity: areaCapacity ? Number(areaCapacity) : undefined,
        requiresApproval: areaApproval,
      });
      setAreaName("");
      setAreaDescription("");
      setAreaLocation("");
      setAreaCapacity("");
      setAreaApproval(true);
      setShowAreaForm(false);
    } catch {
      // error mostrado en el aviso
    }
  }

  async function toggleArea(area: CommonArea) {
    try {
      await updateArea.mutateAsync({
        areaId: area.id,
        name: area.name,
        description: area.description ?? undefined,
        location: area.location ?? undefined,
        capacity: area.capacity ?? undefined,
        requiresApproval: area.requiresApproval,
        active: !area.active,
      });
    } catch {
      // error mostrado en el aviso
    }
  }

  function startBooking(area?: CommonArea) {
    if (area) {
      setAreaId(area.id);
    }
    setShowBooking(true);
  }

  const failed = areas.isError || reservations.isError || createReservation.isError
    || createArea.isError || updateArea.isError || review.isError || cancel.isError;

  if (areas.isLoading || reservations.isLoading) {
    return <FullPageLoader />;
  }

  return (
    <div className="page reservations-page">
      <header className="page-header">
        <div>
          <h1>{t("reservations.title")}</h1>
          <p className="page-subtitle">{t("reservations.subtitle")}</p>
        </div>
        <div className="toolbar">
          {canManageZones ? (
            <Button variant="secondary" onClick={() => setShowAreaForm((value) => !value)}>
              {t("reservations.newArea")}
            </Button>
          ) : null}
          {canCreate ? (
            <Button onClick={() => startBooking()}>
              <Plus size={16} aria-hidden="true" /> {t("reservations.newReservation")}
            </Button>
          ) : null}
        </div>
      </header>

      {failed ? <Alert variant="error">{t("reservations.error")}</Alert> : null}

      <div className="reservation-kpis">
        <div className="reservation-kpi">
          <span>{t("reservations.kpiAreas")}</span>
          <strong>{activeAreas.length}</strong>
        </div>
        <div className="reservation-kpi">
          <span>{t("reservations.kpiUpcoming")}</span>
          <strong>{upcoming.length}</strong>
        </div>
        <div className="reservation-kpi">
          <span>{t("reservations.kpiMine")}</span>
          <strong>{mine.length}</strong>
        </div>
        {canManage ? (
          <div className="reservation-kpi">
            <span>{t("reservations.kpiPending")}</span>
            <strong>{pending.length}</strong>
          </div>
        ) : null}
      </div>

      {showAreaForm && canManageZones ? (
        <form className="form-card" onSubmit={submitArea}>
          <h3>{t("reservations.newArea")}</h3>
          <div className="form-grid">
            <Input label={t("reservations.areaName")} value={areaName} onChange={(event) => setAreaName(event.target.value)} required />
            <Input label={t("reservations.location")} value={areaLocation} onChange={(event) => setAreaLocation(event.target.value)} />
            <Input label={t("reservations.capacity")} type="number" min="0" value={areaCapacity} onChange={(event) => setAreaCapacity(event.target.value)} />
            <label className="inline-check">
              <input type="checkbox" checked={areaApproval} onChange={(event) => setAreaApproval(event.target.checked)} />
              <span>{t("reservations.requiresApproval")}</span>
            </label>
          </div>
          <Textarea label={t("reservations.description")} value={areaDescription} onChange={(event) => setAreaDescription(event.target.value)} rows={2} />
          <div className="form-actions">
            <Button type="submit" loading={createArea.isPending}>{t("common.save")}</Button>
            <Button type="button" variant="secondary" onClick={() => setShowAreaForm(false)}>{t("common.cancel")}</Button>
          </div>
        </form>
      ) : null}

      {showBooking && canCreate ? (
        <form className="form-card" onSubmit={submitBooking}>
          <h3>{t("reservations.newReservation")}</h3>
          <div className="form-grid">
            <Select label={t("reservations.area")} value={areaId} onChange={(event) => setAreaId(event.target.value)} required>
              <option value="">{t("reservations.selectArea")}</option>
              {activeAreas.map((area) => (
                <option value={area.id} key={area.id}>{area.name}</option>
              ))}
            </Select>
            <Input label={t("reservations.date")} type="date" min={todayValue} value={date} onChange={(event) => setDate(event.target.value)} required />
            <Input label={t("reservations.startTime")} type="time" value={startTime} onChange={(event) => setStartTime(event.target.value)} required />
            <Input label={t("reservations.endTime")} type="time" value={endTime} onChange={(event) => setEndTime(event.target.value)} required />
          </div>
          <Textarea label={t("reservations.notes")} value={notes} onChange={(event) => setNotes(event.target.value)} rows={2} />
          <div className="form-actions">
            <Button type="submit" loading={createReservation.isPending} disabled={!areaId}>{t("reservations.reserve")}</Button>
            <Button type="button" variant="secondary" onClick={() => setShowBooking(false)}>{t("common.cancel")}</Button>
          </div>
        </form>
      ) : null}

      <section className="reservation-areas">
        <div className="section-heading">
          <h2>{t("reservations.areas")}</h2>
          <span className="badge badge--muted">{activeAreas.length}</span>
        </div>
        {activeAreas.length || canManageZones ? (
          <div className="area-grid">
            {(canManageZones ? areaList : activeAreas).map((area) => (
              <article className={`area-card ${area.active ? "" : "area-card--inactive"}`} key={area.id}>
                <header className="area-card__head">
                  <h3>{area.name}</h3>
                  {!area.active ? <span className="badge badge--muted">{t("reservations.inactive")}</span> : null}
                </header>
                {area.description ? <p>{area.description}</p> : null}
                <div className="area-card__meta">
                  {area.location ? <span>{area.location}</span> : null}
                  {area.capacity ? <span>{t("reservations.capacityValue", { count: area.capacity })}</span> : null}
                  <span className={`badge ${area.requiresApproval ? "badge--muted" : "badge--success"}`}>
                    {area.requiresApproval ? t("reservations.approvalRequired") : t("reservations.autoApproved")}
                  </span>
                </div>
                <div className="area-card__actions">
                  {canCreate && area.active ? (
                    <Button variant="secondary" onClick={() => startBooking(area)}>
                      <CalendarCheck size={14} aria-hidden="true" /> {t("reservations.reserve")}
                    </Button>
                  ) : null}
                  {canManageZones ? (
                    <Button variant="ghost" loading={updateArea.isPending} onClick={() => void toggleArea(area)}>
                      {area.active ? t("reservations.deactivate") : t("reservations.activate")}
                    </Button>
                  ) : null}
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="empty-state">{t("reservations.noAreas")}</div>
        )}
      </section>

      <section>
        <div className="tabs" role="tablist">
          {(["UPCOMING", "MINE", "ALL"] as const).map((value) => (
            <button
              type="button"
              key={value}
              role="tab"
              aria-selected={tab === value}
              className={`tab ${tab === value ? "tab--active" : ""}`}
              onClick={() => setTab(value)}
            >
              {t(`reservations.tabs.${value}`)}
            </button>
          ))}
        </div>

        {filtered.length ? (
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>{t("reservations.date")}</th>
                  <th>{t("reservations.time")}</th>
                  <th>{t("reservations.area")}</th>
                  <th>{t("reservations.neighbor")}</th>
                  <th>{t("reservations.status")}</th>
                  <th>{t("reservations.actions")}</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((reservation) => (
                  <ReservationRow
                    key={reservation.id}
                    reservation={reservation}
                    language={language}
                    canManage={canManage}
                    onReview={(status) => void review.mutateAsync({ reservationId: reservation.id, status })}
                    onCancel={() => void cancel.mutateAsync(reservation.id)}
                    reviewing={review.isPending}
                    cancelling={cancel.isPending}
                  />
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">{t("reservations.noReservations")}</div>
        )}
      </section>
    </div>
  );
}

function ReservationRow({
  reservation,
  language,
  canManage,
  onReview,
  onCancel,
  reviewing,
  cancelling,
}: {
  reservation: Reservation;
  language: ReturnType<typeof normalizeLanguage>;
  canManage: boolean;
  onReview: (status: ReservationStatus) => void;
  onCancel: () => void;
  reviewing: boolean;
  cancelling: boolean;
}) {
  const { t } = useTranslation();
  const canCancel = (reservation.mine || canManage)
    && (reservation.status === "PENDING" || reservation.status === "APPROVED");
  return (
    <tr>
      <td data-label={t("reservations.date")}>{formatDate(new Date(`${reservation.date}T00:00:00`), language)}</td>
      <td data-label={t("reservations.time")}>{timeLabel(reservation.startTime)}–{timeLabel(reservation.endTime)}</td>
      <td data-label={t("reservations.area")}>{reservation.areaName ?? "—"}</td>
      <td data-label={t("reservations.neighbor")}>
        {reservation.userName ?? "—"} {reservation.mine ? <span className="badge badge--muted">{t("reservations.you")}</span> : null}
      </td>
      <td data-label={t("reservations.status")}>
        <span className={`badge reservation-status reservation-status--${reservation.status.toLowerCase()}`}>
          {t(`reservations.statuses.${reservation.status}`)}
        </span>
      </td>
      <td data-label={t("reservations.actions")}>
        <div className="reservation-actions">
          {canManage && reservation.status === "PENDING" ? (
            <>
              <Button variant="ghost" loading={reviewing} onClick={() => onReview("APPROVED")}>
                <Check size={14} aria-hidden="true" /> {t("reservations.approve")}
              </Button>
              <Button variant="ghost" loading={reviewing} onClick={() => onReview("REJECTED")}>
                <X size={14} aria-hidden="true" /> {t("reservations.reject")}
              </Button>
            </>
          ) : null}
          {canCancel ? (
            <Button variant="ghost" loading={cancelling} onClick={onCancel}>
              {t("reservations.cancel")}
            </Button>
          ) : null}
        </div>
      </td>
    </tr>
  );
}
