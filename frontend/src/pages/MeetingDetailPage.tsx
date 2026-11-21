import { useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Select } from "../components/Select";
import { useAuthStore } from "../features/auth/authStore";
import { useMembers } from "../features/communities/queries";
import { useDocuments } from "../features/documents/queries";
import {
  useAddAgendaItem,
  useCreateAgreement,
  useLinkMinutes,
  useMeeting,
  useRecordAttendance,
  useUpdateMeeting,
} from "../features/meetings/queries";
import { canManageMeetings, nextMeetingStatuses } from "../features/meetings/utils";
import type { AttendanceType, MeetingStatus } from "../types/api";
import { formatDateTime, formatPercent } from "../utils/format";
import { normalizeLanguage } from "../utils/language";

const ATTENDANCE_TYPES: AttendanceType[] = ["IN_PERSON", "ONLINE", "REPRESENTED", "ABSENT"];

export function MeetingDetailPage() {
  const { t, i18n } = useTranslation();
  const { communityId, meetingId } = useParams<{ communityId: string; meetingId: string }>();
  const user = useAuthStore((state) => state.user);
  const membership = user?.memberships.find((item) => item.id === communityId);
  const canManage = canManageMeetings(membership?.roles ?? []);
  const language = normalizeLanguage(i18n.language);
  const meeting = useMeeting(communityId, meetingId);
  const members = useMembers(communityId, canManage);
  const documents = useDocuments(communityId);
  const updateMeeting = useUpdateMeeting(communityId, meetingId);
  const addAgenda = useAddAgendaItem(communityId, meetingId);
  const attendance = useRecordAttendance(communityId, meetingId);
  const agreement = useCreateAgreement(communityId, meetingId);
  const linkMinutes = useLinkMinutes(communityId, meetingId);

  const [agendaTitle, setAgendaTitle] = useState("");
  const [agendaDescription, setAgendaDescription] = useState("");
  const [memberId, setMemberId] = useState("");
  const [attendanceType, setAttendanceType] = useState<AttendanceType>("IN_PERSON");
  const [representativeId, setRepresentativeId] = useState("");
  const [agreementTitle, setAgreementTitle] = useState("");
  const [agendaItemId, setAgendaItemId] = useState("");
  const [votesFor, setVotesFor] = useState("0");
  const [votesAgainst, setVotesAgainst] = useState("0");
  const [abstentions, setAbstentions] = useState("0");
  const [minutesDocumentId, setMinutesDocumentId] = useState("");

  if (meeting.isLoading) {
    return <FullPageLoader />;
  }

  if (meeting.isError || !meeting.data) {
    return (
      <div className="page">
        <Alert variant="error">{t("meetings.error")}</Alert>
        <Link to={`/communities/${communityId}/meetings`}>{t("meetings.backToList")}</Link>
      </div>
    );
  }

  const detail = meeting.data;
  const data = detail.meeting;
  const activeMembers = (members.data ?? []).filter((member) => member.status === "ACTIVE");
  const minutesDocument = (documents.data ?? []).find((document) => document.id === data.minutesDocumentId);

  async function changeStatus(status: MeetingStatus) {
    try {
      await updateMeeting.mutateAsync({ status, version: data.version });
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  async function addAgendaSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await addAgenda.mutateAsync({ title: agendaTitle, description: agendaDescription || undefined });
      setAgendaTitle("");
      setAgendaDescription("");
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  async function attendanceSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!memberId) return;
    try {
      await attendance.mutateAsync({
        membershipId: memberId,
        attendanceType,
        representedByMembershipId: attendanceType === "REPRESENTED" ? representativeId || undefined : undefined,
      });
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  async function agreementSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await agreement.mutateAsync({
        agendaItemId: agendaItemId || undefined,
        title: agreementTitle,
        votesFor: Number(votesFor),
        votesAgainst: Number(votesAgainst),
        abstentions: Number(abstentions),
      });
      setAgreementTitle("");
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  async function linkMinutesSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!minutesDocumentId) return;
    try {
      await linkMinutes.mutateAsync(minutesDocumentId);
    } catch {
      // El error se muestra desde el estado de la mutación.
    }
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <Link className="breadcrumb" to={`/communities/${communityId}/meetings`}>
            {t("meetings.backToList")}
          </Link>
          <h1>{data.title}</h1>
          <div className="chips">
            <span className="badge">{t(`meetings.statuses.${data.status}`)}</span>
            <span className="badge">{t(`meetings.types.${data.type}`)}</span>
          </div>
        </div>
        {canManage ? (
          <div className="page-header__actions">
            {nextMeetingStatuses(data.status).map((status) => (
              <Button
                key={status}
                variant={status === "CANCELLED" ? "ghost" : "secondary"}
                onClick={() => void changeStatus(status)}
                loading={updateMeeting.isPending}
              >
                {t(`meetings.actions.${status}`)}
              </Button>
            ))}
          </div>
        ) : null}
      </header>

      {updateMeeting.isError ? <Alert variant="error">{t("meetings.error")}</Alert> : null}

      <section className="detail-grid">
        <article className="detail-card">
          <h2>{t("meetings.details")}</h2>
          <dl className="detail-list">
            <dt>{t("meetings.date")}</dt>
            <dd>{formatDateTime(data.scheduledStart, language)}</dd>
            <dt>{t("meetings.location")}</dt>
            <dd>{data.location ?? "—"}</dd>
            <dt>{t("meetings.created")}</dt>
            <dd>{formatDateTime(data.createdAt, language)}</dd>
          </dl>
        </article>
        <article className="detail-card">
          <h2>{t("meetings.minutes")}</h2>
          {minutesDocument ? (
            <p>
              <Link to={`/communities/${communityId}/documents/${minutesDocument.id}`}>
                {minutesDocument.title}
              </Link>
            </p>
          ) : (
            <p>{t("meetings.noMinutes")}</p>
          )}
          {canManage ? (
            <form className="form-stack" onSubmit={linkMinutesSubmit}>
              <Select
                label={t("meetings.selectMinutes")}
                value={minutesDocumentId}
                onChange={(event) => setMinutesDocumentId(event.target.value)}
              >
                <option value="">{t("meetings.selectDocument")}</option>
                {(documents.data ?? []).map((document) => (
                  <option value={document.id} key={document.id}>{document.title}</option>
                ))}
              </Select>
              <Button type="submit" loading={linkMinutes.isPending} disabled={!minutesDocumentId}>
                {t("meetings.linkMinutes")}
              </Button>
            </form>
          ) : null}
        </article>
      </section>

      <section className="detail-card">
        <h2>{t("meetings.agenda")}</h2>
        {detail.agenda.length === 0 ? <p>{t("meetings.noAgenda")}</p> : (
          <ol className="meeting-agenda">
            {detail.agenda.map((item) => (
              <li key={item.id}>
                <strong>{item.title}</strong>
                {item.description ? <p>{item.description}</p> : null}
              </li>
            ))}
          </ol>
        )}
        {canManage ? (
          <form className="form-card" onSubmit={addAgendaSubmit}>
            <h3>{t("meetings.addAgenda")}</h3>
            {addAgenda.isError ? <Alert variant="error">{t("meetings.error")}</Alert> : null}
            <div className="form-grid">
              <Input label={t("meetings.agendaTitle")} value={agendaTitle} onChange={(event) => setAgendaTitle(event.target.value)} required />
              <Input label={t("meetings.agendaDescription")} value={agendaDescription} onChange={(event) => setAgendaDescription(event.target.value)} />
            </div>
            <div className="form-actions">
              <Button type="submit" loading={addAgenda.isPending}>{t("meetings.add")}</Button>
            </div>
          </form>
        ) : null}
      </section>

      <section className="detail-card">
        <h2>{t("meetings.attendance")}</h2>
        {detail.attendance.length === 0 ? <p>{t("meetings.noAttendance")}</p> : (
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>{t("meetings.member")}</th>
                  <th>{t("meetings.attendanceType")}</th>
                  <th>{t("meetings.representedBy")}</th>
                  <th>{t("meetings.coefficient")}</th>
                </tr>
              </thead>
              <tbody>
                {detail.attendance.map((item) => (
                  <tr key={item.id}>
                    <td data-label={t("meetings.member")}>{item.memberName ?? "—"}</td>
                    <td data-label={t("meetings.attendanceType")}>{t(`meetings.attendanceTypes.${item.attendanceType}`)}</td>
                    <td data-label={t("meetings.representedBy")}>{item.representedByName ?? "—"}</td>
                    <td data-label={t("meetings.coefficient")}>{formatPercent(item.coefficientSnapshot, language)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {canManage ? (
          <form className="form-card" onSubmit={attendanceSubmit}>
            <h3>{t("meetings.recordAttendance")}</h3>
            {attendance.isError ? <Alert variant="error">{t("meetings.error")}</Alert> : null}
            <div className="form-grid">
              <Select label={t("meetings.member")} value={memberId} onChange={(event) => setMemberId(event.target.value)} required>
                <option value="">{t("meetings.selectMember")}</option>
                {activeMembers.map((member) => (
                  <option value={member.membershipId} key={member.membershipId}>{member.fullName}</option>
                ))}
              </Select>
              <Select label={t("meetings.attendanceType")} value={attendanceType} onChange={(event) => setAttendanceType(event.target.value as AttendanceType)}>
                {ATTENDANCE_TYPES.map((value) => (
                  <option value={value} key={value}>{t(`meetings.attendanceTypes.${value}`)}</option>
                ))}
              </Select>
              {attendanceType === "REPRESENTED" ? (
                <Select label={t("meetings.representedBy")} value={representativeId} onChange={(event) => setRepresentativeId(event.target.value)} required>
                  <option value="">{t("meetings.selectMember")}</option>
                  {activeMembers.map((member) => (
                    <option value={member.membershipId} key={member.membershipId}>{member.fullName}</option>
                  ))}
                </Select>
              ) : null}
            </div>
            <div className="form-actions">
              <Button type="submit" loading={attendance.isPending} disabled={!memberId}>{t("meetings.save")}</Button>
            </div>
          </form>
        ) : null}
      </section>

      <section className="detail-card">
        <h2>{t("meetings.agreements")}</h2>
        {detail.agreements.length === 0 ? <p>{t("meetings.noAgreements")}</p> : (
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>{t("meetings.agreementTitle")}</th>
                  <th>{t("meetings.votesFor")}</th>
                  <th>{t("meetings.votesAgainst")}</th>
                  <th>{t("meetings.abstentions")}</th>
                  <th>{t("meetings.adopted")}</th>
                </tr>
              </thead>
              <tbody>
                {detail.agreements.map((item) => (
                  <tr key={item.id}>
                    <td data-label={t("meetings.agreementTitle")}>{item.title}</td>
                    <td data-label={t("meetings.votesFor")}>{item.votesFor}</td>
                    <td data-label={t("meetings.votesAgainst")}>{item.votesAgainst}</td>
                    <td data-label={t("meetings.abstentions")}>{item.abstentions}</td>
                    <td data-label={t("meetings.adopted")}>
                      <span className="badge">{item.adopted ? t("meetings.yes") : t("meetings.no")}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {canManage ? (
          <form className="form-card" onSubmit={agreementSubmit}>
            <h3>{t("meetings.addAgreement")}</h3>
            {agreement.isError ? <Alert variant="error">{t("meetings.error")}</Alert> : null}
            <div className="form-grid">
              <Input label={t("meetings.agreementTitle")} value={agreementTitle} onChange={(event) => setAgreementTitle(event.target.value)} required />
              <Select label={t("meetings.agenda")} value={agendaItemId} onChange={(event) => setAgendaItemId(event.target.value)}>
                <option value="">{t("meetings.noAgendaItem")}</option>
                {detail.agenda.map((item) => (
                  <option value={item.id} key={item.id}>{item.title}</option>
                ))}
              </Select>
              <Input label={t("meetings.votesFor")} type="number" min={0} value={votesFor} onChange={(event) => setVotesFor(event.target.value)} />
              <Input label={t("meetings.votesAgainst")} type="number" min={0} value={votesAgainst} onChange={(event) => setVotesAgainst(event.target.value)} />
              <Input label={t("meetings.abstentions")} type="number" min={0} value={abstentions} onChange={(event) => setAbstentions(event.target.value)} />
            </div>
            <div className="form-actions">
              <Button type="submit" loading={agreement.isPending}>{t("meetings.add")}</Button>
            </div>
          </form>
        ) : null}
      </section>
    </div>
  );
}
