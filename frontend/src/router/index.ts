import { createRouter, createWebHistory } from 'vue-router'
import App from '../App.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'stations', component: App },
    {
      path: '/stations/:name/bay-mapping',
      name: 'station-bay-mapping',
      component: () => import('../views/StationBayMappingView.vue'),
      props: true,
    },
    {
      path: '/stations/:name',
      name: 'station-detail',
      component: () => import('../views/StationDetailView.vue'),
      props: true,
    },
  ],
})

export default router
