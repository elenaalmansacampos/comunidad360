import { useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Alert } from "../../components/Alert";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Select } from "../../components/Select";
import {
  useCreateReportSchedule,
  useDeleteReportSchedule,
  useReportSchedules,
  useRunReportSchedule,
  useUpdateReportSchedule,
} from "./queries";
import type { ReportFrequency } from "../../types/api";
import { formatDateTime } from "../../utils/format";
import { normalizeLanguage } from "../../utils/language";

interface ReportSchedulesProps {
  communityId: string;
  canManage: boolean;
}

export function ReportSchedules({ communityId, canManage }: ReportSchedulesProps) {
  const { t, i18n } = useTranslation();
  const language = normalizeLanguage(i18n.language);
  const schedules = useReportSchedules(communityId);
  const create = useCreateReportSchedule(communityId);
  const update = useUpdateReportSchedule(communityId);
  const remove = useDeleteReportSchedule(communityId);
  const run = useRunReportSchedule(communityId);
  const [name, setName] = useState("");
  const [frequency, setFrequency] = useState<ReportFrequency>("WEEKLY");
  const [recipients, setRecipients] = useState("");
  const [enabled, setEnabled] = useState(true);

  const failed = create.isError || update.isError || remove.isError || run.isError;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const list = recipients.split(",").map((value) => value.trim()).filter(Boolean);
    try {
      await create.mutateAsync({ name, frequency, recipients: list, enabled });
      setName("");
      setRecipients("");
      setEnabled(true);
    } catch {
      // estado de la mutación
    }
  }

  return (
    <section className="dashboard-section">
      <div className="section-heading">
        <div>
          <h2>{t("reports.title")}</h2>
          <p className="page-subtitle">{t("reports.subtitle")}</p>
        </div>
      </div>

      {failed ? <Alert variant="error">{t("reports.error")}</Alert> : null}

      {schedules.data?.length ? (
        <ul className="dashboard-list">
          {schedules.data.map((schedule) => (
            <li key={schedule.id}>
              <strong>{schedule.name}</strong>
              <span className="badge">{t(`reports.frequencies.${schedule.frequency}`)}</span>
              <span className="badge">
                {schedule.enabled ? t("reports.enabled") : t("reports.disabled")}
              </span>
              <small>{schedule.recipients.join(", ")}</small>
              <small>{t("reports.nextRun")}: {formatDateTime(schedule.nextRunAt, language)}</small>
              {schedule.lastRunAt ? (
                <small>
                  {t("reports.lastRun")}: {formatDateTime(schedule.lastRunAt, language)}
                  {schedule.lastStatus ? ` · ${t(`reports.statuses.${schedule.lastStatus}`)}` : ""}
                </small>
              ) : null}
              {canManage ? (
                <div className="form-actions">
                  <Button
                    variant="secondary"
                    loading={run.isPending}
                    onClick={() => void run.mutateAsync(schedule.id)}
                  >
                    {t("reports.run")}
                  </Button>
                  <Button
                    variant="secondary"
                    loading={remove.isPending}
                    onClick={() => void remove.mutateAsync(schedule.id)}
                  >
                    {t("reports.delete")}
                  </Button>
                  <label className="inline-check">
                    <input
                      type="checkbox"
                      checked={schedule.enabled}
                      onChange={() => void update.mutateAsync({
                        scheduleId: schedule.id,
                        enabled: !schedule.enabled,
                        recipients: schedule.recipients,
                        version: schedule.version,
                      })}
                    />
                    <span>{t("reports.enabled")}</span>
                  </label>
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      ) : <p>{t("reports.empty")}</p>}

      {canManage ? (
        <form className="form-card" onSubmit={submit}>
          <h3>{t("reports.new")}</h3>
          <div className="form-grid">
            <Input label={t("reports.name")} value={name} onChange={(event) => setName(event.target.value)} required />
            <Select
              label={t("reports.frequency")}
              value={frequency}
              onChange={(event) => setFrequency(event.target.value as ReportFrequency)}
            >
              <option value="DAILY">{t("reports.frequencies.DAILY")}</option>
              <option value="WEEKLY">{t("reports.frequencies.WEEKLY")}</option>
              <option value="MONTHLY">{t("reports.frequencies.MONTHLY")}</option>
            </Select>
            <Input
              label={t("reports.recipients")}
              value={recipients}
              onChange={(event) => setRecipients(event.target.value)}
              required
            />
            <label className="inline-check">
              <input type="checkbox" checked={enabled} onChange={(event) => setEnabled(event.target.checked)} />
              <span>{t("reports.enabled")}</span>
            </label>
          </div>
          <div className="form-actions">
            <Button type="submit" loading={create.isPending}>{t("common.save")}</Button>
          </div>
        </form>
      ) : null}
    </section>
  );
}
