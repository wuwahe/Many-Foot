import React from 'react';
import { MessageSquare, MessageSquarePlus, Trash2, X } from 'lucide-react';
import { Session } from '../types';

interface SidebarProps {
  sessions: Session[];
  currentSessionId: string | null;
  onSelectSession: (id: string) => void;
  onNewChat: () => void;
  onDeleteSession: (event: React.MouseEvent, id: string) => void;
  isOpen: boolean;
  onCloseMobile: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({
  sessions,
  currentSessionId,
  onSelectSession,
  onNewChat,
  onDeleteSession,
  isOpen,
  onCloseMobile,
}) => {
  const sortedSessions = [...sessions].sort((a, b) => b.updatedAt - a.updatedAt);

  return (
    <>
      {isOpen && (
        <div
          className="fixed inset-0 z-20 bg-black/50 backdrop-blur-sm md:hidden"
          onClick={onCloseMobile}
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-30 flex w-72 transform flex-col border-r border-gray-200 bg-white backdrop-blur transition-transform duration-300 ease-in-out md:static md:transform-none ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="border-b border-gray-100 p-4">
          <div className="mb-4 flex items-center justify-between gap-3">
            <div>
              <div className="font-sans text-lg font-semibold text-gray-900">ManyFoot</div>
              <div className="text-xs text-gray-500">AI 对话助手</div>
            </div>
            <button
              type="button"
              onClick={onCloseMobile}
              className="cursor-pointer rounded-lg p-2 text-gray-500 transition-colors duration-200 hover:bg-gray-100 hover:text-gray-900 md:hidden"
            >
              <X size={22} />
            </button>
          </div>

          <button
            type="button"
            onClick={() => { onNewChat(); onCloseMobile(); }}
            className="flex w-full cursor-pointer items-center justify-center gap-2 rounded-xl bg-brand-primary px-4 py-3 text-sm font-medium text-white transition-all duration-200 hover:bg-brand-secondary focus:outline-none focus:ring-2 focus:ring-brand-primary/50"
          >
            <MessageSquarePlus size={18} />
            新建对话
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-2 py-4">
          <div className="px-3 py-2 text-xs font-medium uppercase tracking-wider text-gray-400">
            对话记录
          </div>

          <div className="space-y-1">
            {sortedSessions.length === 0 ? (
              <div className="px-4 py-8 text-center text-sm text-gray-400">
                暂无历史记录。
              </div>
            ) : (
              sortedSessions.map((session) => {
                const active = currentSessionId === session.id;
                return (
                  <div
                    key={session.id}
                    onClick={() => { onSelectSession(session.id); onCloseMobile(); }}
                    className={`group flex cursor-pointer items-center gap-3 rounded-xl px-3 py-3 text-sm transition-all duration-200 ${
                      active
                        ? 'bg-brand-light text-brand-primary'
                        : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                    }`}
                  >
                    <MessageSquare size={16} className="flex-shrink-0" />
                    <span className="min-w-0 flex-1 truncate">{session.title || '未命名对话'}</span>
                    <button
                      type="button"
                      onClick={(event) => onDeleteSession(event, session.id)}
                      className={`cursor-pointer rounded-lg p-1 text-gray-400 opacity-0 transition-all duration-200 hover:bg-red-50 hover:text-red-500 group-hover:opacity-100 ${active ? 'opacity-100' : ''}`}
                      title="删除对话"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                );
              })
            )}
          </div>
        </div>

        <div className="border-t border-gray-100 p-4">
          <div className="flex items-center gap-3 rounded-xl bg-gray-50 px-3 py-3 text-sm text-gray-500">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-primary text-xs font-medium text-white">
              MF
            </div>
            <div className="flex min-w-0 flex-col">
              <span className="text-sm font-medium text-gray-900">ManyFoot</span>
              <span className="text-xs text-gray-400">AI 对话助手</span>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
};

export default Sidebar;
