import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { useAuthStore } from "../features/auth/authStore";
import { canManageVotings } from "../features/mvp/utils";
import {
  useCastVote,
  useCloseVoting,
  useOpenVoting,
  useVoting,
} from "../features/votings/queries";
import { formatDateTime, formatPercent, formatPercentValue } from "../utils/format";

export function VotingDetailPage() {
  const { t, i18n } = useTranslation();
  const { communityId, votingId } = useParams<{ communityId: string; votingId: string }>();
  const user = useAuthStore((state) => state.user);
  const canManage = canManageVotings(user?.memberships.find((item) => item.id === communityId)?.roles ?? []);
  const language = i18n.language.startsWith("ca") ? "ca" : "es";
  const voting = useVoting(communityId, votingId);
  const openVoting = useOpenVoting(communityId, votingId);
  const closeVoting = useCloseVoting(communityId, votingId);
  const castVote = useCastVote(communityId, votingId);
  const [optionId, setOptionId] = useState("");

  if (voting.isLoading) return <FullPageLoader />;
  if (voting.isError || !voting.data) {
    return <div className="page"><Alert variant="error">{t("votings.error")}</Alert></div>;
  }
  const data = voting.data;
  const item = data.voting;
  const canVote = item.status === "OPEN" && !item.hasVoted;

  async function vote() {
    if (!optionId) return;
    try { await castVote.mutateAsync(optionId); } catch { /* state */ }
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <Link className="breadcrumb" to={`/communities/${communityId}/votings`}>{t("votings.backToList")}</Link>
          <h1>{item.title}</h1>
          <div className="chips">
            <span className="badge">{t(`votings.statuses.${item.status}`)}</span>
            <span className="badge">{t("votings.eligible")}: {item.totalEligible}</span>
          </div>
        </div>
        {canManage && item.status === "DRAFT" ? (
          <Button variant="secondary" loading={openVoting.isPending} onClick={() => void openVoting.mutateAsync()}>
            {t("votings.open")}
          </Button>
        ) : null}
        {canManage && item.status === "OPEN" ? (
          <Button variant="secondary" loading={closeVoting.isPending} onClick={() => void closeVoting.mutateAsync()}>
            {t("votings.close")}
          </Button>
        ) : null}
      </header>

      <Alert variant="info">{t("votings.legalNotice")}</Alert>
      {openVoting.isError || closeVoting.isError || castVote.isError ? (
        <Alert variant="error">{t("votings.error")}</Alert>
      ) : null}

      <section className="detail-card">
        <h2>{t("votings.details")}</h2>
        <dl className="detail-list">
          <dt>{t("votings.closesAt")}</dt><dd>{formatDateTime(item.closesAt, language)}</dd>
          <dt>{t("votings.status")}</dt><dd>{t(`votings.statuses.${item.status}`)}</dd>
        </dl>
        {item.description ? <p className="detail-description">{item.description}</p> : null}
      </section>

      {data.results ? (
        <section className="detail-card">
          <h2>{t("votings.results")}</h2>
          <p>
            {t("votings.quorum")}: {formatPercentValue(data.results.quorumPercentage, language)}
            {" · "}
            {t("votings.participation", {
              participating: data.results.participatingEligible,
              total: data.results.totalEligible,
            })}
          </p>
          <div className="table-wrapper">
            <table className="table">
              <thead><tr>
                <th>{t("votings.option")}</th><th>{t("votings.votes")}</th>
                <th>{t("votings.coefficient")}</th><th>%</th>
              </tr></thead>
              <tbody>
                {data.results.options.map((result) => (
                  <tr key={result.optionId}>
                    <td data-label={t("votings.option")}>{result.label}</td>
                    <td data-label={t("votings.votes")}>{result.votes}</td>
                    <td data-label={t("votings.coefficient")}>{formatPercent(result.coefficient, language)}</td>
                    <td data-label="%">{formatPercentValue(result.percentage, language)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : (
        <section className="detail-card">
          <h2>{t("votings.options")}</h2>
          <div className="voting-options">
            {data.options.map((option) => (
              <label className={`voting-option ${optionId === option.id ? "voting-option--selected" : ""}`} key={option.id}>
                <input
                  type="radio"
                  name="voting-option"
                  value={option.id}
                  checked={optionId === option.id}
                  onChange={() => setOptionId(option.id)}
                  disabled={!canVote}
                />
                <span>{option.label}</span>
              </label>
            ))}
          </div>
          {item.hasVoted ? <Alert variant="success">{t("votings.alreadyVoted")}</Alert> : null}
          {canVote ? (
            <div className="form-actions">
              <Button loading={castVote.isPending} disabled={!optionId} onClick={() => void vote()}>
                {t("votings.vote")}
              </Button>
            </div>
          ) : null}
        </section>
      )}
    </div>
  );
}
