import React, { useEffect, useRef, useState } from 'react';
import { Send, Square, Paperclip, X, File, Loader2 } from 'lucide-react';
import { FileAttachment } from '../types';
import { uploadFile } from '../services/manyFootApi';

interface ChatInputProps {
  onSend: (message: string, attachments?: FileAttachment[]) => void;
  disabled: boolean;
  isStreaming: boolean;
  sessionId: string;
  baseUrl?: string;
}

const ChatInput: React.FC<ChatInputProps> = ({ onSend, disabled, isStreaming, sessionId, baseUrl }) => {
  const [text, setText] = useState('');
  const [attachments, setAttachments] = useState<FileAttachment[]>([]);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const adjustHeight = () => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 200)}px`;
    }
  };

  useEffect(() => {
    adjustHeight();
  }, [text]);

  const handleFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setUploadError(null);

    try {
      const result = await uploadFile(sessionId, file, baseUrl);

      const attachment: FileAttachment = {
        name: file.name,
        path: result.path,
        size: file.size,
        type: file.type,
      };

      setAttachments(prev => [...prev, attachment]);
    } catch (error) {
      console.error('文件上传失败', error);
      setUploadError('文件上传失败，请重试');
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const removeAttachment = (index: number) => {
    setAttachments(prev => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = (event?: React.FormEvent) => {
    event?.preventDefault();
    if ((!text.trim() && attachments.length === 0) || disabled || isStreaming) return;
    onSend(text, attachments.length > 0 ? attachments : undefined);
    setText('');
    setAttachments([]);
    if (textareaRef.current) textareaRef.current.style.height = 'auto';
  };

  const handleKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSubmit();
    }
  };

  const canSend = (Boolean(text.trim()) || attachments.length > 0) && !disabled && !isStreaming;

  return (
    <div className="w-full border-t border-gray-200 bg-white/95 p-3 backdrop-blur sm:p-4 md:border-none md:bg-transparent">
      {/* 附件预览区域 */}
      {attachments.length > 0 && (
        <div className="mx-auto mb-2 flex w-full max-w-4xl flex-wrap gap-2">
          {attachments.map((attachment, index) => (
            <div
              key={index}
              className="flex items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm"
            >
              <File size={14} className="text-brand-primary" />
              <span className="max-w-[150px] truncate text-gray-700">{attachment.name}</span>
              <button
                type="button"
                onClick={() => removeAttachment(index)}
                className="cursor-pointer text-gray-400 hover:text-red-500 transition-colors duration-200"
              >
                <X size={14} />
              </button>
            </div>
          ))}
        </div>
      )}

      {/* 上传错误提示 */}
      {uploadError && (
        <div className="mx-auto mb-2 w-full max-w-4xl rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
          {uploadError}
        </div>
      )}

      <form
        onSubmit={handleSubmit}
        className="mx-auto flex w-full max-w-4xl items-end gap-2 rounded-2xl border border-gray-200 bg-white p-2 shadow-sm transition-all duration-200 focus-within:border-brand-primary/50 focus-within:shadow-md"
      >
        {/* 文件上传按钮 */}
        <input
          type="file"
          ref={fileInputRef}
          onChange={handleFileSelect}
          className="hidden"
          disabled={disabled || isStreaming || uploading}
        />
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={disabled || isStreaming || uploading}
          className={`mb-1 rounded-xl border p-2.5 transition-all duration-200 focus:outline-none focus:ring-1 focus:ring-brand-primary/50 ${
            disabled || isStreaming || uploading
              ? 'cursor-not-allowed border-gray-200 bg-gray-100 text-gray-400'
              : 'cursor-pointer border-gray-200 bg-gray-50 text-gray-600 hover:border-gray-300 hover:bg-gray-100'
          }`}
        >
          {uploading ? (
            <Loader2 size={18} className="animate-spin" />
          ) : (
            <Paperclip size={18} />
          )}
        </button>

        <textarea
          ref={textareaRef}
          value={text}
          onChange={(event) => setText(event.target.value)}
          onKeyDown={handleKeyDown}
          disabled={disabled || isStreaming}
          placeholder={attachments.length > 0 ? "添加消息（可选）..." : "输入消息，开始对话..."}
          rows={1}
          className="min-h-[44px] max-h-[200px] w-full resize-none bg-transparent px-3 py-3 font-sans text-gray-900 placeholder-gray-400 outline-none disabled:cursor-not-allowed disabled:text-gray-400"
        />

        <button
          type="submit"
          disabled={!canSend}
          className={`mb-1 rounded-xl border p-2.5 transition-all duration-200 focus:outline-none focus:ring-1 focus:ring-brand-primary/50 ${
            canSend
              ? 'cursor-pointer border-brand-primary bg-brand-primary text-white hover:bg-brand-secondary'
              : 'cursor-not-allowed border-gray-200 bg-gray-100 text-gray-400'
          }`}
        >
          {isStreaming ? (
            <Square size={18} fill="currentColor" />
          ) : (
            <Send size={18} />
          )}
        </button>
      </form>
      <div className="mt-2 text-center">
        <p className="text-xs text-gray-400">
          AI 可能会产生不准确的信息，请注意核实
        </p>
      </div>
    </div>
  );
};

export default ChatInput;
