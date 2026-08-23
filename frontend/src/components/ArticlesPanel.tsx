import { useEffect, useState } from 'react';
import {
  listArticles,
  createArticle,
  updateArticle,
  deleteArticle,
  type Article,
} from '../services/api';

interface Props {
  token: string;
  role: string;
}

export default function ArticlesPanel({ token, role }: Props) {
  const [articles, setArticles] = useState<Article[]>([]);
  const [query, setQuery] = useState('');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const isStaff = role === 'ADMIN' || role === 'AGENT';

  const load = async (q?: string) => {
    try {
      setError('');
      setArticles(await listArticles(token, q));
    } catch (err: any) {
      setError(err.message);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    await load(query.trim() || undefined);
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await createArticle(token, {
        title,
        content,
        category: category || undefined,
      });
      setTitle('');
      setContent('');
      setCategory('');
      await load(query.trim() || undefined);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const handleTogglePublish = async (a: Article) => {
    try {
      await updateArticle(token, a.id, { isPublished: !a.isPublished });
      await load(query.trim() || undefined);
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteArticle(token, id);
      await load(query.trim() || undefined);
    } catch (err: any) {
      setError(err.message);
    }
  };

  const inputCls =
    'rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition-shadow placeholder:text-slate-400 focus:border-brand-400 focus:ring-4 focus:ring-brand-500/15 dark:border-white/10 dark:bg-white/5 dark:text-white';

  return (
    <div className="animate-fade-up rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm sm:p-6 dark:border-white/10 dark:bg-white/[0.03]">
      <h2 className="mb-4 font-display text-base font-bold tracking-tight text-slate-900 dark:text-white">
        Base de Conocimiento ({articles.length})
      </h2>

      <form onSubmit={handleSearch} className="mb-5 flex gap-2">
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Buscar en títulos y contenidos…"
          className={`${inputCls} flex-1`}
        />
        <button
          type="submit"
          className="cursor-pointer rounded-xl border border-slate-200/70 bg-white px-4 py-2.5 text-sm font-semibold text-slate-600 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-brand-300 hover:text-brand-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-slate-400 dark:hover:border-brand-500/40 dark:hover:text-brand-300"
        >
          Buscar
        </button>
        {query && (
          <button
            type="button"
            onClick={() => {
              setQuery('');
              load();
            }}
            className="cursor-pointer rounded-xl px-3 py-2.5 text-sm font-medium text-slate-400 transition-colors hover:text-brand-500 dark:hover:text-brand-300"
          >
            Limpiar
          </button>
        )}
      </form>

      {isStaff && (
        <form
          onSubmit={handleCreate}
          className="mb-6 space-y-3 rounded-xl border border-brand-100 bg-brand-50/60 p-4 dark:border-brand-500/20 dark:bg-brand-500/[0.06]"
        >
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Título del artículo"
            required
            maxLength={255}
            className={`${inputCls} w-full`}
          />
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Contenido del artículo"
            rows={3}
            required
            className={`${inputCls} w-full resize-none`}
          />
          <div className="flex items-center gap-3">
            <input
              type="text"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              placeholder="Categoría (opcional)"
              className={`${inputCls} flex-1`}
            />
            <button
              type="submit"
              disabled={busy}
              className="cursor-pointer rounded-xl bg-gradient-to-r from-brand-500 to-violet-500 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-brand-500/25 transition-all duration-200 hover:-translate-y-0.5 disabled:opacity-60"
            >
              {busy ? 'Creando…' : '+ Crear borrador'}
            </button>
          </div>
        </form>
      )}

      {!isStaff && (
        <p className="mb-4 text-xs text-slate-400">Como CUSTOMER solo ves artículos publicados.</p>
      )}

      {error && (
        <div className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="space-y-3">
        {articles.map((a) => (
          <article
            key={a.id}
            className="rounded-2xl border border-slate-200/70 p-4 transition-all duration-200 hover:-translate-y-0.5 hover:border-brand-300 hover:shadow-lg hover:shadow-brand-500/5 dark:border-white/10 dark:hover:border-brand-500/40"
          >
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-sm font-semibold text-slate-800 dark:text-slate-100">{a.title}</span>
                <span
                  className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                    a.isPublished
                      ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300'
                      : 'bg-yellow-100 text-yellow-700 dark:bg-yellow-500/15 dark:text-yellow-300'
                  }`}
                >
                  {a.isPublished ? 'PUBLICADO' : 'BORRADOR'}
                </span>
                {a.category && (
                  <span className="rounded-full bg-brand-100 px-2 py-0.5 text-[11px] font-semibold text-brand-600 dark:bg-brand-500/15 dark:text-brand-300">
                    {a.category}
                  </span>
                )}
              </div>
              {isStaff && (
                <div className="flex gap-1.5">
                  <button
                    onClick={() => handleTogglePublish(a)}
                    className="cursor-pointer rounded-lg border border-slate-300 px-2.5 py-1 text-[11px] font-medium text-slate-600 transition-colors hover:border-brand-400 hover:bg-brand-50 hover:text-brand-600 dark:border-white/10 dark:text-slate-400 dark:hover:border-brand-500/40 dark:hover:bg-brand-500/10 dark:hover:text-brand-300"
                  >
                    {a.isPublished ? 'Despublicar' : 'Publicar'}
                  </button>
                  {role === 'ADMIN' && (
                    <button
                      onClick={() => handleDelete(a.id)}
                      className="cursor-pointer rounded-lg border border-red-200 px-2.5 py-1 text-[11px] font-medium text-red-600 transition-colors hover:bg-red-50 dark:border-red-500/30 dark:text-red-400 dark:hover:bg-red-500/10"
                    >
                      Borrar
                    </button>
                  )}
                </div>
              )}
            </div>
            <p className="mt-2 whitespace-pre-wrap text-xs text-slate-500 dark:text-slate-400">{a.content}</p>
            <p className="mt-1.5 text-xs text-slate-400">
              Autor: {a.authorName || '-'} · Actualizado: {new Date(a.updatedAt).toLocaleString('es')}
            </p>
          </article>
        ))}
        {articles.length === 0 && !error && (
          <p className="py-8 text-center text-sm text-slate-400">
            No hay artículos que mostrar{query ? ' para esa búsqueda' : ''}.
          </p>
        )}
      </div>
    </div>
  );
}
