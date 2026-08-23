import { useTranslation } from 'react-i18next';
import { persistLang } from '../../i18n';

export default function LanguageSwitcher({ size = 'md' }: { size?: 'md' | 'sm' }) {
  const { i18n } = useTranslation();
  const current = i18n.language.startsWith('en') ? 'en' : 'es';

  return (
    <div
      className={`flex cursor-pointer items-center rounded-xl border border-slate-200/80 bg-white/70 font-mono font-semibold dark:border-white/10 dark:bg-white/5 ${
        size === 'sm' ? 'text-[11px]' : 'text-xs'
      }`}
    >
      {(['es', 'en'] as const).map((lng) => (
        <button
          key={lng}
          type="button"
          onClick={() => persistLang(lng)}
          className={`cursor-pointer rounded-xl px-2.5 py-1.5 uppercase transition-all duration-200 ${
            current === lng
              ? 'bg-gradient-to-r from-brand-500 to-violet-500 text-white shadow-md shadow-brand-500/25'
              : 'text-slate-400 hover:text-brand-500 dark:hover:text-brand-400'
          }`}
        >
          {lng}
        </button>
      ))}
    </div>
  );
}
