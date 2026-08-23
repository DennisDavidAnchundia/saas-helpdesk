import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import './i18n'
import App from './App'
import { initThemeClass } from './hooks/useTheme'

initThemeClass()

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      // Al volver a una pestana, refresca lo que lleve mas de staleTime viejo:
      // evita ver datos desactualizados tras crear cosas en otra sesion/pestana
      refetchOnWindowFocus: true,
      staleTime: 15_000,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
)
