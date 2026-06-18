import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Menu, Radar } from 'lucide-react';
import {
  FileAttachment,
  Message,
  Role,
  Session,
} from './types';
import Sidebar from './components/Sidebar';
import ChatInput from './components/ChatInput';
import ChatMessage from './components/ChatMessage';
import { generateSessionTitle, streamChatResponse } from './services/manyFootApi';

const generateId = () => Date.now().toString(36) + Math.random().toString(36).substring(2);
const REPLAY_DETECTION_MAX_PREFIX_LENGTH = 80;
const REPLAY_DETECTION_MIN_PREFIX_LENGTH = 16;
const REPLAY_PREAMBLE_MAX_LENGTH = 200;

const findReplayStartIndex = (currentText: string, incomingText: string): number => {
  const maxPrefixLength = Math.min(REPLAY_DETECTION_MAX_PREFIX_LENGTH, incomingText.length);

  for (let prefixLength = maxPrefixLength; prefixLength >= REPLAY_DETECTION_MIN_PREFIX_LENGTH; prefixLength -= 1) {
    const replayPrefix = incomingText.slice(0, prefixLength);
    const replayStartIndex = currentText.indexOf(replayPrefix);
    if (replayStartIndex >= 0) return replayStartIndex;
  }

  return -1;
};

const mergeAssistantText = (currentText: string, incomingText: string): string => {
  if (!incomingText) return currentText;
  if (!currentText) return incomingText;
  if (currentText.endsWith(incomingText)) return currentText;

  // 后端偶发会先流式输出草稿，再把完整最终稿作为一条 message 重发；
  // 这种情况下继续 append 会导致整段回答重复，并保留早期碎片化 Markdown。
  if (incomingText.length >= REPLAY_DETECTION_MIN_PREFIX_LENGTH) {
    const replayStartIndex = findReplayStartIndex(currentText, incomingText);

    if (replayStartIndex >= 0) {
      const preamble = currentText.slice(0, replayStartIndex).trim();
      if (preamble.length <= REPLAY_PREAMBLE_MAX_LENGTH) return incomingText;
      return currentText.slice(0, replayStartIndex) + incomingText;
    }
  }

  return currentText + incomingText;
};

const resolveAssistantText = (
  currentText: string,
  currentDraftText: string,
  incomingText: string,
): Pick<Message, 'text' | 'draftText' | 'finalText'> => {
  const mergedText = mergeAssistantText(currentText, incomingText);
  const draftText = currentDraftText + incomingText;

  if (mergedText !== currentText + incomingText) {
    return {
      text: mergedText,
      draftText: currentDraftText || undefined,
      finalText: mergedText,
    };
  }

  return {
    text: mergedText,
    draftText,
    finalText: undefined,
  };
};

function App() {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);
  const [isStreaming, setIsStreaming] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const chatContainerRef = useRef<HTMLDivElement>(null);

  const createSessionObject = (title = '新对话'): Session => ({
    id: generateId(),
    title,
    messages: [],
    createdAt: Date.now(),
    updatedAt: Date.now(),
  });

  useEffect(() => {
    const saved = localStorage.getItem('manus-chat-sessions');
    if (saved) {
      try {
        const parsed = JSON.parse(saved) as Session[];
        setSessions(parsed);
        setCurrentSessionId(parsed[0]?.id ?? null);
      } catch (error) {
        console.error('解析会话失败', error);
        const fallback = createSessionObject();
        setSessions([fallback]);
        setCurrentSessionId(fallback.id);
      }
    } else {
      const initial = createSessionObject();
      setSessions([initial]);
      setCurrentSessionId(initial.id);
    }
  }, []);

  useEffect(() => {
    if (sessions.length > 0) {
      localStorage.setItem('manus-chat-sessions', JSON.stringify(sessions));
    }
  }, [sessions]);

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    if (isStreaming) scrollToBottom();
  }, [sessions, isStreaming, scrollToBottom]);

  const getCurrentSession = () => sessions.find((session) => session.id === currentSessionId);

  const createNewSession = () => {
    const newSession = createSessionObject();
    setSessions((prev) => [newSession, ...prev]);
    setCurrentSessionId(newSession.id);
  };

  const deleteSession = (event: React.MouseEvent, id: string) => {
    event.stopPropagation();
    const nextSessions = sessions.filter((session) => session.id !== id);
    setSessions(nextSessions);
    localStorage.setItem('manus-chat-sessions', JSON.stringify(nextSessions));

    if (currentSessionId === id) {
      setCurrentSessionId(nextSessions[0]?.id ?? null);
    }
  };

  const ensureActiveSession = (): string => {
    if (currentSessionId) return currentSessionId;

    const newSession = createSessionObject();
    setSessions((prev) => [newSession, ...prev]);
    setCurrentSessionId(newSession.id);
    return newSession.id;
  };

  const handleSendMessage = async (text: string, attachments?: FileAttachment[]) => {
    if (isStreaming) return;

    const activeSessionId = ensureActiveSession();
    const now = Date.now();
    const userMsg: Message = {
      id: generateId(),
      role: Role.USER,
      text,
      timestamp: now,
      attachments,
    };

    const botMsgId = generateId();
    const botMsg: Message = {
      id: botMsgId,
      role: Role.MODEL,
      text: '',
      draftText: '',
      finalText: undefined,
      timestamp: Date.now(),
      streamingPhase: 'thinking',
      isStreaming: true,
    };

    setSessions((prev) => prev.map((session) => (
      session.id === activeSessionId
        ? {
          ...session,
          messages: [...session.messages, userMsg, botMsg],
          updatedAt: Date.now(),
        }
        : session
    )));

    setIsStreaming(true);

    try {
      const filePaths = attachments?.map(a => a.path);
      const stream = streamChatResponse(activeSessionId, text, filePaths);
      let streamClosed = false;

      for await (const event of stream) {
        if (event.event === 'phase') {
          setSessions((prev) => prev.map((session) => {
            if (session.id !== activeSessionId) return session;
            const messages = session.messages.map((message) => (
              message.id === botMsgId
                ? { ...message, streamingPhase: event.data.phase, isStreaming: true, timestamp: Date.now() }
                : message
            ));
            return { ...session, messages, updatedAt: Date.now() };
          }));
          continue;
        }

        if (event.event === 'message') {
          setSessions((prev) => prev.map((session) => {
            if (session.id !== activeSessionId) return session;
            const messages = session.messages.map((message) => (
              message.id === botMsgId
                ? (() => {
                  const nextText = resolveAssistantText(
                    message.text,
                    message.draftText || '',
                    event.data.text,
                  );

                  return {
                    ...message,
                    ...nextText,
                    isStreaming: true,
                    timestamp: Date.now(),
                  };
                })()
                : message
            ));
            return { ...session, messages, updatedAt: Date.now() };
          }));
          continue;
        }

        if (event.event === 'error') {
          streamClosed = true;
          setSessions((prev) => prev.map((session) => {
            if (session.id !== activeSessionId) return session;
            const messages = session.messages.map((message) => (
              message.id === botMsgId
                ? {
                  ...message,
                  text: event.data.error,
                  isError: true,
                  isStreaming: false,
                  streamingPhase: null,
                  timestamp: Date.now(),
                }
                : message
            ));
            return { ...session, messages, updatedAt: Date.now() };
          }));
          break;
        }

        if (event.event === 'done') {
          streamClosed = true;
          setSessions((prev) => prev.map((session) => {
            if (session.id !== activeSessionId) return session;
            const messages = session.messages.map((message) => (
              message.id === botMsgId
                ? {
                  ...message,
                  draftText: message.finalText ? message.draftText : undefined,
                  finalText: message.finalText || message.text,
                  isStreaming: false,
                  streamingPhase: null,
                  timestamp: Date.now(),
                }
                : message
            ));
            return { ...session, messages, updatedAt: Date.now() };
          }));
          break;
        }
      }

      if (!streamClosed) {
        setSessions((prev) => prev.map((session) => {
          if (session.id !== activeSessionId) return session;
          const messages = session.messages.map((message) => (
            message.id === botMsgId
              ? {
                ...message,
                draftText: message.finalText ? message.draftText : undefined,
                finalText: message.finalText || message.text,
                isStreaming: false,
                streamingPhase: null,
                timestamp: Date.now(),
              }
              : message
          ));
          return { ...session, messages, updatedAt: Date.now() };
        }));
      }

      const title = await generateSessionTitle(text);
      setSessions((prev) => prev.map((session) => {
        if (session.id !== activeSessionId) return session;
        const userMessages = session.messages.filter((message) => message.role === Role.USER);
        return userMessages.length <= 1 ? { ...session, title, updatedAt: Date.now() } : session;
      }));
    } catch (error) {
      console.error('生成响应失败', error);
      setSessions((prev) => prev.map((session) => {
        if (session.id !== activeSessionId) return session;
        const messages = session.messages.map((message) => (
          message.id === botMsgId
            ? {
              ...message,
              text: '连接失败。请检查 /api/chat/stream 服务状态。',
              isError: true,
              isStreaming: false,
              streamingPhase: null,
            }
            : message
        ));
        return { ...session, messages, updatedAt: Date.now() };
      }));
    } finally {
      setIsStreaming(false);
    }
  };

  const currentSession = getCurrentSession();

  return (
    <div className="flex h-screen overflow-hidden bg-white text-gray-900 font-sans">
      <Sidebar
        sessions={sessions}
        currentSessionId={currentSessionId}
        onSelectSession={setCurrentSessionId}
        onNewChat={createNewSession}
        onDeleteSession={deleteSession}
        isOpen={sidebarOpen}
        onCloseMobile={() => setSidebarOpen(false)}
      />

      <main className="relative flex h-full min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-10 flex items-center border-b border-gray-200 bg-white/95 p-4 backdrop-blur md:hidden">
          <button
            type="button"
            onClick={() => setSidebarOpen(true)}
            className="-ml-2 cursor-pointer rounded-lg p-2 text-gray-500 transition-colors duration-200 hover:bg-gray-100 hover:text-gray-900 focus:outline-none focus:ring-1 focus:ring-brand-primary/50"
          >
            <Menu size={24} />
          </button>
          <span className="ml-2 truncate font-sans text-sm text-gray-900">
            {currentSession?.title || 'ManyFoot'}
          </span>
        </header>

        <div ref={chatContainerRef} className="relative flex-1 overflow-y-auto scroll-smooth">
          {!currentSession || currentSession.messages.length === 0 ? (
            <div className="flex h-full flex-col items-center justify-center p-6 text-center text-gray-500">
              <div className="mb-6 flex h-20 w-20 items-center justify-center rounded-2xl border border-gray-200 bg-gray-50 text-brand-primary">
                <Radar size={42} />
              </div>
              <h1 className="mb-3 font-sans text-2xl text-gray-900 sm:text-3xl">ManyFoot</h1>
              <p className="max-w-xl text-sm leading-relaxed text-gray-500 sm:text-base">
                连接 AI 对话服务，开始智能交互。
              </p>
            </div>
          ) : (
            <div className="flex flex-col pb-4">
              {currentSession.messages.map((message) => (
                <ChatMessage key={message.id} message={message} sessionId={currentSession.id} />
              ))}
              <div ref={messagesEndRef} className="h-4" />
            </div>
          )}
        </div>

        <div className="z-10 flex-shrink-0">
          <ChatInput
            onSend={handleSendMessage}
            disabled={false}
            isStreaming={isStreaming}
            sessionId={currentSessionId || ''}
          />
        </div>
      </main>
    </div>
  );
}

export default App;
