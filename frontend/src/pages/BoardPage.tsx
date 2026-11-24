import { useEffect, useState } from "react";
import type { CSSProperties, FormEvent } from "react";
import { MessageSquare, Pencil, Pin, Plus, Send, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Textarea } from "../components/Textarea";
import { useHasDelegation } from "../features/authorizations/queries";
import { useAuthStore } from "../features/auth/authStore";
import {
  useAddBoardNoteComment,
  useBoardNoteComments,
  useBoardNotes,
  useCreateBoardNote,
  useDeleteBoardNote,
  useUpdateBoardNote,
} from "../features/board/queries";
import { useCommunityStore } from "../features/communities/communityStore";
import { canManageBoard, isReadOnly } from "../features/mvp/utils";
import type { BoardNote, BoardNoteColor } from "../types/api";
import { formatDate } from "../utils/format";
import { normalizeLanguage } from "../utils/language";

const COLORS: BoardNoteColor[] = ["YELLOW", "PINK", "BLUE", "GREEN", "ORANGE"];

function rotationFor(id: string): string {
  let hash = 0;
  for (let index = 0; index < id.length; index += 1) {
    hash = (hash * 31 + id.charCodeAt(index)) % 1000;
  }
  return `${((hash % 7) - 3) * 0.6}deg`;
}

function PostitComments({ communityId, noteId, readOnly }: {
  communityId: string;
  noteId: string;
  readOnly: boolean;
}) {
  const { t, i18n } = useTranslation();
  const language = normalizeLanguage(i18n.language);
  const comments = useBoardNoteComments(communityId, noteId);
  const add = useAddBoardNoteComment(communityId, noteId);
  const [body, setBody] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!body.trim()) {
      return;
    }
    try {
      await add.mutateAsync(body.trim());
      setBody("");
    } catch {
      // estado de la mutación
    }
  }

  return (
    <div className="postit__comments">
      {comments.isLoading ? (
        <p className="postit__comments-empty">{t("common.loading")}</p>
      ) : comments.data?.length ? (
        <ul>
          {comments.data.map((comment) => (
            <li key={comment.id}>
              <div className="postit__comment-meta">
                <strong>{comment.authorName ?? "—"}</strong>
                <small>{formatDate(comment.createdAt, language)}</small>
              </div>
              <p>{comment.body}</p>
            </li>
          ))}
        </ul>
      ) : (
        <p className="postit__comments-empty">{t("board.noComments")}</p>
      )}
      {add.isError ? <p className="postit__comments-error">{t("board.error")}</p> : null}
      {!readOnly ? (
        <form className="postit__comment-form" onSubmit={submit}>
          <input
            className="field__input"
            value={body}
            onChange={(event) => setBody(event.target.value)}
            placeholder={t("board.commentPlaceholder")}
            aria-label={t("board.commentPlaceholder")}
            maxLength={1000}
          />
          <button
            type="submit"
            className="postit__comment-send"
            aria-label={t("board.commentSubmit")}
            disabled={add.isPending}
          >
            <Send size={14} aria-hidden="true" />
          </button>
        </form>
      ) : null}
    </div>
  );
}

export function BoardPage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const roles = user?.memberships.find((item) => item.id === communityId)?.roles ?? [];
  const canManage = canManageBoard(roles);
  const readOnly = isReadOnly(roles);
  const delegated = useHasDelegation(communityId, "BOARD");
  const canWrite = !readOnly || delegated;
  const language = normalizeLanguage(i18n.language);
  const notes = useBoardNotes(communityId);
  const create = useCreateBoardNote(communityId);
  const update = useUpdateBoardNote(communityId);
  const remove = useDeleteBoardNote(communityId);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [color, setColor] = useState<BoardNoteColor>("YELLOW");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [commentsNoteId, setCommentsNoteId] = useState<string | null>(null);

  useEffect(() => {
    if (communityId) {
      setSelectedCommunityId(communityId);
    }
  }, [communityId, setSelectedCommunityId]);

  function reset() {
    setTitle("");
    setBody("");
    setColor("YELLOW");
    setEditingId(null);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      if (editingId) {
        await update.mutateAsync({ noteId: editingId, title, body, color });
      } else {
        await create.mutateAsync({ title, body, color });
      }
      reset();
    } catch {
      // estado de la mutación
    }
  }

  function startEdit(note: BoardNote) {
    setEditingId(note.id);
    setTitle(note.title);
    setBody(note.body);
    setColor(note.color);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  const failed = create.isError || update.isError || remove.isError;

  if (notes.isLoading) {
    return <FullPageLoader />;
  }

  return (
    <div className="page board-page">
      <header className="page-header">
        <div>
          <h1>{t("board.title")}</h1>
          <p className="page-subtitle">{t("board.subtitle")}</p>
        </div>
        <span className="badge">{t("board.count", { count: notes.data?.length ?? 0 })}</span>
      </header>

      {notes.isError ? <Alert variant="error">{t("board.error")}</Alert> : null}
      {failed ? <Alert variant="error">{t("board.error")}</Alert> : null}

      {canWrite ? (
      <form className="board-composer" onSubmit={submit}>
        <div className="board-composer__head">
          <strong>{editingId ? t("board.editNote") : t("board.newNote")}</strong>
          <div className="color-swatches" role="group" aria-label={t("board.color")}>
            {COLORS.map((value) => (
              <button
                type="button"
                key={value}
                className={`color-swatch color-swatch--${value.toLowerCase()} ${color === value ? "is-active" : ""}`}
                aria-label={t(`board.colors.${value}`)}
                aria-pressed={color === value}
                onClick={() => setColor(value)}
              />
            ))}
          </div>
        </div>
        <Input label={t("board.noteTitle")} value={title} onChange={(event) => setTitle(event.target.value)} required />
        <Textarea label={t("board.noteBody")} value={body} onChange={(event) => setBody(event.target.value)} rows={3} required />
        <div className="form-actions">
          <Button type="submit" loading={create.isPending || update.isPending}>
            <Plus size={16} aria-hidden="true" /> {editingId ? t("common.save") : t("board.post")}
          </Button>
          {editingId ? (
            <Button type="button" variant="secondary" onClick={reset}>{t("common.cancel")}</Button>
          ) : null}
        </div>
      </form>
      ) : null}

      <div className="board-frame">
        <div className="board-canvas">
          {notes.data?.length ? (
            notes.data.map((note) => {
              const canEdit = canWrite && (canManage || note.createdByUserId === user?.id);
              return (
                <article
                  key={note.id}
                  className={`postit postit--${note.color.toLowerCase()}${note.pinned ? " postit--pinned" : ""}`}
                  style={{ "--rotate": rotationFor(note.id) } as CSSProperties}
                >
                  <span className="postit__pin" aria-hidden="true"><Pin size={16} /></span>
                  {note.pinned ? <span className="postit__badge">{t("board.pinned")}</span> : null}
                  <h2>{note.title}</h2>
                  <p>{note.body}</p>
                  <footer className="postit__meta">
                    <span>{note.createdByName ?? "—"}</span>
                    <small>{formatDate(note.createdAt, language)}</small>
                  </footer>
                  <button
                    type="button"
                    className="postit__comments-toggle"
                    aria-expanded={commentsNoteId === note.id}
                    onClick={() => setCommentsNoteId(commentsNoteId === note.id ? null : note.id)}
                  >
                    <MessageSquare size={14} aria-hidden="true" />
                    <span>{t("board.comments")} ({note.commentCount})</span>
                  </button>
                  {commentsNoteId === note.id ? (
                    <PostitComments
                      communityId={communityId ?? ""}
                      noteId={note.id}
                      readOnly={!canWrite}
                    />
                  ) : null}
                  {canEdit ? (
                    <div className="postit__actions">
                      <button type="button" aria-label={t("common.edit")} title={t("common.edit")} onClick={() => startEdit(note)}>
                        <Pencil size={14} aria-hidden="true" />
                      </button>
                      <button
                        type="button"
                        aria-label={note.pinned ? t("board.unpin") : t("board.pin")}
                        title={note.pinned ? t("board.unpin") : t("board.pin")}
                        onClick={() => void update.mutateAsync({
                          noteId: note.id,
                          pinned: !note.pinned,
                          version: note.version,
                        })}
                      >
                        <Pin size={14} aria-hidden="true" />
                      </button>
                      <button type="button" aria-label={t("board.delete")} title={t("board.delete")} onClick={() => void remove.mutateAsync(note.id)}>
                        <Trash2 size={14} aria-hidden="true" />
                      </button>
                    </div>
                  ) : null}
                </article>
              );
            })
          ) : (
            <div className="board-empty">{t("board.empty")}</div>
          )}
        </div>
      </div>
    </div>
  );
}
