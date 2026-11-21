import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { FullPageLoader } from "../components/FullPageLoader";
import { Input } from "../components/Input";
import { Select } from "../components/Select";
import { Textarea } from "../components/Textarea";
import { useAuthStore } from "../features/auth/authStore";
import {
  useChatChannels,
  useChatMessages,
  useCreateChatChannel,
  useSendChatMessage,
} from "../features/chat/queries";
import { createChatClient } from "../features/chat/socket";
import { useMembers } from "../features/communities/queries";
import { useCommunityStore } from "../features/communities/communityStore";
import { canModerateChat, formatChatTime } from "../features/mvp/utils";
import { demoEnabled } from "../services/demoApi";
import type { ChatChannelType, ChatMessage } from "../types/api";
import { normalizeLanguage } from "../utils/language";

export function ChatPage() {
  const { t, i18n } = useTranslation();
  const { communityId } = useParams<{ communityId: string }>();
  const user = useAuthStore((state) => state.user);
  const setSelectedCommunityId = useCommunityStore((state) => state.setSelectedCommunityId);
  const canModerate = canModerateChat(user?.memberships.find((item) => item.id === communityId)?.roles ?? []);
  const language = normalizeLanguage(i18n.language);
  const channels = useChatChannels(communityId);
  const members = useMembers(communityId, canModerate);
  const [channelId, setChannelId] = useState("");
  const [showForm, setShowForm] = useState(false);
  const messages = useChatMessages(communityId, channelId);
  const sendMessage = useSendChatMessage(communityId, channelId);
  const createChannel = useCreateChatChannel(communityId);
  const [body, setBody] = useState("");
  const [name, setName] = useState("");
  const [type, setType] = useState<ChatChannelType>("THEMATIC");
  const [description, setDescription] = useState("");
  const [participants, setParticipants] = useState<string[]>([]);
  const [live, setLive] = useState(false);
  const queryClient = useQueryClient();
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => { if (communityId) setSelectedCommunityId(communityId); }, [communityId, setSelectedCommunityId]);
  useEffect(() => {
    if (!channelId && channels.data?.length) setChannelId(channels.data[0].id);
  }, [channelId, channels.data]);
  useEffect(() => { endRef.current?.scrollIntoView({ behavior: "smooth" }); }, [messages.data]);
  useEffect(() => {
    if (!communityId || !channelId || demoEnabled) {
      return;
    }
    const client = createChatClient();
    client.onConnect = () => {
      setLive(true);
      client.subscribe(`/topic/communities/${communityId}/channels/${channelId}`, (frame) => {
        const message = JSON.parse(frame.body) as ChatMessage;
        queryClient.setQueryData<ChatMessage[]>(
          ["chat-messages", communityId, channelId],
          (current) => {
            if (!current) {
              return [message];
            }
            if (current.some((item) => item.id === message.id)) {
              return current;
            }
            return [...current, message];
          },
        );
      });
    };
    client.onWebSocketClose = () => setLive(false);
    client.onStompError = () => setLive(false);
    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [communityId, channelId, queryClient]);

  async function send(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!body.trim()) return;
    try { await sendMessage.mutateAsync(body.trim()); setBody(""); } catch { /* state */ }
  }

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await createChannel.mutateAsync({
        type, name, description: description || undefined,
        memberMembershipIds: type === "PRIVATE" ? participants : undefined,
      });
      setShowForm(false); setName(""); setDescription(""); setParticipants([]);
    } catch { /* state */ }
  }

  if (channels.isLoading) return <FullPageLoader />;
  if (channels.isError) return <div className="page"><Alert variant="error">{t("chat.error")}</Alert></div>;
  const activeMembers = (members.data ?? []).filter((member) => member.status === "ACTIVE");

  return (
    <div className="page">
      <header className="page-header">
        <div><h1>{t("chat.title")}</h1><p className="page-subtitle">{t("chat.subtitle")}</p></div>
        {canModerate ? (
          <Button variant="secondary" onClick={() => setShowForm((value) => !value)}>{t("chat.newChannel")}</Button>
        ) : null}
      </header>
      {showForm ? (
        <form className="form-card" onSubmit={create}>
          <h3>{t("chat.newChannel")}</h3>
          {createChannel.isError ? <Alert variant="error">{t("chat.error")}</Alert> : null}
          <div className="form-grid">
            <Input label={t("chat.channelName")} value={name} onChange={(e) => setName(e.target.value)} required />
            <Select label={t("chat.channelType")} value={type} onChange={(e) => setType(e.target.value as ChatChannelType)}>
              <option value="THEMATIC">{t("chat.types.THEMATIC")}</option>
              <option value="PRIVATE">{t("chat.types.PRIVATE")}</option>
            </Select>
            <Textarea label={t("chat.description")} value={description} onChange={(e) => setDescription(e.target.value)} />
            {type === "PRIVATE" ? (
              <div className="chat-participants">
                <span className="field__label">{t("chat.participants")}</span>
                {activeMembers.map((member) => (
                  <label className="inline-check" key={member.membershipId}>
                    <input
                      type="checkbox"
                      checked={participants.includes(member.membershipId)}
                      onChange={(e) => setParticipants((current) => e.target.checked
                        ? [...current, member.membershipId]
                        : current.filter((id) => id !== member.membershipId))}
                    />
                    <span>{member.fullName}</span>
                  </label>
                ))}
              </div>
            ) : null}
          </div>
          <div className="form-actions"><Button type="submit" loading={createChannel.isPending}>{t("chat.create")}</Button></div>
        </form>
      ) : null}

      <div className="chat-layout">
        <aside className="chat-channels">
          {channels.data?.map((channel) => (
            <button
              type="button"
              key={channel.id}
              className={`chat-channel ${channel.id === channelId ? "chat-channel--active" : ""}`}
              onClick={() => setChannelId(channel.id)}
            >
              <strong>{channel.name}</strong>
              <small>{t(`chat.types.${channel.type}`)}</small>
            </button>
          ))}
        </aside>
        <section className="chat-panel">
          {channelId ? (
            <div className="chips">
              <span className="badge">{live ? t("chat.live") : t("chat.polling")}</span>
            </div>
          ) : null}
          {!channelId ? <p>{t("chat.noChannels")}</p> : messages.isLoading ? <FullPageLoader /> : (
            <>
              <div className="chat-messages">
                {messages.data?.length === 0 ? <p>{t("chat.noMessages")}</p> : messages.data?.map((message) => (
                  <article className="chat-message" key={message.id}>
                    <header>
                      <strong>{message.senderName ?? "—"}</strong>
                      <small>{formatChatTime(message.createdAt, language)}</small>
                    </header>
                    <p>{message.body}</p>
                  </article>
                ))}
                <div ref={endRef} />
              </div>
              <form className="chat-composer" onSubmit={send}>
                <Input
                  label={t("chat.message")}
                  value={body}
                  onChange={(e) => setBody(e.target.value)}
                  maxLength={4000}
                  required
                />
                <Button type="submit" loading={sendMessage.isPending}>{t("chat.send")}</Button>
              </form>
              {sendMessage.isError ? <Alert variant="error">{t("chat.error")}</Alert> : null}
            </>
          )}
        </section>
      </div>
    </div>
  );
}
