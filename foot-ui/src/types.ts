export enum Role {
  USER = 'user',
  MODEL = 'model',
}

export type ChatPhase = 'thinking' | 'acting' | 'responding';

export type ChatEventType = 'phase' | 'message' | 'done' | 'error';

export type ChatEvent =
  | { event: 'phase'; data: PhasePayload }
  | { event: 'message'; data: MessagePayload }
  | { event: 'done'; data: DonePayload }
  | { event: 'error'; data: ErrorPayload };

export interface PhasePayload {
  phase: ChatPhase;
}

export interface MessagePayload {
  text: string;
}

export interface DonePayload {
  sessionId: string;
}

export interface ErrorPayload {
  error: string;
}

export interface FileAttachment {
  name: string;
  path: string;
  size: number;
  type: string;
}

export interface Message {
  id: string;
  role: Role;
  text: string;
  timestamp: number;
  isError?: boolean;
  draftText?: string;
  finalText?: string;
  streamingPhase?: ChatPhase | null;
  isStreaming?: boolean;
  attachments?: FileAttachment[];
}

export interface Session {
  id: string;
  title: string;
  messages: Message[];
  createdAt: number;
  updatedAt: number;
}

export interface AppSettings {
  baseUrl: string;
}

export interface UploadResponse {
  path: string;
  mimeType: string;
  type: 'image' | 'file';
}
