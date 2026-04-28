import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface LayoutStore {
  sidebarCollapsed: boolean;
  toggleSidebar: () => void;
  setSidebarCollapsed: (v: boolean) => void;
}

export const useLayoutStore = create<LayoutStore>()(
  persist(
    (set) => ({
      sidebarCollapsed: false,
      toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
      setSidebarCollapsed: (v) => set({ sidebarCollapsed: v }),
    }),
    { name: 'tms-layout' }
  )
);
