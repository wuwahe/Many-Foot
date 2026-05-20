import React, { useEffect, useState } from 'react';
import { Save, X } from 'lucide-react';
import { AppSettings } from '../types';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  settings: AppSettings;
  onSave: (newSettings: AppSettings) => void;
}

const SettingsModal: React.FC<SettingsModalProps> = ({ isOpen, onClose, settings, onSave }) => {
  const [baseUrl, setBaseUrl] = useState(settings.baseUrl);

  useEffect(() => {
    if (isOpen) setBaseUrl(settings.baseUrl);
  }, [isOpen, settings]);

  if (!isOpen) return null;

  const handleSave = () => {
    onSave({ ...settings, baseUrl: baseUrl.trim() });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div className="flex max-h-[90vh] w-full max-w-md flex-col rounded-2xl border border-gray-200 bg-white shadow-lg">
        <div className="flex items-center justify-between border-b border-gray-100 p-4">
          <h2 className="font-sans text-lg font-semibold text-gray-900">设置</h2>
          <button
            type="button"
            onClick={onClose}
            className="cursor-pointer rounded-lg p-1.5 text-gray-500 transition-colors duration-200 hover:bg-gray-100 hover:text-gray-900"
          >
            <X size={20} />
          </button>
        </div>

        <div className="space-y-4 p-6">
          <div>
            <label htmlFor="baseUrl" className="mb-1 block text-sm font-medium text-gray-700">
              API 基础地址 (Base URL)
            </label>
            <p className="mb-3 text-xs leading-relaxed text-gray-500">
              可选。留空使用 Vite 代理，开发环境默认代理到 http://127.0.0.1:8100。
            </p>
            <input
              id="baseUrl"
              type="text"
              value={baseUrl}
              onChange={(event) => setBaseUrl(event.target.value)}
              placeholder="留空使用 /supervisor/chat 代理"
              className="w-full rounded-xl border border-gray-200 bg-gray-50 p-3 font-mono text-sm text-gray-900 outline-none transition-all duration-200 placeholder:text-gray-400 focus:border-brand-primary/50 focus:ring-1 focus:ring-brand-primary/50"
            />
          </div>
        </div>

        <div className="flex justify-end gap-3 border-t border-gray-100 p-4">
          <button
            type="button"
            onClick={onClose}
            className="cursor-pointer rounded-xl border border-gray-200 px-4 py-2 text-sm font-medium text-gray-600 transition-colors duration-200 hover:border-gray-300 hover:bg-gray-50 hover:text-gray-900"
          >
            取消
          </button>
          <button
            type="button"
            onClick={handleSave}
            className="flex cursor-pointer items-center gap-2 rounded-xl bg-brand-primary px-4 py-2 text-sm font-medium text-white transition-colors duration-200 hover:bg-brand-secondary"
          >
            <Save size={16} />
            保存更改
          </button>
        </div>
      </div>
    </div>
  );
};

export default SettingsModal;
