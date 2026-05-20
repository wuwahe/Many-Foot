import React from 'react';
import ReactMarkdown from 'react-markdown';
import type { Components } from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Bot, Brain, Check, ChevronDown, ChevronRight, Copy, File, Loader2, MessageCircle, User, Wrench } from 'lucide-react';
import { ChatPhase, Message, Role } from '../types';

interface ChatMessageProps {
  message: Message;
}

const getNodeText = (node: React.ReactNode): string => {
  if (typeof node === 'string' || typeof node === 'number') {
    return String(node);
  }

  if (Array.isArray(node)) {
    return node.map(getNodeText).join('');
  }

  if (React.isValidElement<{ children?: React.ReactNode }>(node)) {
    return getNodeText(node.props.children);
  }

  return '';
};

const CodeBlock: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [copied, setCopied] = React.useState(false);
  const codeText = React.useMemo(() => getNodeText(children).replace(/\n$/, ''), [children]);

  const handleCopyCode = async () => {
    try {
      await navigator.clipboard.writeText(codeText);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      console.error('复制代码失败', error);
    }
  };

  return (
    <div className="group/code relative my-4 overflow-hidden rounded-xl border border-cyan-400/25 bg-slate-950 shadow-[0_0_24px_rgba(6,182,212,0.08)]">
      <button
        type="button"
        onClick={handleCopyCode}
        className="absolute right-2 top-2 z-10 inline-flex h-8 cursor-pointer items-center gap-1.5 rounded-md border border-cyan-400/30 bg-slate-900/90 px-2 text-xs font-medium text-cyan-100 transition-colors duration-200 hover:border-cyan-300 hover:bg-cyan-950 focus:outline-none focus:ring-2 focus:ring-cyan-300/60"
        aria-label={copied ? '代码已复制' : '复制代码'}
      >
        {copied ? <Check size={13} /> : <Copy size={13} />}
        <span>{copied ? '已复制' : '复制'}</span>
      </button>
      <pre className="m-0 overflow-x-auto p-4 pt-12 font-mono text-sm leading-6 text-slate-100 [&_code]:border-0 [&_code]:bg-transparent [&_code]:p-0 [&_code]:text-slate-100">
        {children}
      </pre>
    </div>
  );
};

const markdownComponents: Components = {
  pre: ({ children }) => <CodeBlock>{children}</CodeBlock>,
  code: ({ className, children, ...props }) => {
    const match = /language-([\w-]+)/.exec(className || '');

    if (match) {
      return (
        <code className={`${className} font-mono text-cyan-50`} {...props}>
          {children}
        </code>
      );
    }

    return (
      <code
        className="rounded border border-cyan-400/20 bg-cyan-950/50 px-1.5 py-0.5 font-mono text-[0.85em] text-cyan-200"
        {...props}
      >
        {children}
      </code>
    );
  },
};

const MarkdownContent: React.FC<{ children: string }> = ({ children }) => (
  <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>{children}</ReactMarkdown>
);

const ChatMessage: React.FC<ChatMessageProps> = ({ message }) => {
  const isUser = message.role === Role.USER;
  const [copied, setCopied] = React.useState(false);
  const [draftCollapsed, setDraftCollapsed] = React.useState(false);

  const phaseConfig: Record<ChatPhase, { label: string; icon: React.ReactNode }> = {
    thinking: { label: '正在思考…', icon: <Brain size={12} /> },
    acting: { label: '正在处理…', icon: <Wrench size={12} /> },
    responding: { label: '正在回答…', icon: <MessageCircle size={12} /> },
  };

  const phase = !isUser && message.isStreaming && message.streamingPhase ? phaseConfig[message.streamingPhase] : null;
  const draftText = !isUser && !message.isError ? message.draftText : undefined;
  const finalText = !isUser && !message.isError ? (message.finalText || (!draftText ? message.text : '')) : undefined;
  const showDraftPanel = Boolean(draftText?.trim());
  const shouldCollapseDraft = Boolean(finalText?.trim());
  const draftPanelId = `draft-panel-${message.id}`;

  React.useEffect(() => {
    setDraftCollapsed(shouldCollapseDraft);
  }, [shouldCollapseDraft, message.id]);

  const handleCopy = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      console.error('复制文本失败', error);
    }
  };

  return (
    <div className={`group flex w-full gap-3 p-4 sm:gap-4 md:p-6 ${isUser ? 'bg-transparent' : 'bg-gray-50/50'}`}>
      <div className="flex-shrink-0">
        <div className={`flex h-9 w-9 items-center justify-center rounded-xl transition-all duration-200 ${
          isUser
            ? 'bg-gray-100 text-gray-600'
            : 'bg-brand-light text-brand-primary'
        }`}>
          {isUser ? <User size={18} /> : <Bot size={18} />}
        </div>
      </div>

      <div className="min-w-0 flex-1 overflow-hidden">
        <div className="mb-2 flex items-center justify-between gap-3">
          <div className="text-xs font-medium text-gray-500">
            {isUser ? '你' : 'AI'}
          </div>
          <div className="flex items-center gap-2">
            {phase && (
              <span className="inline-flex items-center gap-1 rounded-full border border-brand-primary/20 bg-brand-light px-2 py-0.5 text-xs font-medium text-brand-primary">
                <Loader2 size={12} className="animate-spin" />
                {phase.icon}
                {phase.label}
              </span>
            )}
            <div className="text-xs text-gray-400">
              {new Date(message.timestamp).toLocaleTimeString('zh-CN', { hour12: false })}
            </div>
          </div>
        </div>

        {message.attachments && message.attachments.length > 0 && (
          <div className="mb-2 flex flex-wrap gap-2">
            {message.attachments.map((attachment, index) => (
              <div
                key={index}
                className="flex items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-1.5 text-xs"
              >
                <File size={12} className="text-brand-primary" />
                <span className="max-w-[120px] truncate text-gray-700">{attachment.name}</span>
              </div>
            ))}
          </div>
        )}

        {message.isError ? (
          <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-600">
            {message.text}
          </div>
        ) : (
          <>
            {showDraftPanel && (
              <div className="mb-3 rounded-xl border border-gray-200/70 bg-gray-50/45 text-gray-500/80 shadow-sm backdrop-blur-sm transition-colors duration-200">
                <button
                  type="button"
                  onClick={() => setDraftCollapsed((prev) => !prev)}
                  className="flex w-full cursor-pointer items-center justify-between gap-3 px-3 py-2 text-left text-xs font-medium text-gray-500/90 transition-colors duration-200 hover:bg-white/35 focus:outline-none focus:ring-1 focus:ring-brand-primary/30"
                  aria-expanded={!draftCollapsed}
                  aria-controls={draftPanelId}
                >
                  <span className="flex items-center gap-2">
                    {draftCollapsed ? <ChevronRight size={14} /> : <ChevronDown size={14} />}
                    <span>{message.isStreaming ? '生成过程片段' : '已收起生成过程'}</span>
                  </span>
                  <span className="text-[11px] text-gray-400/80">
                    {message.isStreaming ? '实时更新中' : '点击查看'}
                  </span>
                </button>

                {!draftCollapsed && (
                  <div
                    id={draftPanelId}
                    aria-live={message.isStreaming ? 'polite' : undefined}
                    className="max-h-40 overflow-y-auto border-t border-gray-200/60 px-3 py-2"
                  >
                    <div className="prose prose-sm max-w-none break-words text-gray-500/75 markdown-body opacity-80">
                      <MarkdownContent>{draftText || ''}</MarkdownContent>
                    </div>
                  </div>
                )}
              </div>
            )}

            {finalText ? (
              <div className="prose max-w-none text-gray-700 markdown-body">
                <MarkdownContent>{finalText}</MarkdownContent>
              </div>
            ) : !showDraftPanel ? (
              <div className="prose max-w-none text-gray-700 markdown-body">
                <MarkdownContent>{message.text || '...'}</MarkdownContent>
              </div>
            ) : null}
          </>
        )}

        {!isUser && !message.isError && (
          <div className="mt-4 flex items-center gap-2 opacity-0 transition-opacity duration-200 group-hover:opacity-100">
            <button
              type="button"
              onClick={() => handleCopy(message.finalText || message.text)}
              className="cursor-pointer rounded-lg border border-gray-200 p-1.5 text-gray-400 transition-colors duration-200 hover:border-gray-300 hover:bg-gray-50 hover:text-gray-600 focus:outline-none focus:ring-1 focus:ring-brand-primary/50"
              title="复制全部内容"
            >
              {copied ? <Check size={14} /> : <Copy size={14} />}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default React.memo(ChatMessage);
