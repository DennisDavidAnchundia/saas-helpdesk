import { useTranslation } from 'react-i18next';
import { useTheme } from '../../hooks/useTheme';
import { MoonIcon, SunIcon } from '../icons';

export default function ThemeToggle({ size = 'md' }: { size?: 'md' | 'sm' }) {
  const { theme, toggle } = useTheme();
  const { t } = useTranslation();

  return (
    <button
      type="button"
      onClick={toggle}
      title={theme === 'dark' ? t('layout.themeToLight') : t('layout.themeToDark')}
      className={`grid cursor-pointer place-items-center rounded-xl border border-slate-200/80 bg-white/70 text-slate-500 transition-all duration-200 hover:-translate-y-0.5 hover:border-brand-300 hover:text-brand-500 hover:shadow-md hover:shadow-brand-500/10 dark:border-white/10 dark:bg-white/5 dark:text-slate-400 dark:hover:border-brand-500/40 dark:hover:text-brand-400 ${
        size === 'sm' ? 'size-8 text-sm' : 'size-9 text-base'
      }`}
    >
      {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
    </button>
  );
}
