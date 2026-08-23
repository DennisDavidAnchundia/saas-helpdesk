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

  return (
    <div className="bg-white rounded-xl shadow-lg border border-slate-200 p-6">
      <h2 className="text-lg font-semibold text-slate-800 mb-4">
        Base de Conocimiento ({articles.length})
      </h2>

      <form onSubmit={handleSearch} className="flex gap-2 mb-5">
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Buscar en titulos y contenidos..."
          className="flex-1 px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm"
        />
        <button
          type="submit"
          className="px-4 py-2 text-sm font-medium text-white bg-slate-700 rounded-lg hover:bg-slate-800 transition-colors"
        >
          Buscar
        </button>
        {query && (
          <button
            type="button"
            onClick={() => { setQuery(''); load(); }}
            className="px-3 py-2 text-sm border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
          >
            Limpiar
          </button>
        )}
      </form>

      {isStaff && (
        <form onSubmit={handleCreate} className="space-y-3 mb-6 p-4 bg-slate-50 rounded-lg border border-slate-200">
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Titulo del articulo"
            required
            maxLength={255}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm"
          />
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Contenido del articulo"
            rows={3}
            required
            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-sm"
          />
          <div className="flex gap-3 items-center">
            <input
              type="text"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              placeholder="Categoria (opcional)"
              className="px-3 py-2 border border-slate-300 rounded-lg text-sm flex-1"
            />
            <button
              type="submit"
              disabled={busy}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
            >
              {busy ? 'Creando...' : '+ Crear borrador'}
            </button>
          </div>
        </form>
      )}

      {!isStaff && (
        <p className="text-xs text-slate-400 mb-4">Como CUSTOMER solo ves articulos publicados.</p>
      )}

      {error && (
        <div className="bg-red-50 text-red-700 text-sm p-3 rounded-lg border border-red-200 mb-4">
          {error}
        </div>
      )}

      <div className="space-y-3">
        {articles.map((a) => (
          <div key={a.id} className="border border-slate-200 rounded-lg p-4">
            <div className="flex flex-wrap items-center gap-2 justify-between">
              <div className="flex flex-wrap items-center gap-2">
                <span className="font-semibold text-slate-800 text-sm">{a.title}</span>
                <span
                  className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                    a.isPublished ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                  }`}
                >
                  {a.isPublished ? 'PUBLICADO' : 'BORRADOR'}
                </span>
                {a.category && (
                  <span className="text-xs px-2 py-0.5 rounded-full font-medium bg-indigo-100 text-indigo-700">
                    {a.category}
                  </span>
                )}
              </div>
              {isStaff && (
                <div className="flex gap-1.5">
                  <button
                    onClick={() => handleTogglePublish(a)}
                    className="text-xs px-2.5 py-1 border border-slate-300 rounded-md hover:bg-slate-50 transition-colors"
                  >
                    {a.isPublished ? 'Despublicar' : 'Publicar'}
                  </button>
                  {role === 'ADMIN' && (
                    <button
                      onClick={() => handleDelete(a.id)}
                      className="text-xs px-2.5 py-1 border border-red-200 text-red-600 rounded-md hover:bg-red-50 transition-colors"
                    >
                      Borrar
                    </button>
                  )}
                </div>
              )}
            </div>
            <p className="text-xs text-slate-500 mt-2 whitespace-pre-wrap">{a.content}</p>
            <p className="text-xs text-slate-400 mt-1">
              Autor: {a.authorName || '-'} · Actualizado: {new Date(a.updatedAt).toLocaleString('es')}
            </p>
          </div>
        ))}
        {articles.length === 0 && !error && (
          <p className="text-sm text-slate-400 text-center py-4">
            No hay articulos que mostrar{query ? ' para esa busqueda' : ''}.
          </p>
        )}
      </div>
    </div>
  );
}
