import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Textarea } from "../components/Textarea";
import { useAuthStore } from "../features/auth/authStore";
import { useCommunityStore } from "../features/communities/communityStore";
import { canManageVotings } from "../features/mvp/utils";
import { useCreateVoting, useVotings } from "../features/votings/queries";
import { formatDateTime } from "../utils/format";
import { normalizeLanguage } from "../utils/language";

export function VotingsPage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const canManage = canManageVotings(user?.memberships.find((item) => item.id === communityId)?.roles ?? []);
  const language = normalizeLanguage(i18n.language);
  const [showForm, setShowForm] = useState(false);
  const votings = useVotings(communityId);

  useEffect(() => { if (communityId) setSelectedCommunityId(communityId); }, [communityId, setSelectedCommunityId]);

  if (votings.isLoading) return <FullPageLoader />;
  if (votings.isError) return <div className="page"><Alert variant="error">{t("votings.error")}</Alert></div>;

  return (
    <div className="page">
      <header className="page-header">
        <div><h1>{t("votings.title")}</h1><p className="page-subtitle">{t("votings.subtitle")}</p></div>
        {canManage ? <Button variant="secondary" onClick={() => setShowForm((v) => !v)}>{t("votings.newVoting")}</Button> : null}
      </header>
      <Alert variant="info">{t("votings.legalNotice")}</Alert>
      {showForm && communityId ? <VotingForm communityId={communityId} onDone={() => setShowForm(false)} /> : null}
      {votings.data?.length === 0 ? <div className="empty-state">{t("votings.noVotings")}</div> : (
        <div className="table-wrapper">
          <table className="table">
            <thead><tr>
              <th>{t("votings.votingTitle")}</th><th>{t("votings.status")}</th>
              <th>{t("votings.closesAt")}</th><th>{t("votings.eligible")}</th>
            </tr></thead>
            <tbody>
              {votings.data?.map((voting) => (
                <tr key={voting.id}>
                  <td data-label={t("votings.votingTitle")}>
                    <Link to={`/communities/${communityId}/votings/${voting.id}`}>{voting.title}</Link>
                  </td>
                  <td data-label={t("votings.status")}><span className="badge">{t(`votings.statuses.${voting.status}`)}</span></td>
                  <td data-label={t("votings.closesAt")}>{formatDateTime(voting.closesAt, language)}</td>
                  <td data-label={t("votings.eligible")}>{voting.totalEligible}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function VotingForm({ communityId, onDone }: { communityId: string; onDone: () => void }) {
  const { t } = useTranslation();
  const mutation = useCreateVoting(communityId);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [closesAt, setClosesAt] = useState("");
  const [options, setOptions] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const labels = options.split("\n").map((line) => line.trim()).filter(Boolean);
    try {
      await mutation.mutateAsync({
        title, description: description || undefined,
        closesAt: new Date(closesAt).toISOString(), options: labels,
      });
      onDone();
    } catch { /* state */ }
  }

  return (
    <form className="form-card" onSubmit={submit}>
      <h3>{t("votings.newVoting")}</h3>
      {mutation.isError ? <Alert variant="error">{t("votings.error")}</Alert> : null}
      <div className="form-grid">
        <Input label={t("votings.votingTitle")} value={title} onChange={(e) => setTitle(e.target.value)} required />
        <Input label={t("votings.closesAt")} type="datetime-local" value={closesAt} onChange={(e) => setClosesAt(e.target.value)} required />
        <Textarea label={t("votings.description")} value={description} onChange={(e) => setDescription(e.target.value)} />
        <Textarea label={t("votings.optionsHelp")} value={options} onChange={(e) => setOptions(e.target.value)} required />
      </div>
      <div className="form-actions"><Button type="submit" loading={mutation.isPending}>{t("votings.create")}</Button></div>
    </form>
  );
}
