import {
  ChatEvent,
  ChatEventType,
  DonePayload,
  ErrorPayload,
  MessagePayload,
  PhasePayload,
  UploadResponse,
} from '../types';

const DEFAULT_BASE_URL = '';

const chatEventTypes: ChatEventType[] = [
  'phase',
  'message',
  'done',
  'error',
];

const isRecord = (value: unknown): value is Record<string, unknown> => (
  typeof value === 'object' && value !== null && !Array.isArray(value)
);

const isChatEventType = (value: unknown): value is ChatEventType => (
  typeof value === 'string' && chatEventTypes.includes(value as ChatEventType)
);

const isPhasePayload = (value: unknown): value is PhasePayload => (
  isRecord(value)
  && (value.phase === 'thinking' || value.phase === 'acting' || value.phase === 'responding')
);

const isMessagePayload = (value: unknown): value is MessagePayload => (
  isRecord(value)
  && typeof value.text === 'string'
);

const isDonePayload = (value: unknown): value is DonePayload => (
  isRecord(value)
  && typeof value.sessionId === 'string'
);

const isErrorPayload = (value: unknown): value is ErrorPayload => (
  isRecord(value)
  && typeof value.error === 'string'
);

const toChatEvent = (event: ChatEventType, data: unknown): ChatEvent | null => {
  if (event === 'phase' && isPhasePayload(data)) return { event, data };
  if (event === 'message' && isMessagePayload(data)) return { event, data };
  if (event === 'done' && isDonePayload(data)) return { event, data };
  if (event === 'error' && isErrorPayload(data)) return { event, data };
  return null;
};

const parseSSEBlock = (block: string): ChatEvent | null => {
  const lines = block.split(/\r?\n/);
  let frameEventType: string | null = null;
  const dataLines: string[] = [];

  for (const line of lines) {
    if (line.startsWith('event:')) {
      frameEventType = line.substring(6).trim();
    } else if (line.startsWith('data:')) {
      dataLines.push(line.substring(5).trimStart());
    }
  }

  if (!isChatEventType(frameEventType) || dataLines.length === 0) return null;

  try {
    return toChatEvent(frameEventType, JSON.parse(dataLines.join('\n')));
  } catch (error) {
    console.error('解析 SSE 数据失败', error);
    return null;
  }
};

export async function* streamChatResponse(
  sessionId: string,
  message: string,
  baseUrl?: string,
  filePaths?: string[]
): AsyncGenerator<ChatEvent, void, unknown> {
  const apiBase = baseUrl || DEFAULT_BASE_URL;
  const url = apiBase ? `${apiBase.replace(/\/$/, '')}/api/chat/stream` : '/api/chat/stream';

  try {
    const body: Record<string, unknown> = { sessionId, message, filePaths: filePaths ?? [] };

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) throw new Error(`HTTP error: ${response.status}`);
    if (!response.body) throw new Error('Response body is null');

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const parts = buffer.split(/\r?\n\r?\n/);
      buffer = parts.pop() || '';

      for (const part of parts) {
        if (!part.trim()) continue;

        const result = parseSSEBlock(part);
        if (!result) continue;

        yield result;
        if (result.event === 'done' || result.event === 'error') return;
      }
    }

    buffer += decoder.decode();
    const rest = buffer.trim();
    if (rest) {
      const result = parseSSEBlock(rest);
      if (result) yield result;
    }
  } catch (error) {
    console.error('Chat API 错误', error);
    throw error;
  }
}

export async function uploadFile(
  sessionId: string,
  file: File,
  baseUrl?: string
): Promise<UploadResponse> {
  const apiBase = baseUrl || DEFAULT_BASE_URL;
  const url = apiBase ? `${apiBase.replace(/\/$/, '')}/api/chat/upload` : '/api/chat/upload';

  const formData = new FormData();
  formData.append('sessionId', sessionId);
  formData.append('file', file);

  try {
    const response = await fetch(url, {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`上传失败: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('文件上传错误', error);
    throw error;
  }
}

export async function downloadFile(
  sessionId: string,
  path: string,
  baseUrl?: string
): Promise<Blob> {
  const apiBase = baseUrl || DEFAULT_BASE_URL;
  const url = apiBase
    ? `${apiBase.replace(/\/$/, '')}/api/files/download?sessionId=${encodeURIComponent(sessionId)}&path=${encodeURIComponent(path)}`
    : `/api/files/download?sessionId=${encodeURIComponent(sessionId)}&path=${encodeURIComponent(path)}`;

  try {
    const response = await fetch(url);

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('文件不存在');
      }
      if (response.status === 413) {
        throw new Error('文件过大');
      }
      throw new Error(`下载失败: ${response.status}`);
    }

    return await response.blob();
  } catch (error) {
    console.error('文件下载错误', error);
    throw error;
  }
}

export async function generateSessionTitle(firstMessage: string): Promise<string> {
  const normalized = firstMessage.replace(/\s+/g, ' ').trim();
  return normalized.slice(0, 18) + (normalized.length > 18 ? '...' : '');
}
