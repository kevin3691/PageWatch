import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import CreateMonitor from '../views/CreateMonitor.vue'
import MonitorDetail from '../views/MonitorDetail.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Dashboard },
    { path: '/new', component: CreateMonitor },
    { path: '/monitors/:id', component: MonitorDetail },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ],
  scrollBehavior: () => ({ top: 0 })
})
